package com.example

import android.Manifest
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.data.ApiStation
import com.example.data.FavoriteStation
import com.example.player.PlaybackState
import com.example.ui.AppTab
import com.example.ui.RadioViewModel
import com.example.ui.StationAdapter
import com.example.ui.UIStationItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: RadioViewModel by viewModels {
        RadioViewModel.Factory(application)
    }

    // View declarations
    private lateinit var loadingScreen: View
    private lateinit var mainAppContent: View
    private lateinit var loadingProgressMeter: View
    private lateinit var loadingStatusText: TextView

    private lateinit var searchEditText: EditText
    private lateinit var clearSearchBtn: View

    private lateinit var recyclerViewRadio: RecyclerView
    private lateinit var recyclerViewFavorites: RecyclerView

    private lateinit var emptyRadioLayout: View
    private lateinit var emptyFavoritesLayout: View

    private lateinit var radioPaginationLoader: ProgressBar

    private lateinit var radioScrollToTopBtn: FloatingActionButton
    private lateinit var favoritesScrollToTopBtn: FloatingActionButton

    private lateinit var radioTab: View
    private lateinit var favoritesTab: View
    private lateinit var aboutTab: View

    private lateinit var appHeaderLayout: View
    private lateinit var searchRowContainer: View

    private lateinit var playToggleBtn: ImageButton
    private lateinit var playingStationTitle: TextView
    private lateinit var playerStatusIcon: ImageView
    private lateinit var playerStatusText: TextView
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var radioAdapter: StationAdapter
    private lateinit var favoritesAdapter: StationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request notifications permission for Android 13+
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { _ -> }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Bind raw layout views
        initViews()

        // Configure lists / recyclerviews
        setupRecyclerViews()

        // Bind interactive events and listeners
        setupListeners()

        // Start collecting StateFlow events
        observeViewModel()
    }

    private fun initViews() {
        loadingScreen = findViewById(R.id.loadingScreen)
        mainAppContent = findViewById(R.id.mainAppContent)
        loadingProgressMeter = findViewById(R.id.loadingProgressMeter)
        loadingStatusText = findViewById(R.id.loadingStatusText)

        searchEditText = findViewById(R.id.searchEditText)
        clearSearchBtn = findViewById(R.id.clearSearchBtn)

        recyclerViewRadio = findViewById(R.id.radioRecyclerView)
        recyclerViewFavorites = findViewById(R.id.favoritesRecyclerView)

        emptyRadioLayout = findViewById(R.id.emptyRadioLayout)
        emptyFavoritesLayout = findViewById(R.id.emptyFavoritesLayout)

        radioPaginationLoader = findViewById(R.id.radioPaginationLoader)

        radioScrollToTopBtn = findViewById(R.id.radioScrollToTopBtn)
        favoritesScrollToTopBtn = findViewById(R.id.favoritesScrollToTopBtn)

        radioTab = findViewById(R.id.radioTab)
        favoritesTab = findViewById(R.id.favoritesTab)
        aboutTab = findViewById(R.id.aboutTab)

        appHeaderLayout = findViewById(R.id.appHeaderLayout)
        searchRowContainer = findViewById(R.id.searchRowContainer)

        playToggleBtn = findViewById(R.id.playToggleBtn)
        playingStationTitle = findViewById(R.id.playingStationTitle)
        playerStatusIcon = findViewById(R.id.playerStatusIcon)
        playerStatusText = findViewById(R.id.playerStatusText)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupRecyclerViews() {
        // Setup Radio list
        radioAdapter = StationAdapter(
            onSelect = { item ->
                viewModel.selectStation(item.urlResolved, item.name, item.favicon)
            },
            onToggleFavorite = { item ->
                viewModel.toggleFavorite(item.urlResolved, item.name, item.favicon, item.tags)
            }
        )
        recyclerViewRadio.layoutManager = LinearLayoutManager(this)
        recyclerViewRadio.adapter = radioAdapter

        // Setup Favorites list
        favoritesAdapter = StationAdapter(
            onSelect = { item ->
                viewModel.selectStation(item.urlResolved, item.name, item.favicon)
            },
            onToggleFavorite = { item ->
                viewModel.toggleFavorite(item.urlResolved, item.name, item.favicon, item.tags)
            }
        )
        recyclerViewFavorites.layoutManager = LinearLayoutManager(this)
        recyclerViewFavorites.adapter = favoritesAdapter
    }

    private fun setupListeners() {
        // Clean Search listener
        clearSearchBtn.setOnClickListener {
            searchEditText.setText("")
            viewModel.updateSearchQuery("")
            // Hide keyboard on search clear
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(searchEditText.windowToken, 0)
            searchEditText.clearFocus()
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                if (query != viewModel.searchQuery.value) {
                    viewModel.updateSearchQuery(query)
                }
                clearSearchBtn.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Scroll to top clicks
        radioScrollToTopBtn.setOnClickListener {
            recyclerViewRadio.smoothScrollToPosition(0)
        }
        favoritesScrollToTopBtn.setOnClickListener {
            recyclerViewFavorites.smoothScrollToPosition(0)
        }

        // Infinite scroll Pagination loading
        recyclerViewRadio.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val manager = recyclerView.layoutManager as LinearLayoutManager
                val totalCount = manager.itemCount
                val lastVisible = manager.findLastVisibleItemPosition()

                if (totalCount > 0 && lastVisible >= (totalCount - 10)) {
                    if (!viewModel.isLoading.value && viewModel.hasMore.value) {
                        viewModel.fetchStations(reset = false)
                    }
                }

                // Scroll to top button visibility
                val firstVisible = manager.findFirstVisibleItemPosition()
                radioScrollToTopBtn.visibility = if (firstVisible > 4) View.VISIBLE else View.GONE
            }
        })

        recyclerViewFavorites.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val manager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisible = manager.findFirstVisibleItemPosition()
                favoritesScrollToTopBtn.visibility = if (firstVisible > 4) View.VISIBLE else View.GONE
            }
        })

        // Play Pause toggle
        playToggleBtn.setOnClickListener {
            viewModel.togglePlay()
        }

        // Tab selection listener
        bottomNavigation.setOnItemSelectedListener { item ->
            val targetTab = when (item.itemId) {
                R.id.navigation_favorites -> AppTab.FAVORITES
                R.id.navigation_about -> AppTab.ABOUT
                else -> AppTab.RADIO
            }
            if (viewModel.activeTab.value != targetTab) {
                viewModel.selectTab(targetTab)
            }
            true
        }

        // Clean Back dispatcher integration
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.activeTab.value != AppTab.RADIO) {
                    viewModel.selectTab(AppTab.RADIO)
                } else if (viewModel.searchQuery.value.isNotEmpty()) {
                    viewModel.updateSearchQuery("")
                    searchEditText.setText("")
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Connection states
                launch {
                    viewModel.isConnecting.collect { isConnecting ->
                        if (isConnecting) {
                            loadingScreen.visibility = View.VISIBLE
                            mainAppContent.visibility = View.GONE
                        } else {
                            loadingScreen.visibility = View.GONE
                            mainAppContent.visibility = View.VISIBLE
                        }
                    }
                }

                launch {
                    viewModel.connectionProgress.collect { progress ->
                        val floatProgress = progress / 100f
                        loadingProgressMeter.post {
                            val parent = loadingProgressMeter.parent as? View
                            val parentWidth = parent?.width ?: 0
                            val params = loadingProgressMeter.layoutParams as? FrameLayout.LayoutParams
                            if (params != null && parentWidth > 0) {
                                params.width = (parentWidth * floatProgress).toInt()
                                loadingProgressMeter.layoutParams = params
                            }
                        }
                    }
                }

                launch {
                    viewModel.connectionStatusText.collect { msg ->
                        loadingStatusText.text = msg
                    }
                }

                // 2. Active Tab switched
                launch {
                    viewModel.activeTab.collect { tab ->
                        // Switch content pages
                        radioTab.visibility = if (tab == AppTab.RADIO) View.VISIBLE else View.GONE
                        favoritesTab.visibility = if (tab == AppTab.FAVORITES) View.VISIBLE else View.GONE
                        aboutTab.visibility = if (tab == AppTab.ABOUT) View.VISIBLE else View.GONE

                        // Control Search header
                        searchRowContainer.visibility = if (tab == AppTab.RADIO) View.VISIBLE else View.GONE

                        // Sync Bottom navigation View active item index without causing loops
                        val expectedMenuId = when (tab) {
                            AppTab.RADIO -> R.id.navigation_radio
                            AppTab.FAVORITES -> R.id.navigation_favorites
                            AppTab.ABOUT -> R.id.navigation_about
                        }
                        if (bottomNavigation.selectedItemId != expectedMenuId) {
                            bottomNavigation.selectedItemId = expectedMenuId
                        }
                    }
                }

                // 3. Search query updates
                launch {
                    viewModel.searchQuery.collect { query ->
                        if (searchEditText.text.toString() != query) {
                            searchEditText.setText(query)
                        }
                        clearSearchBtn.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                }

                // 4. Combined mapping for stations with fast favorite lookups
                launch {
                    combine(viewModel.stations, viewModel.favorites) { stations, favorites ->
                        val favUrls = favorites.map { it.urlResolved }.toSet()
                        stations.map { station ->
                            UIStationItem(
                                urlResolved = station.url_resolved,
                                name = station.name,
                                tags = station.tags,
                                favicon = station.favicon,
                                isFavorite = favUrls.contains(station.url_resolved),
                                originalApi = station
                            )
                        }
                    }.collect { list ->
                        radioAdapter.submitList(list)
                        emptyRadioLayout.visibility = if (list.isEmpty() && !viewModel.isLoading.value) View.VISIBLE else View.GONE
                    }
                }

                // 5. Mapping Favorites
                launch {
                    viewModel.favorites.collect { favorites ->
                        val favList = favorites.map { fav ->
                            UIStationItem(
                                urlResolved = fav.urlResolved,
                                name = fav.name,
                                tags = fav.tags,
                                favicon = fav.favicon,
                                isFavorite = true,
                                originalFav = fav
                            )
                        }
                        favoritesAdapter.submitList(favList)
                        emptyFavoritesLayout.visibility = if (favList.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                // 6. Loading more spinner
                launch {
                    viewModel.isLoading.collect { loading ->
                        radioPaginationLoader.visibility = if (loading && radioAdapter.itemCount > 0) View.VISIBLE else View.GONE
                        // Update empty radio result bounds
                        if (!loading && radioAdapter.itemCount == 0) {
                            emptyRadioLayout.visibility = View.VISIBLE
                        }
                    }
                }

                // 7. Player core management bindings
                launch {
                    viewModel.playerManager.currentUrl.collect { url ->
                        radioAdapter.setActiveUrl(url)
                        favoritesAdapter.setActiveUrl(url)
                    }
                }

                launch {
                    viewModel.playerManager.currentName.collect { name ->
                        playingStationTitle.text = name ?: "Выберите станцию"
                        playingStationTitle.isSelected = true // triggers marquee scroll
                    }
                }

                launch {
                    viewModel.playerManager.playbackState.collect { playState ->
                        // 1. Play Button toggle src
                        val controlIcon = if (playState == PlaybackState.PLAYING) R.drawable.ic_pause else R.drawable.ic_play
                        playToggleBtn.setImageResource(controlIcon)

                        // 2. Status icon and text styling
                        val statusColor: Int
                        val statusText: String
                        val statusIcon: Int

                        when (playState) {
                            PlaybackState.PLAYING -> {
                                statusColor = Color.parseColor("#4CAF50")
                                statusText = "Играет"
                                statusIcon = R.drawable.ic_volume_up
                            }
                            PlaybackState.BUFFERING -> {
                                statusColor = ContextCompat.getColor(this@MainActivity, R.color.light_pink)
                                statusText = "Буфер..."
                                statusIcon = R.drawable.ic_sync
                            }
                            PlaybackState.PAUSED -> {
                                statusColor = Color.parseColor("#FFA000")
                                statusText = "Пауза"
                                statusIcon = R.drawable.ic_pause_circle
                            }
                            PlaybackState.ERROR -> {
                                statusColor = Color.parseColor("#F44336")
                                statusText = "Ошибка"
                                statusIcon = R.drawable.ic_error
                            }
                            PlaybackState.IDLE -> {
                                statusColor = ContextCompat.getColor(this@MainActivity, R.color.text_secondary)
                                statusText = "Ожидание"
                                statusIcon = R.drawable.ic_circle
                            }
                        }

                        // Apply visual tint
                        playerStatusIcon.setImageResource(statusIcon)
                        playerStatusIcon.imageTintList = ColorStateList.valueOf(statusColor)
                        playerStatusText.text = statusText
                        playerStatusText.setTextColor(statusColor)

                        // If buffering, spin the sync icon to display loading activity
                        if (playState == PlaybackState.BUFFERING) {
                            val rotate = RotateAnimation(
                                0f, 360f,
                                Animation.RELATIVE_TO_SELF, 0.5f,
                                Animation.RELATIVE_TO_SELF, 0.5f
                            ).apply {
                                duration = 1200
                                repeatCount = Animation.INFINITE
                                interpolator = LinearInterpolator()
                            }
                            playerStatusIcon.startAnimation(rotate)
                        } else {
                            playerStatusIcon.clearAnimation()
                        }
                    }
                }
            }
        }
    }
}
