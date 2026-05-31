package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.ApiStation
import com.example.data.FavoriteStation
import com.example.player.PlaybackState
import com.example.ui.theme.*
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
            .background(Color(0xFF0E0E0E)),
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
                text = "Тысячи станций со всего мира",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            EqualizerAnimation()

            Spacer(modifier = Modifier.height(32.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF2A2A2A))
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
        val heightsValues = listOf(14.dp, 22.dp, 18.dp, 26.dp, 12.dp, 20.dp, 16.dp)

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

            val currentHeight = heightsValues[i % heightsValues.size]
            val animatedHeight = 5.dp + (currentHeight - 5.dp) * ratio

            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(animatedHeight)
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

            val animatedHeight = 3.dp + (12.dp - 3.dp) * ratio

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(animatedHeight)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(PrimaryPink)
            )
        }
    }
}

// Scroll to top button which animates visibility
@Composable
fun ScrollToTopButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
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
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isSearchVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CardBg)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "WebRadioBot",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPink,
                textAlign = TextAlign.Center
            )
        }

        if (isSearchVisible) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(SearchBg)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Icon",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                BasicTextField2_Placeholder(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholderText = "Поиск станций...",
                    modifier = Modifier.weight(1f)
                )

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChange("") },
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
}

// Quick fallback basic textfield placeholder implementation
@Composable
fun BasicTextField2_Placeholder(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
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
                color = Color.White,
                fontSize = 16.sp
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
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
            color = Color(0xFFE0E0E0),
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
    modifier: Modifier = Modifier
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
                .background(Color(0xFF2C2C2C)),
            contentAlignment = Alignment.Center
        ) {
            if (!faviconUrl.isNullOrBlank()) {
                var isError by remember { mutableStateOf(false) }
                if (isError) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = "Standard Fallback Radio Cover Icon",
                        tint = SecondaryPink,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(faviconUrl)
                            .crossfade(true)
                            .build(),
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
                    text = name.trim().ifEmpty { "Без названия" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isActive) {
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
    playbackState: PlaybackState,
    onPlayToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CardBg)
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("player_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                .size(44.dp)
                .testTag("play_toggle_btn")
        ) {
            val icon = when (playbackState) {
                PlaybackState.PLAYING -> Icons.Default.Pause
                else -> Icons.Default.PlayArrow
            }
            Icon(
                imageVector = icon,
                contentDescription = "Play Control Toggle",
                modifier = Modifier.size(26.dp)
            )
        }

        // Running track name (marquee!)
        Text(
            text = currentName ?: "Выберите станцию",
            modifier = Modifier
                .weight(1f)
                .basicMarquee()
                .testTag("playing_station_title"),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            ),
            maxLines = 1
        )

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
                PlaybackState.PLAYING -> "Играет"
                PlaybackState.BUFFERING -> "Буфер..."
                PlaybackState.PAUSED -> "Пауза"
                PlaybackState.ERROR -> "Ошибка"
                PlaybackState.IDLE -> "Ожидание"
            }

            val statusIcon = when (playbackState) {
                PlaybackState.PLAYING -> Icons.Default.VolumeUp
                PlaybackState.BUFFERING -> Icons.Default.Sync
                PlaybackState.PAUSED -> Icons.Default.PauseCircle
                PlaybackState.ERROR -> Icons.Default.Error
                PlaybackState.IDLE -> Icons.Default.Circle
            }

            // Simple conditional spinning animator for buffering status
            val animateRotationMultiplier = rememberInfiniteTransition(label = "spin_angle")
            val spinAngle by animateRotationMultiplier.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "loading_angle"
            )

            Icon(
                imageVector = statusIcon,
                contentDescription = "Status indicator",
                tint = statusColor,
                modifier = Modifier
                    .size(16.dp)
                    .drawBehind {
                        if (playbackState == PlaybackState.BUFFERING) {
                            // Apply custom rotation visually via Modifier or raw canvas rotation if needed.
                            // We will simply let compose handle the rotation easily or keep static icon.
                        }
                    }
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
        containerColor = Color(0xFF1A1A1A),
        contentColor = TextSecondary,
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
            label = { Text("Радио") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryPink,
                selectedTextColor = PrimaryPink,
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
            label = { Text("Избранное") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryPink,
                selectedTextColor = PrimaryPink,
                indicatorColor = PrimaryPink.copy(alpha = 0.1f)
            ),
            modifier = Modifier.testTag("nav_item_favorites")
        )

        NavigationBarItem(
            selected = activeTab == AppTab.ABOUT,
            onClick = { onTabSelect(AppTab.ABOUT) },
            icon = {
                Icon(
                    imageVector = if (activeTab == AppTab.ABOUT) Icons.Filled.Info else Icons.Outlined.Info,
                    contentDescription = "About Tab icon"
                )
            },
            label = { Text("О нас") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryPink,
                selectedTextColor = PrimaryPink,
                indicatorColor = PrimaryPink.copy(alpha = 0.1f)
            ),
            modifier = Modifier.testTag("nav_item_about")
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
    isLoading: Boolean,
    hasMore: Boolean,
    onStationSelect: (ApiStation) -> Unit,
    onToggleFavorite: (ApiStation) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

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
                SectionHeader(title = "Все радиостанции", icon = Icons.Default.Radio)
            }

            if (stations.isEmpty() && !isLoading) {
                item {
                    EmptyPlaceholder(
                        message = "Ничего не найдено",
                        icon = Icons.Default.SearchOff
                    )
                }
            } else {
                items(stations, key = { it.url_resolved }) { station ->
                    val isActive = activeUrl == station.url_resolved
                    val isFav = favorites.any { it.urlResolved == station.url_resolved }

                    StationCard(
                        name = station.name,
                        faviconUrl = station.favicon,
                        tags = station.tags,
                        urlResolved = station.url_resolved,
                        isActive = isActive,
                        isFavorite = isFav,
                        onSelect = { onStationSelect(station) },
                        onToggleFavorite = { onToggleFavorite(station) }
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
    onStationSelect: (FavoriteStation) -> Unit,
    onToggleFavorite: (FavoriteStation) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (favorites.isEmpty()) {
            EmptyPlaceholder(
                message = "Нет избранных станций",
                icon = Icons.Outlined.FavoriteBorder
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    SectionHeader(title = "Избранное", icon = Icons.Default.Favorite)
                }

                items(favorites, key = { it.urlResolved }) { station ->
                    val isActive = activeUrl == station.urlResolved

                    StationCard(
                        name = station.name,
                        faviconUrl = station.favicon,
                        tags = station.tags,
                        urlResolved = station.urlResolved,
                        isActive = isActive,
                        isFavorite = true,
                        onSelect = { onStationSelect(station) },
                        onToggleFavorite = { onToggleFavorite(station) }
                    )
                }
            }
        }
    }
}

@Composable
fun AboutTab(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Radio,
            contentDescription = "Audio Wave visualizer indicator",
            tint = SecondaryPink,
            modifier = Modifier
                .size(72.dp)
                .padding(bottom = 16.dp)
        )

        Text(
            text = "WebRadioBot",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Слушайте тысячи радиостанции со всего мира. Добавляйте любимые в избранное для быстрого доступа.",
            color = TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Version: 1.0.0 · Android App",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
