package com.offpolice.webradiobot

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offpolice.webradiobot.ui.*
import com.offpolice.webradiobot.ui.theme.*

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context?) {
        val nextBase = if (newBase != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            newBase.createAttributionContext("webradio")
        } else {
            newBase
        }
        super.attachBaseContext(nextBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            // Initialize the ViewModel using the custom factory
            val viewModel: RadioViewModel by viewModels {
                RadioViewModel.Factory(application)
            }

            val languageSetting by viewModel.languageSetting.collectAsStateWithLifecycle()
            val themeSetting by viewModel.themeSetting.collectAsStateWithLifecycle()

            androidx.compose.runtime.CompositionLocalProvider(
                LocalLanguageSetting provides languageSetting
            ) {
                MyApplicationTheme(themeSetting = themeSetting) {
                    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
                    val connectionProgress by viewModel.connectionProgress.collectAsStateWithLifecycle()
                    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

                    AnimatedContent(
                        targetState = isConnecting,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "connection_transition"
                    ) { connecting ->
                        if (connecting) {
                            val statusText = when (connectionState) {
                                ConnectionState.INITIALIZING -> stringLoc("state_init")
                                ConnectionState.LOADING_RESOURCES -> stringLoc("state_resources")
                                ConnectionState.CONNECTING_SERVER -> stringLoc("state_connecting")
                                ConnectionState.GETTING_STATIONS -> stringLoc("state_stations")
                                ConnectionState.READY -> stringLoc("state_ready")
                            }
                            FullScreenLoadingScreen(
                                progress = connectionProgress / 100f,
                                statusText = statusText
                            )
                        } else {
                            MainAppContent(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: RadioViewModel) {
    val isLanguageScreenOpen by viewModel.isLanguageScreenOpen.collectAsStateWithLifecycle()
    val areAnimationsEnabled by viewModel.animationsEnabled.collectAsStateWithLifecycle()
    val areHapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = isLanguageScreenOpen,
        transitionSpec = {
            if (!areAnimationsEnabled) {
                EnterTransition.None togetherWith ExitTransition.None
            } else if (targetState) {
                // Opening LanguageScreen: slide in from right with fade and scale
                (slideInHorizontally(
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    initialOffsetX = { width -> width }
                ) + fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f)) togetherWith
                (slideOutHorizontally(
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    targetOffsetX = { width -> -width / 4 }
                ) + fadeOut(animationSpec = tween(250)) + scaleOut(targetScale = 0.95f))
            } else {
                // Closing LanguageScreen: slide out to right
                (slideInHorizontally(
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    initialOffsetX = { width -> -width / 4 }
                ) + fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f)) togetherWith
                (slideOutHorizontally(
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    targetOffsetX = { width -> width }
                ) + fadeOut(animationSpec = tween(250)) + scaleOut(targetScale = 0.95f))
            }
        },
        label = "language_screen_transition"
    ) { isOpen ->
        if (isOpen) {
            val languageOptions = remember {
                listOf(
                    Triple(AppLanguageSetting.AUTO, "language_auto", "language_default_hint"),
                    Triple(AppLanguageSetting.EN, "language_en", null),
                    Triple(AppLanguageSetting.RU, "language_ru", null),
                    Triple(AppLanguageSetting.UK, "language_uk", null)
                )
            }
            val languageSetting by viewModel.languageSetting.collectAsStateWithLifecycle()
            BackHandler {
                viewModel.showLanguageScreen(false)
            }
            LanguageScreen(
                selected = languageSetting,
                options = languageOptions,
                onSelect = {
                    viewModel.setLanguageSetting(it)
                    viewModel.showLanguageScreen(false)
                },
                onBack = {
                    viewModel.showLanguageScreen(false)
                }
            )
        } else {
            val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
            val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
            val stations by viewModel.stations.collectAsStateWithLifecycle()
            val favorites by viewModel.favorites.collectAsStateWithLifecycle()
            val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
            val hasMore by viewModel.hasMore.collectAsStateWithLifecycle()

            // Player state management flows
            val currentUrl by viewModel.playerManager.currentUrl.collectAsStateWithLifecycle()
            val currentName by viewModel.playerManager.currentName.collectAsStateWithLifecycle()
            val currentFavicon by viewModel.playerManager.currentFavicon.collectAsStateWithLifecycle()
            val playbackState by viewModel.playerManager.playbackState.collectAsStateWithLifecycle()

            // Intercept back actions. If we are on secondary tabs, back press directs us home to Radio Tab.
            if (activeTab != AppTab.RADIO) {
                BackHandler {
                    viewModel.selectTab(AppTab.RADIO)
                }
            } else if (searchQuery.isNotEmpty()) {
                BackHandler {
                    viewModel.updateSearchQuery("")
                }
            }

            Scaffold(
                topBar = {
                    AppHeader(
                        activeTab = activeTab,
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.updateSearchQuery(it) },
                        isSearchVisible = activeTab == AppTab.RADIO,
                        animationsEnabled = areAnimationsEnabled
                    )
                },
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .background(CardBg)
                            .navigationBarsPadding()
                    ) {
                        PlayerBar(
                            currentName = currentName,
                            playbackState = playbackState,
                            onPlayToggle = { viewModel.togglePlay() }
                        )
                        AppBottomNav(
                            activeTab = activeTab,
                            onTabSelect = { viewModel.selectTab(it) },
                            animationsEnabled = areAnimationsEnabled,
                            hapticsEnabled = areHapticsEnabled
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            if (!areAnimationsEnabled) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                val isForward = targetState.ordinal > initialState.ordinal
                                if (isForward) {
                                    (slideInHorizontally(
                                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                                        initialOffsetX = { width -> width }
                                    ) + fadeIn(animationSpec = tween(250)) + scaleIn(initialScale = 0.96f)) togetherWith
                                    (slideOutHorizontally(
                                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                                        targetOffsetX = { width -> -width / 3 }
                                    ) + fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.96f))
                                } else {
                                    (slideInHorizontally(
                                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                                        initialOffsetX = { width -> -width }
                                    ) + fadeIn(animationSpec = tween(250)) + scaleIn(initialScale = 0.96f)) togetherWith
                                    (slideOutHorizontally(
                                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                                        targetOffsetX = { width -> width / 3 }
                                    ) + fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.96f))
                                }
                            }
                        },
                        label = "tab_content_transition"
                    ) { tab ->
                        when (tab) {
                            AppTab.RADIO -> {
                                RadioTab(
                                    stations = stations,
                                    favorites = favorites,
                                    activeUrl = currentUrl,
                                    playbackState = playbackState,
                                    isLoading = isLoading,
                                    hasMore = hasMore,
                                    onStationSelect = { station ->
                                        viewModel.selectStation(station.url_resolved, station.name, station.favicon)
                                    },
                                    onToggleFavorite = { station ->
                                        viewModel.toggleFavorite(station.url_resolved, station.name, station.favicon, station.tags)
                                    },
                                    onLoadMore = {
                                        viewModel.fetchStations(reset = false)
                                    }
                                )
                            }
                            AppTab.FAVORITES -> {
                                FavoritesTab(
                                    favorites = favorites,
                                    activeUrl = currentUrl,
                                    playbackState = playbackState,
                                    onStationSelect = { favStation ->
                                        viewModel.selectStation(favStation.urlResolved, favStation.name, favStation.favicon)
                                    },
                                    onToggleFavorite = { favStation ->
                                        viewModel.toggleFavorite(favStation.urlResolved, favStation.name, favStation.favicon, favStation.tags)
                                    }
                                )
                            }
                            AppTab.SETTINGS -> {
                                SettingsTab(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
