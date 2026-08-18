package com.offpolice.webradiobot.ui

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.offpolice.webradiobot.data.ApiStation
import com.offpolice.webradiobot.data.FavoriteStation
import com.offpolice.webradiobot.player.PlaybackState
import com.offpolice.webradiobot.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch

// Full screen loaders representing loadingScreen element from user code
@Composable
fun FullScreenLoadingScreen(
    progress: Float,
    statusText: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated concentric pulsing rings
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                LoaderPulseAnimation()
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(PrimaryPink, SecondaryPink),
                                radius = 100f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = "Radio Logo Icon",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "WebRadioBot",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryPink,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringLoc("loading_tagline"),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(SearchBg)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(SecondaryPink, LightPink)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun LoaderPulseAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")

    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1_scale"
    )

    val pulseOpacity1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1_opacity"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2_scale"
    )

    val pulseOpacity2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2_opacity"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Ring 1
                drawCircle(
                    color = PrimaryPink.copy(alpha = pulseOpacity1),
                    radius = (size.minDimension / 2f) * pulseScale1,
                    style = Stroke(width = 2.dp.toPx())
                )
                // Staggered Ring 2 (outer)
                drawCircle(
                    color = PrimaryPink.copy(alpha = pulseOpacity2 * 0.5f),
                    radius = (size.minDimension / 2f) * pulseScale2 * 1.2f,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
    )
}

// Dancing Equalizer bar animations
@Composable
fun EqualizerAnimation(modifier: Modifier = Modifier, count: Int = 7) {
    Row(
        modifier = modifier.height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "equalizer_pulse")

        val animTimes = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.3f, 0.6f, 0.5f)

        for (i in 0 until count) {
            val ratio by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (1000 * animTimes[i % animTimes.size]).toInt(),
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "eq_bar_$i"
            )

            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleY = ratio
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(SecondaryPink, LightPink)
                        )
                    )
            )
        }
    }
}

// Compact persistent track bar inside the list
@Composable
fun CompactEqualizerAnimation(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "compact_eq")
        val animTimes = listOf(0.3f, 0.6f, 0.4f, 0.5f)

        for (i in 0 until 4) {
            val ratio by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (500 * animTimes[i % animTimes.size]).toInt(),
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "comp_eq_bar_$i"
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleY = ratio
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(PrimaryPink)
            )
        }
    }
}

// Scroll to top button which animates visibility with bouncy scale animation
@Composable
fun ScrollToTopButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(animationSpec = tween(250)),
        exit = scaleOut(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = SecondaryPink,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .size(48.dp)
                .testTag("scroll_to_top")
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "Scroll to top icon",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// Main Top App bar
@Composable
fun AppHeader(
    activeTab: AppTab,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    isSearchVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var localText by remember(searchQuery) { mutableStateOf(searchQuery) }

    val title = when (activeTab) {
        AppTab.RADIO -> "WebRadioBot"
        AppTab.FAVORITES -> stringLoc("tab_favorites")
        AppTab.SETTINGS -> stringLoc("tab_settings")
    }

    Column(
        modifier = modifier
            .background(CardBg)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPink,
                    textAlign = TextAlign.Center
                )
            }

            if (isSearchVisible) {
                Spacer(modifier = Modifier.height(8.dp))
                var isFocused by remember { mutableStateOf(false) }
                val borderColor = PrimaryPink
                val borderWidth = 1.5.dp

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(SearchBg)
                        .border(borderWidth, borderColor, RoundedCornerShape(26.dp))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            onSearchSubmit(localText)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = if (isFocused) PrimaryPink else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    BasicTextField2_Placeholder(
                        value = localText,
                        onValueChange = {
                            localText = it
                            onSearchChange(it)
                        },
                        onSearch = {
                            focusManager.clearFocus()
                            onSearchSubmit(localText)
                        },
                        placeholderText = stringLoc("search_placeholder"),
                        onFocusChanged = { isFocused = it },
                        modifier = Modifier.weight(1f)
                    )

                    if (localText.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                localText = ""
                                onSearchChange("")
                                onSearchSubmit("")
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Search",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        HorizontalDivider(thickness = 1.dp, color = DividerColor)
    }
}

// Quick fallback basic textfield placeholder implementation with ImeAction.Search
@Composable
fun BasicTextField2_Placeholder(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    placeholderText: String,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) {
            Text(
                text = placeholderText,
                color = TextSecondary,
                fontSize = 16.sp
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = TextPrimary,
                fontSize = 16.sp
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
                keyboardType = KeyboardType.Text
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearch()
                }
            ),
            cursorBrush = SolidColor(PrimaryPink),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    onFocusChanged?.invoke(focusState.isFocused)
                }
                .testTag("search_input")
        )
    }
}

// Section title mimicking .section-title class
@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Section Head Icon",
            tint = SecondaryPink,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            fontSize = 14.sp,
            letterSpacing = 0.5.sp
        )
    }
}

// Station list item layout with high-fidelity visuals
@Composable
fun StationCard(
    name: String,
    faviconUrl: String?,
    tags: String?,
    urlResolved: String,
    isActive: Boolean,
    isFavorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false
) {
    val cardBackground = if (isActive) ActiveCardBg else CardBg
    val borderBrush = if (isActive) {
        val infiniteTransition = rememberInfiniteTransition(label = "active_pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        Brush.linearGradient(
            colors = listOf(PrimaryPink.copy(alpha = alpha), SecondaryPink.copy(alpha = alpha * 0.3f))
        )
    } else {
        remember {
            Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.03f), Color.White.copy(alpha = 0.03f))
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackground)
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect)
            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 12.dp)
            .testTag("station_card_${urlResolved.hashCode()}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover Art Container
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SearchBg),
            contentAlignment = Alignment.Center
        ) {
            if (!faviconUrl.isNullOrBlank()) {
                var isError by remember(faviconUrl) { mutableStateOf(false) }
                if (isError) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = "Standard Fallback Radio Cover Icon",
                        tint = SecondaryPink,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    val context = LocalContext.current
                    val imageRequest = remember(faviconUrl) {
                        ImageRequest.Builder(context)
                            .data(faviconUrl)
                            .crossfade(true)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "Station Favicon Cover",
                        contentScale = ContentScale.Crop,
                        onError = { isError = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = "Standard Fallback Radio Cover Icon",
                    tint = SecondaryPink,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Station Details Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name.trim().ifEmpty { stringLoc("no_title") },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isActive && isPlaying) {
                    Spacer(modifier = Modifier.width(6.dp))
                    CompactEqualizerAnimation()
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Badges / Tags row (Optimized using remember for string splitting operations)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val parsedTags = remember(tags) {
                    tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.distinct()?.take(3) ?: emptyList()
                }
                if (parsedTags.isNotEmpty()) {
                    parsedTags.forEach { tag ->
                        TagChip(text = tag, isActive = isActive)
                    }
                } else {
                    TagChip(text = "radio", isActive = isActive)
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Favorite Toggle button
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .size(36.dp)
                .testTag("fav_btn_${urlResolved.hashCode()}")
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) PrimaryPink else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun TagChip(text: String, isActive: Boolean) {
    Box(
        modifier = Modifier
            .background(
                color = if (isActive) Color(0xFF3A2A33) else SearchBg,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = if (isActive) Color(0xFFE0B0C0) else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Universal Persistent Player control block at the bottom of the Screen
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerBar(
    currentName: String?,
    currentArtist: String? = null,
    currentTrackTitle: String? = null,
    currentFavicon: String? = null,
    playbackState: PlaybackState,
    onPlayToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CardBg)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("player_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Large Play Button
        Button(
            onClick = onPlayToggle,
            colors = ButtonDefaults.buttonColors(
                containerColor = SecondaryPink,
                contentColor = Color.White
            ),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .size(42.dp)
                .testTag("play_toggle_btn")
        ) {
            val icon = when (playbackState) {
                PlaybackState.PLAYING -> Icons.Default.Pause
                else -> Icons.Default.PlayArrow
            }
            Icon(
                imageVector = icon,
                contentDescription = "Play Control Toggle",
                modifier = Modifier.size(24.dp)
            )
        }

        // Running track name (station name on top, artist + track title below)
        Column(
            modifier = Modifier
                .weight(1f)
                .testTag("playing_station_info"),
            verticalArrangement = Arrangement.Center
        ) {
            val stationNameText = currentName ?: stringLoc("select_station")
            val trackInfoText = when {
                !currentArtist.isNullOrBlank() && !currentTrackTitle.isNullOrBlank() -> "${currentArtist} — ${currentTrackTitle}"
                !currentTrackTitle.isNullOrBlank() -> currentTrackTitle
                !currentArtist.isNullOrBlank() -> currentArtist
                else -> null
            }

            Text(
                text = stationNameText,
                modifier = Modifier
                    .basicMarquee()
                    .testTag("playing_station_title"),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!trackInfoText.isNullOrBlank()) {
                Text(
                    text = trackInfoText,
                    modifier = Modifier
                        .basicMarquee()
                        .testTag("playing_track_info"),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = PrimaryPink
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Status Area Indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.wrapContentWidth()
        ) {
            val statusColor = when (playbackState) {
                PlaybackState.PLAYING -> Color(0xFF4CAF50) // Green
                PlaybackState.BUFFERING -> LightPink
                PlaybackState.PAUSED -> Color(0xFFFFA000) // Orange
                PlaybackState.ERROR -> Color(0xFFF44336) // Red
                PlaybackState.IDLE -> TextSecondary
            }

            val statusText = when (playbackState) {
                PlaybackState.PLAYING -> stringLoc("playing")
                PlaybackState.BUFFERING -> stringLoc("buffering")
                PlaybackState.PAUSED -> stringLoc("paused")
                PlaybackState.ERROR -> stringLoc("error")
                PlaybackState.IDLE -> stringLoc("idle")
            }

            val statusIcon = when (playbackState) {
                PlaybackState.PLAYING -> Icons.Default.VolumeUp
                PlaybackState.BUFFERING -> Icons.Default.Sync
                PlaybackState.PAUSED -> Icons.Default.PauseCircle
                PlaybackState.ERROR -> Icons.Default.Error
                PlaybackState.IDLE -> Icons.Default.Circle
            }

            Icon(
                imageVector = statusIcon,
                contentDescription = "Status indicator",
                tint = statusColor,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = statusText,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// Tab Switching Bottom bar
@Composable
fun AppBottomNav(
    activeTab: AppTab,
    onTabSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = CardBg,
        contentColor = TextSecondary,
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.testTag("bottom_nav")
    ) {
        NavigationBarItem(
            selected = activeTab == AppTab.RADIO,
            onClick = { onTabSelect(AppTab.RADIO) },
            icon = {
                Icon(
                    imageVector = if (activeTab == AppTab.RADIO) Icons.Filled.Radio else Icons.Outlined.Radio,
                    contentDescription = "Radio Tab icon"
                )
            },
            label = { Text(stringLoc("tab_radio")) },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryPink,
                selectedTextColor = PrimaryPink,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryPink.copy(alpha = 0.1f)
            ),
            modifier = Modifier.testTag("nav_item_radio")
        )

        NavigationBarItem(
            selected = activeTab == AppTab.FAVORITES,
            onClick = { onTabSelect(AppTab.FAVORITES) },
            icon = {
                Icon(
                    imageVector = if (activeTab == AppTab.FAVORITES) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorites Tab icon"
                )
            },
            label = { Text(stringLoc("tab_favorites")) },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryPink,
                selectedTextColor = PrimaryPink,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryPink.copy(alpha = 0.1f)
            ),
            modifier = Modifier.testTag("nav_item_favorites")
        )

        NavigationBarItem(
            selected = activeTab == AppTab.SETTINGS,
            onClick = { onTabSelect(AppTab.SETTINGS) },
            icon = {
                Icon(
                    imageVector = if (activeTab == AppTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Settings Tab icon"
                )
            },
            label = { Text(stringLoc("tab_settings")) },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryPink,
                selectedTextColor = PrimaryPink,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = PrimaryPink.copy(alpha = 0.1f)
            ),
            modifier = Modifier.testTag("nav_item_settings")
        )
    }
}

// Clean Empty Layout
@Composable
fun EmptyPlaceholder(message: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Empty list icon",
            tint = TextMuted,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = TextSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

// Individual Tab Views
@Composable
fun RadioTab(
    stations: List<ApiStation>,
    favorites: List<FavoriteStation>,
    activeUrl: String?,
    playbackState: PlaybackState,
    isLoading: Boolean,
    hasMore: Boolean,
    onStationSelect: (ApiStation) -> Unit,
    onToggleFavorite: (ApiStation) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    // Precompute a hashed Set of favorite station URLs to enable O(1) lookups during scroll
    val favoriteUrls = remember(favorites) {
        favorites.map { it.urlResolved }.toSet()
    }

    // Infinite scroll detection
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItemsCount = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            // Trigger load-more when scrolled 80% through the loaded list
            totalItemsCount > 0 && lastVisibleItemIndex >= (totalItemsCount - 10)
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoading && hasMore) {
            onLoadMore()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                SectionHeader(title = stringLoc("all_stations"), icon = Icons.Default.Radio)
            }

            if (stations.isEmpty() && !isLoading) {
                item {
                    EmptyPlaceholder(
                        message = stringLoc("nothing_found"),
                        icon = Icons.Default.SearchOff
                    )
                }
            } else {
                items(stations, key = { it.url_resolved }) { station ->
                    val isActive = activeUrl == station.url_resolved
                    val isFav = favoriteUrls.contains(station.url_resolved)

                    // Optimize lambdas to prevent unnecessary recompositions of StationCard on scroll
                    val onSelectLambda = remember(station.url_resolved) {
                        { onStationSelect(station) }
                    }
                    val onToggleFavLambda = remember(station.url_resolved) {
                        { onToggleFavorite(station) }
                    }

                    StationCard(
                        name = station.name,
                        faviconUrl = station.favicon,
                        tags = station.tags,
                        urlResolved = station.url_resolved,
                        isActive = isActive,
                        isFavorite = isFav,
                        onSelect = onSelectLambda,
                        onToggleFavorite = onToggleFavLambda,
                        isPlaying = playbackState == PlaybackState.PLAYING
                    )
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = PrimaryPink,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Scroll to Top FAB Button overlay
        val isScrollBtnVisible by remember {
            derivedStateOf { listState.firstVisibleItemIndex > 4 }
        }

        ScrollToTopButton(
            visible = isScrollBtnVisible,
            onClick = {
                scope.launch { listState.animateScrollToItem(0) }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp)
        )
    }
}

@Composable
fun FavoritesTab(
    favorites: List<FavoriteStation>,
    activeUrl: String?,
    playbackState: PlaybackState,
    onStationSelect: (FavoriteStation) -> Unit,
    onToggleFavorite: (FavoriteStation) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (favorites.isEmpty()) {
            EmptyPlaceholder(
                message = stringLoc("empty_favorites"),
                icon = Icons.Outlined.FavoriteBorder
            )
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favorites, key = { it.urlResolved }) { station ->
                    val isActive = activeUrl == station.urlResolved

                    // Optimize lambdas to prevent unnecessary recompositions of StationCard on scroll
                    val onSelectLambda = remember(station.urlResolved) {
                        { onStationSelect(station) }
                    }
                    val onToggleFavLambda = remember(station.urlResolved) {
                        { onToggleFavorite(station) }
                    }

                    StationCard(
                        name = station.name,
                        faviconUrl = station.favicon,
                        tags = station.tags,
                        urlResolved = station.urlResolved,
                        isActive = isActive,
                        isFavorite = true,
                        onSelect = onSelectLambda,
                        onToggleFavorite = onToggleFavLambda,
                        isPlaying = playbackState == PlaybackState.PLAYING
                    )
                }
            }

            // Scroll to Top FAB Button overlay
            val isScrollBtnVisible by remember {
                derivedStateOf { listState.firstVisibleItemIndex > 4 }
            }

            ScrollToTopButton(
                visible = isScrollBtnVisible,
                onClick = {
                    scope.launch { listState.animateScrollToItem(0) }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 20.dp, end = 20.dp)
            )
        }
    }
}

@Composable
fun <T> SettingsOptionCard(
    title: String,
    options: List<Triple<T, String, String?>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, TextPrimary.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryPink,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        options.forEach { (option, label, subtitle) ->
            val isSelected = option == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) PrimaryPink.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onSelect(option) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = PrimaryPink,
                        unselectedColor = TextSecondary
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = TextPrimary
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelectorRow(
    selected: AppLanguageSetting,
    options: List<Triple<AppLanguageSetting, String, String?>>,
    onClick: () -> Unit
) {
    val currentOption = options.find { it.first == selected }
    val currentLabel = currentOption?.let { stringLoc(it.second) } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, TextPrimary.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringLoc("setting_language"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryPink
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = currentLabel,
                fontSize = 14.sp,
                color = TextSecondary
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Open language selection screen",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun LanguageScreen(
    selected: AppLanguageSetting,
    options: List<Triple<AppLanguageSetting, String, String?>>,
    onSelect: (AppLanguageSetting) -> Unit,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom Header Bar matching Android standards & screenshot
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryPink,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { onBack() }
                        .testTag("language_back_button")
                )
                Text(
                    text = stringLoc("setting_language"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPink,
                    textAlign = TextAlign.Center
                )
            }
            HorizontalDivider(thickness = 1.dp, color = DividerColor)
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .border(1.dp, TextPrimary.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    options.forEach { (option, labelKey, hintKey) ->
                        val isSelected = option == selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimaryPink.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { onSelect(option) }
                                .padding(vertical = 16.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelect(option) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PrimaryPink,
                                    unselectedColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("radio_lang_${option.name}")
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringLoc(labelKey),
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = TextPrimary
                                )
                                if (hintKey != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringLoc(hintKey),
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SocialLinkRow(
    title: String,
    handle: String,
    icon: ImageVector,
    url: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PrimaryPink.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryPink,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = handle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Icon(
            imageVector = Icons.Default.Launch,
            contentDescription = "Open Link",
            tint = TextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SettingsTab(viewModel: RadioViewModel, modifier: Modifier = Modifier) {
    val themeSetting by viewModel.themeSetting.collectAsStateWithLifecycle()
    val languageSetting by viewModel.languageSetting.collectAsStateWithLifecycle()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    val languageOptions = remember {
        listOf(
            Triple(AppLanguageSetting.AUTO, "language_auto", "language_default_hint"),
            Triple(AppLanguageSetting.EN, "language_en", null),
            Triple(AppLanguageSetting.RU, "language_ru", null),
            Triple(AppLanguageSetting.UK, "language_uk", null)
        )
    }

    val themeOptions = remember {
        listOf(
            Triple(AppThemeSetting.SYSTEM, "theme_system", "theme_default_hint"),
            Triple(AppThemeSetting.LIGHT, "theme_light", null),
            Triple(AppThemeSetting.DARK, "theme_dark", null)
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Language settings
        item {
            LanguageSelectorRow(
                selected = languageSetting,
                options = languageOptions,
                onClick = { viewModel.showLanguageScreen(true) }
            )
        }

        // Theme settings
        item {
            SettingsOptionCard(
                title = stringLoc("setting_theme"),
                options = themeOptions.map { (opt, labelKey, hintKey) ->
                    Triple(opt, stringLoc(labelKey), hintKey?.let { stringLoc(it) })
                },
                selected = themeSetting,
                onSelect = { viewModel.setThemeSetting(it) }
            )
        }

        // About section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .border(1.dp, TextPrimary.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large About Icon Header
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = null,
                    tint = SecondaryPink,
                    modifier = Modifier
                        .size(56.dp)
                        .padding(bottom = 8.dp)
                )

                Text(
                    text = "WebRadioBot",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringLoc("about_text"),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Follow Author title
                Text(
                    text = stringLoc("socials_heading").uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPink,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // Social platforms list
                SocialLinkRow(
                    title = "Telegram",
                    handle = "@OFFpolice",
                    icon = SocialIcons.Telegram,
                    url = "https://t.me/OFFpolice"
                ) {
                    uriHandler.openUri("https://t.me/OFFpolice")
                }

                SocialLinkRow(
                    title = "X (Twitter)",
                    handle = "@OFFpolice2077",
                    icon = SocialIcons.X,
                    url = "https://x.com/OFFpolice2077"
                ) {
                    uriHandler.openUri("https://x.com/OFFpolice2077")
                }

                SocialLinkRow(
                    title = "Instagram",
                    handle = "@offpolice2077",
                    icon = SocialIcons.Instagram,
                    url = "https://www.instagram.com/offpolice2077"
                ) {
                    uriHandler.openUri("https://www.instagram.com/offpolice2077")
                }

                SocialLinkRow(
                    title = "WebRadioBot",
                    handle = "t.me/Web_radio_bot/app",
                    icon = SocialIcons.Telegram,
                    url = "https://t.me/Web_radio_bot/app"
                ) {
                    uriHandler.openUri("https://t.me/Web_radio_bot/app")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dev API URL element
                Text(
                    text = stringLoc("link_dev_api"),
                    color = PrimaryPink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { uriHandler.openUri("https://api.radio-browser.info/") }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringLoc("version_info"),
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
