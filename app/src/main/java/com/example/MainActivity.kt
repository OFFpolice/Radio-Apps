package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
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
                searchQuery = searchQuery,
                onSearchChange = { viewModel.updateSearchQuery(it) },
                isSearchVisible = activeTab == AppTab.RADIO
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
                    onTabSelect = { viewModel.selectTab(it) }
                )
            }
        },
        modifier = Modifier
            .fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (activeTab) {
                AppTab.RADIO -> {
                    RadioTab(
                        stations = stations,
                        favorites = favorites,
                        activeUrl = currentUrl,
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
