package com.offpolice.webradiobot.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.offpolice.webradiobot.data.isValidFaviconUrl
import com.offpolice.webradiobot.player.PlaybackState
import com.offpolice.webradiobot.ui.theme.CardBg
import com.offpolice.webradiobot.ui.theme.LightPink
import com.offpolice.webradiobot.ui.theme.PrimaryPink
import com.offpolice.webradiobot.ui.theme.SearchBg
import com.offpolice.webradiobot.ui.theme.SecondaryPink
import com.offpolice.webradiobot.ui.theme.TextMuted
import com.offpolice.webradiobot.ui.theme.TextPrimary
import com.offpolice.webradiobot.ui.theme.TextSecondary
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val ArtworkCornerShape = RoundedCornerShape(28.dp)
private val DragHandleShape = RoundedCornerShape(2.dp)
private val LiveBadgeShape = RoundedCornerShape(50)
private val PlayButtonGradient = Brush.linearGradient(
    colors = listOf(PrimaryPink, SecondaryPink)
)

@Composable
fun FullScreenPlayer(
    viewModel: RadioViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onDismiss()
    }

    val currentUrl by viewModel.playerManager.currentUrl.collectAsStateWithLifecycle()
    val currentName by viewModel.playerManager.currentName.collectAsStateWithLifecycle()
    val currentArtist by viewModel.playerManager.currentArtist.collectAsStateWithLifecycle()
    val currentTrackTitle by viewModel.playerManager.currentTrackTitle.collectAsStateWithLifecycle()
    val currentFavicon by viewModel.playerManager.currentFavicon.collectAsStateWithLifecycle()
    val playbackState by viewModel.playerManager.playbackState.collectAsStateWithLifecycle()
    val favoriteUrls by viewModel.favoriteUrls.collectAsStateWithLifecycle()
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    val isFavorite = currentUrl != null && favoriteUrls.contains(currentUrl)

    val activeApiStation = remember(currentUrl, stations) {
        if (currentUrl == null) null else stations.firstOrNull { it.url_resolved == currentUrl }
    }
    val activeFavStation = remember(currentUrl, favorites) {
        if (currentUrl == null) null else favorites.firstOrNull { it.urlResolved == currentUrl }
    }

    val topTags = activeApiStation?.topTags ?: activeFavStation?.topTags
    val streamSpec = remember(activeApiStation) {
        if (activeApiStation == null) null
        else {
            val codec = activeApiStation.codec?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
            val bitrate = activeApiStation.bitrate?.takeIf { it > 0 }?.let { "$it kbps" }
            listOfNotNull(codec, bitrate).joinToString(" • ").takeIf { it.isNotEmpty() }
        }
    }

    val onTogglePlay = remember(viewModel) { { viewModel.togglePlay() } }
    val onNext = remember(viewModel) { { viewModel.playNextStation() } }
    val onPrev = remember(viewModel) { { viewModel.playPreviousStation() } }
    val onToggleFavorite = remember(viewModel) { { viewModel.toggleCurrentStationFavorite() } }

    FullScreenPlayerContent(
        currentUrl = currentUrl,
        currentName = currentName,
        currentArtist = currentArtist,
        currentTrackTitle = currentTrackTitle,
        currentFavicon = currentFavicon,
        playbackState = playbackState,
        isFavorite = isFavorite,
        topTags = topTags,
        streamSpec = streamSpec,
        onDismiss = onDismiss,
        onTogglePlay = onTogglePlay,
        onNext = onNext,
        onPrev = onPrev,
        onToggleFavorite = onToggleFavorite,
        modifier = modifier
    )
}

@Composable
fun FullScreenPlayerContent(
    currentUrl: String?,
    currentName: String?,
    currentArtist: String?,
    currentTrackTitle: String?,
    currentFavicon: String?,
    playbackState: PlaybackState,
    isFavorite: Boolean,
    topTags: Triple<String?, String?, String?>?,
    streamSpec: String?,
    onDismiss: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }
    val dismissThresholdPx = with(LocalDensity.current) { 130.dp.toPx() }

    val isPlaying = playbackState == PlaybackState.PLAYING
    val isBuffering = playbackState == PlaybackState.BUFFERING
    val hasStation = currentUrl != null

    val glowAlpha by animateFloatAsState(
        targetValue = when {
            isPlaying -> 0.26f
            isBuffering -> 0.16f
            else -> 0.08f
        },
        animationSpec = tween(durationMillis = 450),
        label = "player_glow_alpha"
    )

    val bgColor = MaterialTheme.colorScheme.background

    Surface(
        color = bgColor,
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffsetY.value > dismissThresholdPx) {
                            onDismiss()
                        } else {
                            scope.launch {
                                dragOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            dragOffsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                        }
                    },
                    onVerticalDrag = { _, dragAmount ->
                        val next = (dragOffsetY.value + dragAmount).coerceAtLeast(0f)
                        scope.launch {
                            dragOffsetY.snapTo(next)
                        }
                    }
                )
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* absorb background taps */ }
            .testTag("fullscreen_player_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PrimaryPink.copy(alpha = glowAlpha),
                                SecondaryPink.copy(alpha = glowAlpha * 0.45f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.36f),
                            radius = size.maxDimension * 0.55f
                        ),
                        radius = size.maxDimension * 0.55f,
                        center = Offset(size.width * 0.5f, size.height * 0.36f)
                    )
                }
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                val availableWidth = maxWidth
                val availableHeight = maxHeight
                val isCompactHeight = availableHeight < 480.dp

                if (isCompactHeight) {
                    // Landscape / compact height adaptive layout
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FullScreenTopBar(
                            hasStation = hasStation,
                            isFavorite = isFavorite,
                            playbackState = playbackState,
                            onDismiss = onDismiss,
                            onToggleFavorite = onToggleFavorite
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .widthIn(max = 720.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            val artSize = (availableHeight - 100.dp).coerceIn(120.dp, 220.dp)
                            StationArtworkCard(
                                faviconUrl = currentFavicon,
                                isPlaying = isPlaying,
                                size = artSize
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StationMetadataSection(
                                    currentName = currentName,
                                    currentArtist = currentArtist,
                                    currentTrackTitle = currentTrackTitle,
                                    hasStation = hasStation,
                                    topTags = topTags,
                                    streamSpec = streamSpec,
                                    playbackState = playbackState,
                                    compact = true
                                )

                                PlayerControlsRow(
                                    hasStation = hasStation,
                                    playbackState = playbackState,
                                    onPrev = onPrev,
                                    onTogglePlay = onTogglePlay,
                                    onNext = onNext
                                )
                            }
                        }
                    }
                } else {
                    // Standard portrait layout
                    val artworkSize = minOf(availableWidth - 64.dp, availableHeight * 0.36f).coerceIn(160.dp, 280.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 520.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Subtle pull-down pill handle
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp, bottom = 6.dp)
                                .width(38.dp)
                                .height(4.dp)
                                .clip(DragHandleShape)
                                .background(TextMuted.copy(alpha = 0.45f))
                        )

                        FullScreenTopBar(
                            hasStation = hasStation,
                            isFavorite = isFavorite,
                            playbackState = playbackState,
                            onDismiss = onDismiss,
                            onToggleFavorite = onToggleFavorite
                        )

                        Spacer(modifier = Modifier.weight(0.6f))

                        StationArtworkCard(
                            faviconUrl = currentFavicon,
                            isPlaying = isPlaying,
                            size = artworkSize
                        )

                        Spacer(modifier = Modifier.weight(0.6f))

                        StationMetadataSection(
                            currentName = currentName,
                            currentArtist = currentArtist,
                            currentTrackTitle = currentTrackTitle,
                            hasStation = hasStation,
                            topTags = topTags,
                            streamSpec = streamSpec,
                            playbackState = playbackState,
                            compact = false
                        )

                        Spacer(modifier = Modifier.weight(0.6f))

                        PlayerControlsRow(
                            hasStation = hasStation,
                            playbackState = playbackState,
                            onPrev = onPrev,
                            onTogglePlay = onTogglePlay,
                            onNext = onNext
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenTopBar(
    hasStation: Boolean,
    isFavorite: Boolean,
    playbackState: PlaybackState,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(48.dp)
                .clip(CircleShape)
                .background(CardBg.copy(alpha = 0.7f))
                .testTag("fullscreen_player_collapse_btn")
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Collapse Player",
                tint = TextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringLoc("now_playing").uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(3.dp))
            StatusBadgeRow(playbackState = playbackState)
        }

        val favTint by animateColorAsState(
            targetValue = if (isFavorite) PrimaryPink else TextSecondary,
            label = "fullscreen_fav_tint"
        )
        IconButton(
            onClick = onToggleFavorite,
            enabled = hasStation,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(48.dp)
                .clip(CircleShape)
                .background(CardBg.copy(alpha = 0.7f))
                .testTag("fullscreen_player_fav_btn")
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (hasStation) favTint else TextMuted,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun StatusBadgeRow(playbackState: PlaybackState) {
    val statusColor = when (playbackState) {
        PlaybackState.PLAYING -> Color(0xFF4CAF50)
        PlaybackState.BUFFERING -> LightPink
        PlaybackState.PAUSED -> Color(0xFFFFA000)
        PlaybackState.ERROR -> Color(0xFFF44336)
        PlaybackState.IDLE -> TextSecondary
    }
    val statusText = when (playbackState) {
        PlaybackState.PLAYING -> stringLoc("live_radio")
        PlaybackState.BUFFERING -> stringLoc("buffering")
        PlaybackState.PAUSED -> stringLoc("paused")
        PlaybackState.ERROR -> stringLoc("error")
        PlaybackState.IDLE -> stringLoc("idle")
    }
    val statusIcon = when (playbackState) {
        PlaybackState.PLAYING -> Icons.AutoMirrored.Filled.VolumeUp
        PlaybackState.BUFFERING -> null
        PlaybackState.PAUSED -> Icons.Default.PauseCircle
        PlaybackState.ERROR -> Icons.Default.Error
        PlaybackState.IDLE -> Icons.Default.Circle
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(LiveBadgeShape)
            .background(statusColor.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        if (statusIcon != null) {
            Icon(
                imageVector = statusIcon,
                contentDescription = "Playback Status",
                tint = statusColor,
                modifier = Modifier.size(13.dp)
            )
        } else {
            CircularProgressIndicator(
                color = statusColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = statusText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
    }
}

@Composable
private fun StationArtworkCard(
    faviconUrl: String?,
    isPlaying: Boolean,
    size: Dp
) {
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "artwork_scale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = artworkScale
                scaleY = artworkScale
            }
            .shadow(
                elevation = if (isPlaying) 24.dp else 10.dp,
                shape = ArtworkCornerShape,
                ambientColor = PrimaryPink.copy(alpha = 0.5f),
                spotColor = PrimaryPink.copy(alpha = 0.45f)
            )
            .clip(ArtworkCornerShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(CardBg, SearchBg)
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = if (isPlaying) {
                        listOf(PrimaryPink.copy(alpha = 0.7f), SecondaryPink.copy(alpha = 0.25f))
                    } else {
                        listOf(TextPrimary.copy(alpha = 0.12f), TextPrimary.copy(alpha = 0.04f))
                    }
                ),
                shape = ArtworkCornerShape
            ),
        contentAlignment = Alignment.Center
    ) {
        val hasValidFavicon = remember(faviconUrl) { isValidFaviconUrl(faviconUrl) }
        if (hasValidFavicon) {
            val context = LocalContext.current
            val imageRequest = remember(faviconUrl) {
                ImageRequest.Builder(context.applicationContext)
                    .data(faviconUrl)
                    .size(512)
                    .precision(Precision.INEXACT)
                    .allowHardware(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = "Station Cover Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size * 0.48f)
                    .clip(CircleShape)
                    .background(PrimaryPink.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = "Default Radio Artwork",
                    tint = PrimaryPink,
                    modifier = Modifier.size(size * 0.26f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StationMetadataSection(
    currentName: String?,
    currentArtist: String?,
    currentTrackTitle: String?,
    hasStation: Boolean,
    topTags: Triple<String?, String?, String?>?,
    streamSpec: String?,
    playbackState: PlaybackState,
    compact: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val stationTitle = currentName?.takeIf { it.isNotBlank() } ?: stringLoc("select_station")
        Text(
            text = stationTitle,
            fontSize = if (compact) 19.sp else 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = if (compact) 23.sp else 30.sp,
            modifier = Modifier.testTag("fullscreen_player_station_name")
        )

        Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))

        val trackInfoText = when {
            !currentArtist.isNullOrBlank() && !currentTrackTitle.isNullOrBlank() ->
                "$currentArtist — $currentTrackTitle"
            !currentTrackTitle.isNullOrBlank() -> currentTrackTitle
            !currentArtist.isNullOrBlank() -> currentArtist
            hasStation -> stringLoc("no_track_metadata")
            else -> stringLoc("tap_to_play")
        }

        val hasTrackMeta = !currentArtist.isNullOrBlank() || !currentTrackTitle.isNullOrBlank()

        Text(
            text = trackInfoText,
            fontSize = if (compact) 14.sp else 16.sp,
            fontWeight = if (hasTrackMeta) FontWeight.SemiBold else FontWeight.Medium,
            color = if (hasTrackMeta) PrimaryPink else TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .basicMarquee()
                .testTag("fullscreen_player_track_info")
        )

        val tag1 = topTags?.first
        val tag2 = topTags?.second
        val tag3 = topTags?.third
        if (tag1 != null || streamSpec != null) {
            Spacer(modifier = Modifier.height(if (compact) 8.dp else 12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (tag1 != null) TagChip(text = tag1, isActive = true)
                if (tag2 != null) TagChip(text = tag2, isActive = true)
                if (tag3 != null && streamSpec == null) TagChip(text = tag3, isActive = true)
                if (streamSpec != null) TagChip(text = streamSpec, isActive = false)
            }
        }

        if (!compact) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.height(28.dp),
                contentAlignment = Alignment.Center
            ) {
                if (playbackState == PlaybackState.PLAYING) {
                    EqualizerAnimation()
                }
            }
        }
    }
}

@Composable
private fun PlayerControlsRow(
    hasStation: Boolean,
    playbackState: PlaybackState,
    onPrev: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrev,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(56.dp)
                .clip(CircleShape)
                .background(CardBg)
                .border(1.dp, TextPrimary.copy(alpha = 0.08f), CircleShape)
                .testTag("fullscreen_player_prev_btn")
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous Station",
                tint = TextPrimary,
                modifier = Modifier.size(30.dp)
            )
        }

        val isPlayingOrBuffering = playbackState == PlaybackState.PLAYING || playbackState == PlaybackState.BUFFERING

        Box(
            modifier = Modifier
                .size(76.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = PrimaryPink.copy(alpha = 0.55f),
                    spotColor = PrimaryPink.copy(alpha = 0.55f)
                )
                .clip(CircleShape)
                .background(PlayButtonGradient)
                .clickable(enabled = hasStation || playbackState != PlaybackState.IDLE, onClick = onTogglePlay)
                .testTag("fullscreen_player_play_btn"),
            contentAlignment = Alignment.Center
        ) {
            if (playbackState == PlaybackState.BUFFERING) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Icon(
                    imageVector = if (isPlayingOrBuffering) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlayingOrBuffering) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        IconButton(
            onClick = onNext,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(56.dp)
                .clip(CircleShape)
                .background(CardBg)
                .border(1.dp, TextPrimary.copy(alpha = 0.08f), CircleShape)
                .testTag("fullscreen_player_next_btn")
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next Station",
                tint = TextPrimary,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
