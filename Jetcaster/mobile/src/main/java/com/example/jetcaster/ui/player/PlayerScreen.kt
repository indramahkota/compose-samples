/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.example.jetcaster.ui.player

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import com.example.jetcaster.R
import com.example.jetcaster.core.player.EpisodePlayerState
import com.example.jetcaster.core.player.model.PlayerEpisode
import com.example.jetcaster.designsystem.component.HtmlTextContainer
import com.example.jetcaster.designsystem.component.ImageBackgroundColorScrim
import com.example.jetcaster.designsystem.component.PodcastImage
import com.example.jetcaster.ui.LocalAnimatedVisibilityScope
import com.example.jetcaster.ui.LocalSharedTransitionScope
import com.example.jetcaster.ui.theme.JetcasterTheme
import com.example.jetcaster.ui.tooling.DevicePreviews
import com.example.jetcaster.util.isBookPosture
import com.example.jetcaster.util.isSeparatingPosture
import com.example.jetcaster.util.isTableTopPosture
import com.example.jetcaster.util.verticalGradientScrim
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane
import com.google.accompanist.adaptive.VerticalTwoPaneStrategy
import java.time.Duration
import kotlinx.coroutines.launch

/**
 * Stateful version of the Podcast player
 */
@Composable
fun PlayerScreen(
    windowSizeClass: WindowSizeClass,
    displayFeatures: List<DisplayFeature>,
    onBackPress: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState
    PlayerScreen(
        uiState = uiState,
        windowSizeClass = windowSizeClass,
        displayFeatures = displayFeatures,
        onBackPress = onBackPress,
        onAddToQueue = viewModel::onAddToQueue,
        onStop = viewModel::onStop,
        playerControlActions = PlayerControlActions(
            onPlayPress = viewModel::onPlay,
            onPausePress = viewModel::onPause,
            onAdvanceBy = viewModel::onAdvanceBy,
            onRewindBy = viewModel::onRewindBy,
            onSeekingStarted = viewModel::onSeekingStarted,
            onSeekingFinished = viewModel::onSeekingFinished,
            onNext = viewModel::onNext,
            onPrevious = viewModel::onPrevious,
        ),
    )
}

/**
 * Stateless version of the Player screen
 */
@Composable
private fun PlayerScreen(
    uiState: PlayerUiState,
    windowSizeClass: WindowSizeClass,
    displayFeatures: List<DisplayFeature>,
    onBackPress: () -> Unit,
    onAddToQueue: () -> Unit,
    onStop: () -> Unit,
    playerControlActions: PlayerControlActions,
    modifier: Modifier = Modifier,
) {
    DisposableEffect(Unit) {
        onDispose {
            onStop()
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val snackBarText = stringResource(id = R.string.episode_added_to_your_queue)
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = modifier,
    ) { contentPadding ->
        if (uiState.episodePlayerState.currentEpisode != null) {
            PlayerContentWithBackground(
                uiState = uiState,
                windowSizeClass = windowSizeClass,
                displayFeatures = displayFeatures,
                onBackPress = onBackPress,
                onAddToQueue = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(snackBarText)
                    }
                    onAddToQueue()
                },
                playerControlActions = playerControlActions,
                contentPadding = contentPadding,
            )
        } else {
            FullScreenLoading()
        }
    }
}

@Composable
private fun PlayerBackground(episode: PlayerEpisode?, modifier: Modifier) {
    ImageBackgroundColorScrim(
        url = episode?.podcastImageUrl,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        modifier = modifier,
    )
}

@Composable
fun PlayerContentWithBackground(
    uiState: PlayerUiState,
    windowSizeClass: WindowSizeClass,
    displayFeatures: List<DisplayFeature>,
    onBackPress: () -> Unit,
    onAddToQueue: () -> Unit,
    playerControlActions: PlayerControlActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        PlayerBackground(
            episode = uiState.episodePlayerState.currentEpisode,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        )
        PlayerContent(
            uiState = uiState,
            windowSizeClass = windowSizeClass,
            displayFeatures = displayFeatures,
            onBackPress = onBackPress,
            onAddToQueue = onAddToQueue,
            playerControlActions = playerControlActions,
        )
    }
}

/**
 * Wrapper around all actions for the player controls.
 */
data class PlayerControlActions(
    val onPlayPress: () -> Unit,
    val onPausePress: () -> Unit,
    val onAdvanceBy: (Duration) -> Unit,
    val onRewindBy: (Duration) -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onSeekingStarted: () -> Unit,
    val onSeekingFinished: (newElapsed: Duration) -> Unit,
)

@Composable
fun PlayerContent(
    uiState: PlayerUiState,
    windowSizeClass: WindowSizeClass,
    displayFeatures: List<DisplayFeature>,
    onBackPress: () -> Unit,
    onAddToQueue: () -> Unit,
    playerControlActions: PlayerControlActions,
    modifier: Modifier = Modifier,
) {
    val foldingFeature = displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()

    // Use a two pane layout if there is a fold impacting layout (meaning it is separating
    // or non-flat) or if we have a large enough width to show both.
    if (
        windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED ||
        isBookPosture(foldingFeature) ||
        isTableTopPosture(foldingFeature) ||
        isSeparatingPosture(foldingFeature)
    ) {
        // Determine if we are going to be using a vertical strategy (as if laying out
        // both sides in a column). We want to do so if we are in a tabletop posture,
        // or we have an impactful horizontal fold. Otherwise, we'll use a horizontal strategy.
        val usingVerticalStrategy =
            isTableTopPosture(foldingFeature) ||
                (
                    isSeparatingPosture(foldingFeature) &&
                        foldingFeature.orientation ==
                        FoldingFeature.Orientation.HORIZONTAL
                    )

        if (usingVerticalStrategy) {
            TwoPane(
                first = {
                    PlayerContentTableTopTop(
                        uiState = uiState,
                    )
                },
                second = {
                    PlayerContentTableTopBottom(
                        uiState = uiState,
                        onBackPress = onBackPress,
                        onAddToQueue = onAddToQueue,
                        playerControlActions = playerControlActions,
                    )
                },
                strategy = VerticalTwoPaneStrategy(splitFraction = 0.5f),
                displayFeatures = displayFeatures,
                modifier = modifier,
            )
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalGradientScrim(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.50f),
                        startYPercentage = 1f,
                        endYPercentage = 0f,
                    )
                    .systemBarsPadding()
                    .padding(horizontal = 8.dp),
            ) {
                TopAppBar(
                    onBackPress = onBackPress,
                    onAddToQueue = onAddToQueue,
                )
                TwoPane(
                    first = {
                        PlayerContentBookStart(uiState = uiState)
                    },
                    second = {
                        PlayerContentBookEnd(
                            uiState = uiState,
                            playerControlActions = playerControlActions,
                        )
                    },
                    strategy = HorizontalTwoPaneStrategy(splitFraction = 0.5f),
                    displayFeatures = displayFeatures,
                )
            }
        }
    } else {
        PlayerContentRegular(
            uiState = uiState,
            onBackPress = onBackPress,
            onAddToQueue = onAddToQueue,
            playerControlActions = playerControlActions,
            modifier = modifier,
        )
    }
}

/**
 * The UI for the top pane of a tabletop layout.
 */
@Composable
private fun PlayerContentRegular(
    uiState: PlayerUiState,
    onBackPress: () -> Unit,
    onAddToQueue: () -> Unit,
    playerControlActions: PlayerControlActions,
    modifier: Modifier = Modifier,
) {
    val playerEpisode = uiState.episodePlayerState
    val currentEpisode = playerEpisode.currentEpisode ?: return

    val sharedTransitionScope = LocalSharedTransitionScope.current
        ?: throw IllegalStateException("No SharedElementScope found")
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
        ?: throw IllegalStateException("No SharedElementScope found")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalGradientScrim(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.50f),
                startYPercentage = 1f,
                endYPercentage = 0f,
            )
            .systemBarsPadding()
            .padding(horizontal = 8.dp),
    ) {
        TopAppBar(
            onBackPress = onBackPress,
            onAddToQueue = onAddToQueue,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            with(sharedTransitionScope) {
                with(animatedVisibilityScope) {
                    PlayerImage(
                        podcastImageUrl = currentEpisode.podcastImageUrl,
                        modifier = Modifier
                            .weight(10f)
                            .animateEnterExit(
                                enter = fadeIn(spring(stiffness = Spring.StiffnessLow)),
                                exit = fadeOut(),
                            ),
                        imageModifier = Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(
                                key = currentEpisode.title,
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                            clipInOverlayDuringTransition =
                            OverlayClip(MaterialTheme.shapes.medium),
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                PodcastDescription(currentEpisode.title, currentEpisode.podcastName)
                Spacer(modifier = Modifier.height(32.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(10f),
                ) {
                    PlayerSlider(
                        timeElapsed = playerEpisode.timeElapsed,
                        episodeDuration = currentEpisode.duration,
                        onSeekingStarted = playerControlActions.onSeekingStarted,
                        onSeekingFinished = playerControlActions.onSeekingFinished,
                    )
                    PlayerButtons(
                        hasNext = playerEpisode.queue.isNotEmpty(),
                        isPlaying = playerEpisode.isPlaying,
                        onPlayPress = playerControlActions.onPlayPress,
                        onPausePress = playerControlActions.onPausePress,
                        onAdvanceBy = playerControlActions.onAdvanceBy,
                        onRewindBy = playerControlActions.onRewindBy,
                        onNext = playerControlActions.onNext,
                        onPrevious = playerControlActions.onPrevious,
                        Modifier.padding(vertical = 8.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * The UI for the top pane of a tabletop layout.
 */
@Composable
private fun PlayerContentTableTopTop(uiState: PlayerUiState, modifier: Modifier = Modifier) {
    // Content for the top part of the screen
    val episode = uiState.episodePlayerState.currentEpisode ?: return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalGradientScrim(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.50f),
                startYPercentage = 1f,
                endYPercentage = 0f,
            )
            .windowInsetsPadding(
                WindowInsets.systemBars.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                ),
            )
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PlayerImage(episode.podcastImageUrl)
    }
}

/**
 * The UI for the bottom pane of a tabletop layout.
 */
@Composable
private fun PlayerContentTableTopBottom(
    uiState: PlayerUiState,
    onBackPress: () -> Unit,
    onAddToQueue: () -> Unit,
    playerControlActions: PlayerControlActions,
    modifier: Modifier = Modifier,
) {
    val episodePlayerState = uiState.episodePlayerState
    val episode = uiState.episodePlayerState.currentEpisode ?: return
    // Content for the table part of the screen
    Column(
        modifier = modifier
            .windowInsetsPadding(
                WindowInsets.systemBars.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                ),
            )
            .padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopAppBar(
            onBackPress = onBackPress,
            onAddToQueue = onAddToQueue,
        )
        PodcastDescription(
            title = episode.title,
            podcastName = episode.podcastName,
        )
        Spacer(modifier = Modifier.weight(0.5f))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(10f),
        ) {
            PlayerButtons(
                hasNext = episodePlayerState.queue.isNotEmpty(),
                isPlaying = episodePlayerState.isPlaying,
                onPlayPress = playerControlActions.onPlayPress,
                onPausePress = playerControlActions.onPausePress,
                onAdvanceBy = playerControlActions.onAdvanceBy,
                onRewindBy = playerControlActions.onRewindBy,
                onNext = playerControlActions.onNext,
                onPrevious = playerControlActions.onPrevious,
                modifier = Modifier.padding(top = 8.dp),
            )
            PlayerSlider(
                timeElapsed = episodePlayerState.timeElapsed,
                episodeDuration = episode.duration,
                onSeekingStarted = playerControlActions.onSeekingStarted,
                onSeekingFinished = playerControlActions.onSeekingFinished,
            )
        }
    }
}

/**
 * The UI for the start pane of a book layout.
 */
@Composable
private fun PlayerContentBookStart(uiState: PlayerUiState, modifier: Modifier = Modifier) {
    val episode = uiState.episodePlayerState.currentEpisode ?: return
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                vertical = 40.dp,
                horizontal = 16.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PodcastInformation(
            title = episode.title,
            name = episode.podcastName,
            summary = episode.summary,
        )
    }
}

/**
 * The UI for the end pane of a book layout.
 */
@Composable
private fun PlayerContentBookEnd(uiState: PlayerUiState, playerControlActions: PlayerControlActions, modifier: Modifier = Modifier) {
    val episodePlayerState = uiState.episodePlayerState
    val episode = episodePlayerState.currentEpisode ?: return
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround,
    ) {
        PlayerImage(
            podcastImageUrl = episode.podcastImageUrl,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .weight(1f),
        )
        PlayerSlider(
            timeElapsed = episodePlayerState.timeElapsed,
            episodeDuration = episode.duration,
            onSeekingStarted = playerControlActions.onSeekingStarted,
            onSeekingFinished = playerControlActions.onSeekingFinished,
        )
        PlayerButtons(
            hasNext = episodePlayerState.queue.isNotEmpty(),
            isPlaying = episodePlayerState.isPlaying,
            onPlayPress = playerControlActions.onPlayPress,
            onPausePress = playerControlActions.onPausePress,
            onAdvanceBy = playerControlActions.onAdvanceBy,
            onRewindBy = playerControlActions.onRewindBy,
            onNext = playerControlActions.onNext,
            onPrevious = playerControlActions.onPrevious,
            Modifier.padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun TopAppBar(onBackPress: () -> Unit, onAddToQueue: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        IconButton(onClick = onBackPress) {
            Icon(
                painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.cd_back),
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onAddToQueue) {
            Icon(
                painterResource(id = R.drawable.ic_playlist_add),
                contentDescription = stringResource(R.string.cd_add),
            )
        }
        IconButton(onClick = { /* TODO */ }) {
            Icon(
                painterResource(id = R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.cd_more),
            )
        }
    }
}

@Composable
private fun PlayerImage(podcastImageUrl: String, modifier: Modifier = Modifier, imageModifier: Modifier = Modifier) {
    PodcastImage(
        podcastImageUrl = podcastImageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .sizeIn(maxWidth = 500.dp, maxHeight = 500.dp)
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium),
        imageModifier = imageModifier,
    )
}

@Composable
private fun PodcastDescription(title: String, podcastName: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.displayLarge,
        maxLines = 2,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.basicMarquee(),
    )
    Text(
        text = podcastName,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
    )
}

@Composable
private fun PodcastInformation(
    title: String,
    name: String,
    summary: String,
    modifier: Modifier = Modifier,
    titleTextStyle: TextStyle = MaterialTheme.typography.headlineLarge,
    nameTextStyle: TextStyle = MaterialTheme.typography.displaySmall,
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = name,
            style = nameTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = title,
            style = titleTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        HtmlTextContainer(text = summary) {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current,
            )
        }
    }
}

fun Duration.formatString(): String {
    val minutes = this.toMinutes().toString().padStart(2, '0')
    val secondsLeft = (this.toSeconds() % 60).toString().padStart(2, '0')
    return "$minutes:$secondsLeft"
}

@Composable
private fun PlayerSlider(
    timeElapsed: Duration,
    episodeDuration: Duration?,
    onSeekingStarted: () -> Unit,
    onSeekingFinished: (newElapsed: Duration) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        var sliderValue by remember(timeElapsed) { mutableStateOf(timeElapsed) }
        val maxRange = (episodeDuration?.toSeconds() ?: 0).toFloat()

        Row(Modifier.fillMaxWidth()) {
            Text(
                text = "${sliderValue.formatString()} • ${episodeDuration?.formatString()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Slider(
            value = sliderValue.seconds.toFloat(),
            valueRange = 0f..maxRange,
            onValueChange = {
                onSeekingStarted()
                sliderValue = Duration.ofSeconds(it.toLong())
            },
            onValueChangeFinished = { onSeekingFinished(sliderValue) },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerButtons(
    hasNext: Boolean,
    isPlaying: Boolean,
    onPlayPress: () -> Unit,
    onPausePress: () -> Unit,
    onAdvanceBy: (Duration) -> Unit,
    onRewindBy: (Duration) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ToggleButton(
            checked = isPlaying,
            onCheckedChange = {
                if (isPlaying) {
                    onPausePress()
                } else {
                    onPlayPress()
                }
            },
            colors = ToggleButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                checkedContainerColor = MaterialTheme.colorScheme.tertiary,
                checkedContentColor = MaterialTheme.colorScheme.onTertiary,
            ),
            shapes = ToggleButtonShapes(
                shape = RoundedCornerShape(60.dp),
                pressedShape = RoundedCornerShape(if (isPlaying) 60.dp else 30.dp),
                checkedShape = RoundedCornerShape(30.dp),
            ),
            modifier = Modifier
                .width(186.dp)
                .height(136.dp),
        ) {
            Icon(
                painterResource(id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                modifier = Modifier.fillMaxSize(),
                contentDescription = null,
            )
        }
        ButtonGroup(
            overflowIndicator = {},
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val skipButtonsModifier = Modifier
                .width(56.dp)
                .height(68.dp)

            val rewindFastForwardButtonsModifier = Modifier
                .size(68.dp)
            val interactionSources = List(4) { MutableInteractionSource() }
            customItem(
                buttonGroupContent = {
                    IconButton(
                        onClick = onPrevious,
                        modifier = skipButtonsModifier.animateWidth(interactionSource = interactionSources[0]),
                        shape = RoundedCornerShape(50.dp),
                        colors = IconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        interactionSource = interactionSources[0],
                        enabled = isPlaying,
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_skip_previous),
                            contentDescription = null,
                        )
                    }
                },
                menuContent = { },
            )

            customItem(
                buttonGroupContent = {
                    IconButton(
                        onClick = { onRewindBy(Duration.ofSeconds(10)) },
                        modifier = rewindFastForwardButtonsModifier.animateWidth(interactionSource = interactionSources[1]),
                        shape = RoundedCornerShape(15.dp),
                        colors = IconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            disabledContainerColor = MaterialTheme.colorScheme.secondary,
                            disabledContentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                        interactionSource = interactionSources[1],
                        enabled = isPlaying,
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_replay_10),
                            contentDescription = null,
                        )
                    }
                },
                menuContent = { },
            )

            customItem(
                buttonGroupContent = {
                    IconButton(
                        onClick = { onAdvanceBy(Duration.ofSeconds(10)) },
                        modifier = rewindFastForwardButtonsModifier.animateWidth(interactionSource = interactionSources[2]),
                        shape = RoundedCornerShape(15.dp),
                        colors = IconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            disabledContainerColor = MaterialTheme.colorScheme.secondary,
                            disabledContentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                        interactionSource = interactionSources[2],
                        enabled = isPlaying,
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_forward_10),
                            contentDescription = null,
                        )
                    }
                },
                menuContent = { },
            )

            customItem(
                buttonGroupContent = {
                    IconButton(
                        onClick = onNext,
                        modifier = skipButtonsModifier.animateWidth(interactionSource = interactionSources[3]),
                        shape = RoundedCornerShape(50.dp),
                        colors = IconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            disabledContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        interactionSource = interactionSources[3],
                        enabled = hasNext,
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_skip_next),
                            contentDescription = null,
                        )
                    }
                },
                menuContent = { },
            )
        }
    }
}

/**
 * Full screen circular progress indicator
 */
@Composable
private fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
    ) {
        CircularProgressIndicator()
    }
}

@Preview
@Composable
fun TopAppBarPreview() {
    JetcasterTheme {
        TopAppBar(
            onBackPress = {},
            onAddToQueue = {},
        )
    }
}

@Preview
@Composable
fun PlayerButtonsPreview() {
    JetcasterTheme {
        PlayerButtons(
            hasNext = false,
            isPlaying = true,
            onPlayPress = {},
            onPausePress = {},
            onAdvanceBy = {},
            onRewindBy = {},
            onNext = {},
            onPrevious = {},
        )
    }
}

@DevicePreviews
@Composable
fun PlayerScreenPreview() {
    JetcasterTheme {
        BoxWithConstraints {
            PlayerScreen(
                PlayerUiState(
                    episodePlayerState = EpisodePlayerState(
                        currentEpisode = PlayerEpisode(
                            title = "Title",
                            duration = Duration.ofHours(2),
                            podcastName = "Podcast",
                        ),
                        isPlaying = false,
                        queue = listOf(
                            PlayerEpisode(),
                            PlayerEpisode(),
                            PlayerEpisode(),
                        ),
                    ),
                ),
                displayFeatures = emptyList(),
                windowSizeClass = WindowSizeClass.compute(maxWidth.value, maxHeight.value),
                onBackPress = { },
                onAddToQueue = {},
                onStop = {},
                playerControlActions = PlayerControlActions(
                    onPlayPress = {},
                    onPausePress = {},
                    onAdvanceBy = {},
                    onRewindBy = {},
                    onSeekingStarted = {},
                    onSeekingFinished = {},
                    onNext = {},
                    onPrevious = {},
                ),
            )
        }
    }
}
