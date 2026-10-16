package com.offpolice.webradiobot.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offpolice.webradiobot.data.ApiStation
import com.offpolice.webradiobot.data.FavoriteStation
import com.offpolice.webradiobot.data.RadioRepository
import com.offpolice.webradiobot.player.PlaybackState
import com.offpolice.webradiobot.player.RadioPlayerManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    RADIO,
    FAVORITES,
    SETTINGS
}

@OptIn(FlowPreview::class)
class RadioViewModel(private val application: Application) : AndroidViewModel(application) {

    private val repository = RadioRepository(application)
    val playerManager = RadioPlayerManager.getInstance(application)

    private val sharedPrefs = application.getSharedPreferences("webradio_prefs", Context.MODE_PRIVATE)

    private val _themeSetting = MutableStateFlow(
        AppThemeSetting.valueOf(
            sharedPrefs.getString("theme_setting", AppThemeSetting.SYSTEM.name) ?: AppThemeSetting.SYSTEM.name
        )
    )
    val themeSetting: StateFlow<AppThemeSetting> = _themeSetting.asStateFlow()

    private val _languageSetting = MutableStateFlow(
        AppLanguageSetting.valueOf(
            sharedPrefs.getString("language_setting", AppLanguageSetting.AUTO.name) ?: AppLanguageSetting.AUTO.name
        )
    )
    val languageSetting: StateFlow<AppLanguageSetting> = _languageSetting.asStateFlow()

    fun setThemeSetting(theme: AppThemeSetting) {
        _themeSetting.value = theme
        sharedPrefs.edit().putString("theme_setting", theme.name).apply()
    }

    fun setLanguageSetting(lang: AppLanguageSetting) {
        _languageSetting.value = lang
        sharedPrefs.edit().putString("language_setting", lang.name).apply()
    }

    private val _isLanguageScreenOpen = MutableStateFlow(false)
    val isLanguageScreenOpen: StateFlow<Boolean> = _isLanguageScreenOpen.asStateFlow()

    fun showLanguageScreen(show: Boolean) {
        _isLanguageScreenOpen.value = show
    }

    private val _activeTab = MutableStateFlow(AppTab.RADIO)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _stations = MutableStateFlow<List<ApiStation>>(emptyList())
    val stations: StateFlow<List<ApiStation>> = _stations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    val favorites: StateFlow<List<FavoriteStation>> = repository.getFavoritesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _apiError = MutableStateFlow<String?>(null)
    val apiError: StateFlow<String?> = _apiError.asStateFlow()

    private val _connectionProgress = MutableStateFlow(10) // 0-100%
    val connectionProgress: StateFlow<Int> = _connectionProgress.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.INITIALIZING)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private var offset = 0
    private val LIMIT = 100
    private var searchJob: Job? = null
    private var isFetchingPage = false
    private val loadedStationUrls = HashSet<String>()

    init {
        playerManager.onNextStation = {
            playNextStation()
        }
        playerManager.onPreviousStation = {
            playPreviousStation()
        }

        // 1. Instantly load cached stations from Room if available
        viewModelScope.launch {
            val cached = repository.getCachedStations()
            if (_stations.value.isEmpty() && cached.isNotEmpty()) {
                _stations.value = cached
                for (s in cached) {
                    val key = s.stationuuid?.ifBlank { null } ?: s.url_resolved
                    loadedStationUrls.add(key)
                }
            }
        }

        // 2. Debounce search input (350ms)
        viewModelScope.launch {
            _searchQuery
                .drop(1)
                .debounce(350L)
                .distinctUntilChanged()
                .collect { query ->
                    fetchStations(reset = true, query = query)
                }
        }

        // 3. Resolve active server and fetch fresh catalog in background without blocking UI
        viewModelScope.launch {
            repository.resolveActiveServer()
            fetchStations(reset = true, query = "")

            // Restore last played station into UI only if the user had previously selected one
            val lastUrl = sharedPrefs.getString("last_url", null)
            val lastName = sharedPrefs.getString("last_name", null)
            val lastFavicon = sharedPrefs.getString("last_favicon", null)
            if (lastUrl != null && lastName != null) {
                playerManager.initStationWithoutPlayback(lastUrl, lastName, lastFavicon)
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun submitSearch(query: String = _searchQuery.value) {
        _searchQuery.value = query
        fetchStations(reset = true, query = query)
    }

    fun clearSearch() {
        _searchQuery.value = ""
        fetchStations(reset = true, query = "")
    }

    fun fetchStations(reset: Boolean, query: String = searchQuery.value) {
        if (reset) {
            // Cancel previous in-flight search request immediately
            searchJob?.cancel()
            _isLoading.value = true
            _apiError.value = null
            offset = 0
            _hasMore.value = true
            loadedStationUrls.clear()
            _stations.value = emptyList()
        } else {
            // For next pages: do not fire if already fetching or reached end
            if (_isLoading.value || isFetchingPage || !_hasMore.value) return
            _isLoading.value = true
            _apiError.value = null
        }

        searchJob = viewModelScope.launch {
            if (!reset) isFetchingPage = true
            try {
                val currentOffset = offset
                val processed = withContext(Dispatchers.IO) {
                    val results = repository.searchStations(query, LIMIT, currentOffset)
                    val validResults = results.filter { it.url_resolved.isNotBlank() && it.name.isNotBlank() }

                    val newUniqueResults = ArrayList<ApiStation>(validResults.size)
                    for (item in validResults) {
                        val key = item.stationuuid?.ifBlank { null } ?: item.url_resolved
                        if (loadedStationUrls.add(key)) {
                            newUniqueResults.add(item)
                        }
                    }

                    if (reset) {
                        Pair(newUniqueResults, validResults.size)
                    } else {
                        Pair(_stations.value + newUniqueResults, validResults.size)
                    }
                }

                _stations.value = processed.first
                offset += processed.second
                _hasMore.value = processed.second == LIMIT

                // Cache first page of default catalog for fast offline startup
                if (reset && query.isBlank() && processed.first.isNotEmpty()) {
                    repository.saveCachedStations(processed.first.take(50))
                }
            } catch (e: CancellationException) {
                // Search cancelled by new query - do not treat as error
            } catch (e: Exception) {
                e.printStackTrace()
                _apiError.value = "Ошибка сети при загрузке станций"
            } finally {
                _isLoading.value = false
                isFetchingPage = false
            }
        }
    }

    fun selectStation(url: String, name: String, favicon: String?) {
        playerManager.play(url, name, favicon)

        // Save last played
        sharedPrefs.edit()
            .putString("last_url", url)
            .putString("last_name", name)
            .putString("last_favicon", favicon)
            .apply()
    }

    fun toggleFavorite(url: String, name: String, favicon: String?, tags: String?) {
        viewModelScope.launch {
            val isFav = favorites.value.any { it.urlResolved == url }
            if (isFav) {
                repository.removeFavoriteByUrl(url)
            } else {
                repository.addFavorite(url, name, tags, favicon)
            }
        }
    }

    fun togglePlay() {
        playerManager.togglePlay()
    }

    fun playNextStation() {
        val list = _stations.value
        if (list.isEmpty()) return
        val currentUrl = playerManager.currentUrl.value
        val currentIndex = list.indexOfFirst { it.url_resolved == currentUrl }
        val nextIndex = if (currentIndex in 0 until list.size - 1) currentIndex + 1 else 0
        val station = list[nextIndex]
        selectStation(station.url_resolved, station.name, station.favicon)
    }

    fun playPreviousStation() {
        val list = _stations.value
        if (list.isEmpty()) return
        val currentUrl = playerManager.currentUrl.value
        val currentIndex = list.indexOfFirst { it.url_resolved == currentUrl }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        val station = list[prevIndex]
        selectStation(station.url_resolved, station.name, station.favicon)
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.onNextStation = null
        playerManager.onPreviousStation = null
        // Player lifecycle is managed by RadioPlaybackService to support background playback
    }

    // Custom Factory for the ViewModel
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RadioViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RadioViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
