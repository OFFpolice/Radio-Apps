package com.offpolice.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offpolice.data.ApiStation
import com.offpolice.data.FavoriteStation
import com.offpolice.data.RadioRepository
import com.offpolice.player.PlaybackState
import com.offpolice.player.RadioPlayerManager
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    RADIO,
    FAVORITES,
    ABOUT
}

@OptIn(FlowPreview::class)
class RadioViewModel(private val application: Application) : AndroidViewModel(application) {

    private val repository = RadioRepository(application)
    val playerManager = RadioPlayerManager(application)

    private val sharedPrefs = application.getSharedPreferences("webradio_prefs", Context.MODE_PRIVATE)

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

    private val _connectionProgress = MutableStateFlow(10)
    val connectionProgress: StateFlow<Int> = _connectionProgress.asStateFlow()

    private val _connectionStatusText = MutableStateFlow("Инициализация...")
    val connectionStatusText: StateFlow<String> = _connectionStatusText.asStateFlow()

    private val _isConnecting = MutableStateFlow(true)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private var offset = 0
    private val LIMIT = 100
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            _isConnecting.value = true
            _connectionProgress.value = 15
            _connectionStatusText.value = "Загрузка ресурсов..."
            delay(200)

            _connectionProgress.value = 45
            _connectionStatusText.value = "Подключение к серверу..."
            delay(200)

            repository.resolveActiveServer()
            _connectionProgress.value = 80
            _connectionStatusText.value = "Получение списка станций..."

            fetchStations(reset = true, query = "")

            _connectionProgress.value = 100
            _connectionStatusText.value = "Готово!"
            delay(300)
            _isConnecting.value = false

            val lastUrl = sharedPrefs.getString("last_url", null)
            val lastName = sharedPrefs.getString("last_name", null)
            val lastFavicon = sharedPrefs.getString("last_favicon", null)
            if (lastUrl != null && lastName != null) {
                playerManager.play(lastUrl, lastName, lastFavicon)
                playerManager.pause()
            } else if (stations.value.isNotEmpty()) {
                val first = stations.value.first()
                playerManager.play(first.url_resolved, first.name, first.favicon)
                playerManager.pause()
            }
        }

        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .collect { query ->
                    if (!isConnecting.value) {
                        fetchStations(reset = true, query = query)
                    }
                }
        }
    }

    fun selectTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun fetchStations(reset: Boolean, query: String = searchQuery.value) {
        if (_isLoading.value) return
        if (!reset && !_hasMore.value) return

        _isLoading.value = true
        _apiError.value = null

        if (reset) {
            offset = 0
            _hasMore.value = true
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                val processed = withContext(Dispatchers.Default) {
                    val results = repository.searchStations(query, LIMIT, offset)
                    val validResults = results.filter { it.url_resolved.isNotBlank() && it.name.isNotBlank() }

                    if (reset) {
                        Pair(validResults.distinctBy { it.url_resolved }, validResults.size)
                    } else {
                        val currentList = _stations.value
                        val existingUrls = currentList.map { it.url_resolved }.toSet()
                        val newUniqueResults = validResults.filter { !existingUrls.contains(it.url_resolved) }
                        Pair(currentList + newUniqueResults, validResults.size)
                    }
                }

                _stations.value = processed.first
                offset += processed.second
                _hasMore.value = processed.second == LIMIT
            } catch (e: Exception) {
                e.printStackTrace()
                _apiError.value = "Ошибка сети при загрузке станций"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectStation(url: String, name: String, favicon: String?) {
        playerManager.play(url, name, favicon)

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

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }

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
