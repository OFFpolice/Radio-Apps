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
import kotlinx.coroutines.flow.map
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

    val favoriteUrls: StateFlow<Set<String>> = repository.getFavoritesFlow()
        .map { list -> list.mapTo(HashSet(list.size)) { it.urlResolved } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    private val _apiError = MutableStateFlow<String?>(null)
    val apiError: StateFlow<String?> = _apiError.asStateFlow()

    private val _connectionProgress = MutableStateFlow(15) // 0-100%
    val connectionProgress: StateFlow<Int> = _connectionProgress.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.INITIALIZING)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _isConnecting = MutableStateFlow(true)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private var offset = 0
    private val LIMIT = 100
    private var searchJob: Job? = null
    private var isFetchingPage = false
    private val loadedStationUrls = HashSet<String>()
    private var lastFetchedQuery: String? = null

    init {
        playerManager.onNextStation = {
            playNextStation()
        }
        playerManager.onPreviousStation = {
            playPreviousStation()
        }

        // 1. Startup loading flow: show loading screen, load cache instantly, then fetch fresh data
        viewModelScope.launch {
            _isConnecting.value = true
            _connectionProgress.value = 20
            _connectionState.value = ConnectionState.INITIALIZING

            // Instantly load cached catalog from Room so UI is never blank
            val cached = repository.getCachedStations()
            if (cached.isNotEmpty()) {
                _stations.value = cached
                for (s in cached) {
                    val key = s.stationuuid?.ifBlank { null } ?: s.url_resolved
                    loadedStationUrls.add(key)
                }
            }

            _connectionProgress.value = 55
            _connectionState.value = ConnectionState.CONNECTING_SERVER

            repository.resolveActiveServer()

            _connectionProgress.value = 80
            _connectionState.value = ConnectionState.GETTING_STATIONS

            fetchStations(reset = true, query = "")

            _connectionProgress.value = 100
            _connectionState.value = ConnectionState.READY
            delay(350)
            _isConnecting.value = false

            // Restore last played station metadata into UI only if previously chosen
            val lastUrl = sharedPrefs.getString("last_url", null)
            val lastName = sharedPrefs.getString("last_name", null)
            val lastFavicon = sharedPrefs.getString("last_favicon", null)
            if (lastUrl != null && lastName != null) {
                playerManager.initStationWithoutPlayback(lastUrl, lastName, lastFavicon)
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
            // If already completed this exact search query, don't refetch needlessly
            if (query == lastFetchedQuery && _stations.value.isNotEmpty() && !_isLoading.value) {
                return
            }
            searchJob?.cancel()
            _isLoading.value = true
            _apiError.value = null
            offset = 0
            _hasMore.value = true
            // Note: do not clear _stations here to prevent cache/screen flickering until fresh data arrives
        } else {
            if (_isLoading.value || isFetchingPage || !_hasMore.value) return
            _isLoading.value = true
            _apiError.value = null
        }

        searchJob = viewModelScope.launch {
            if (!reset) isFetchingPage = true
            try {
                val currentOffset = offset
                val validResults = withContext(Dispatchers.IO) {
                    val results = repository.searchStations(query, LIMIT, currentOffset)
                    results.filter { it.url_resolved.isNotBlank() && it.name.isNotBlank() }
                }

                // Deduplicate safely on the Main thread
                if (reset) {
                    loadedStationUrls.clear()
                }
                val newUniqueResults = ArrayList<ApiStation>(validResults.size)
                for (item in validResults) {
                    val key = item.stationuuid?.ifBlank { null } ?: item.url_resolved
                    if (loadedStationUrls.add(key)) {
                        newUniqueResults.add(item)
                    }
                }

                _stations.value = if (reset) newUniqueResults else (_stations.value + newUniqueResults)
                offset += validResults.size
                _hasMore.value = validResults.size == LIMIT
                lastFetchedQuery = query

                // Cache top stations for fast offline startup
                if (reset && query.isBlank() && _stations.value.isNotEmpty()) {
                    repository.saveCachedStations(_stations.value.take(50))
                }
            } catch (e: CancellationException) {
                // Ignore search cancellation
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
