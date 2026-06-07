package com.example

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.databinding.ActivityMainBinding
import com.example.ui.AppLanguageSetting
import com.example.ui.AppTab
import com.example.ui.AppThemeSetting
import com.example.ui.ConnectionState
import com.example.ui.DisplayStation
import com.example.ui.Loc
import com.example.ui.RadioViewModel
import com.example.ui.StationAdapter
import com.example.player.PlaybackState
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    
    private val viewModel: RadioViewModel by viewModels {
        RadioViewModel.Factory(application)
    }

    private lateinit var radioAdapter: StationAdapter
    private lateinit var favAdapter: StationAdapter

    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val attributionContext = newBase.createAttributionContext("webradio")
            super.attachBaseContext(attributionContext)
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle edge to edge system bar insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContentLayout) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            
            // AppHeader top margin should be status bars height
            binding.appHeader.setPadding(
                binding.appHeader.paddingLeft,
                statusBars.top,
                binding.appHeader.paddingRight,
                binding.appHeader.paddingBottom
            )
            
            // Bottom navigation should have bottom padding matches navigation bars
            binding.bottomNavigation.setPadding(
                binding.bottomNavigation.paddingLeft,
                binding.bottomNavigation.paddingTop,
                binding.bottomNavigation.paddingRight,
                navigationBars.bottom
            )
            insets
        }

        setupRecyclerViews()
        setupBottomNavigation()
        setupSearch()
        setupSettings()
        setupPlayerBar()
        setupScrollFABs()
        setupBackPress()

        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // Radio list
        radioAdapter = StationAdapter(
            onSelect = { displayStation ->
                viewModel.selectStation(displayStation.urlResolved, displayStation.name, displayStation.favicon)
            },
            onToggleFav = { displayStation ->
                viewModel.toggleFavorite(
                    displayStation.urlResolved, 
                    displayStation.name, 
                    displayStation.favicon, 
                    displayStation.tags
                )
            }
        )
        binding.radioRecycler.layoutManager = LinearLayoutManager(this)
        binding.radioRecycler.adapter = radioAdapter

        // Scroll listener for pagination load-more + scroll to top FAB visibility
        binding.radioRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                if (totalItemCount > 0 && lastVisibleItemPosition >= totalItemCount - 8) {
                    val isLoading = viewModel.isLoading.value
                    val hasMore = viewModel.hasMore.value
                    val isConnecting = viewModel.isConnecting.value
                    if (!isLoading && hasMore && !isConnecting) {
                        viewModel.fetchStations(reset = false)
                    }
                }

                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                binding.scrollToTopRadio.visibility = if (firstVisibleItemPosition > 4) View.VISIBLE else View.GONE
            }
        })

        // Favorites list
        favAdapter = StationAdapter(
            onSelect = { displayStation ->
                viewModel.selectStation(displayStation.urlResolved, displayStation.name, displayStation.favicon)
            },
            onToggleFav = { displayStation ->
                viewModel.toggleFavorite(
                    displayStation.urlResolved, 
                    displayStation.name, 
                    displayStation.favicon, 
                    displayStation.tags
                )
            }
        )
        binding.favRecycler.layoutManager = LinearLayoutManager(this)
        binding.favRecycler.adapter = favAdapter

        binding.favRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                binding.scrollToTopFav.visibility = if (firstVisibleItemPosition > 4) View.VISIBLE else View.GONE
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_radio -> {
                    viewModel.selectTab(AppTab.RADIO)
                    true
                }
                R.id.nav_favorites -> {
                    viewModel.selectTab(AppTab.FAVORITES)
                    true
                }
                R.id.nav_settings -> {
                    viewModel.selectTab(AppTab.SETTINGS)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupSearch() {
        // Query watcher
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                viewModel.updateSearchQuery(query)
                binding.searchClearBtn.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Action search close keyboard
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
                binding.searchInput.clearFocus()
                true
            } else {
                false
            }
        }

        binding.searchClearBtn.setOnClickListener {
            binding.searchInput.setText("")
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
            binding.searchInput.clearFocus()
        }
    }

    private fun setupSettings() {
        // Language Option clicks
        binding.langRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val lang = when (checkedId) {
                R.id.lang_auto -> AppLanguageSetting.AUTO
                R.id.lang_en -> AppLanguageSetting.EN
                R.id.lang_ru -> AppLanguageSetting.RU
                R.id.lang_uk -> AppLanguageSetting.UK
                else -> AppLanguageSetting.AUTO
            }
            if (viewModel.languageSetting.value != lang) {
                viewModel.setLanguageSetting(lang)
            }
        }

        // Theme Option clicks
        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.theme_system -> AppThemeSetting.SYSTEM
                R.id.theme_light -> AppThemeSetting.LIGHT
                R.id.theme_dark -> AppThemeSetting.DARK
                else -> AppThemeSetting.SYSTEM
            }
            if (viewModel.themeSetting.value != theme) {
                viewModel.setThemeSetting(theme)
            }
        }

        // Social Links clicks
        binding.socialTelegram.setOnClickListener {
            openUrl("https://t.me/OFFpolice")
        }
        binding.socialX.setOnClickListener {
            openUrl("https://x.com/OFFpolice2077")
        }
        binding.socialInstagram.setOnClickListener {
            openUrl("https://www.instagram.com/offpolice2077")
        }
        binding.socialBot.setOnClickListener {
            openUrl("https://t.me/Web_radio_bot/app")
        }
        binding.developerApiBtn.setOnClickListener {
            openUrl("https://api.radio-browser.info/")
        }
    }

    private fun setupPlayerBar() {
        binding.playToggleBtn.setOnClickListener {
            viewModel.togglePlay()
        }
    }

    private fun setupScrollFABs() {
        binding.scrollToTopRadio.setOnClickListener {
            binding.radioRecycler.smoothScrollToPosition(0)
        }
        binding.scrollToTopFav.setOnClickListener {
            binding.favRecycler.smoothScrollToPosition(0)
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val query = viewModel.searchQuery.value
                val tab = viewModel.activeTab.value
                if (tab != AppTab.RADIO) {
                    viewModel.selectTab(AppTab.RADIO)
                } else if (query.isNotEmpty()) {
                    binding.searchInput.setText("")
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
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                
                // 1. Observe Language changes to dynamically localise all texts
                launch {
                    viewModel.languageSetting.collect { lang ->
                        updateLocalizedTexts(lang)
                        // Reload lists to bind translated strings/placeholders correctly
                        radioAdapter.notifyDataSetChanged()
                        favAdapter.notifyDataSetChanged()
                    }
                }

                // 2. Observe Theme Setting change
                launch {
                    viewModel.themeSetting.collect { theme ->
                        val targetMode = when (theme) {
                            AppThemeSetting.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            AppThemeSetting.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                            AppThemeSetting.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                        }
                        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
                            AppCompatDelegate.setDefaultNightMode(targetMode)
                        }

                        // Sync standard preferences Radio buttons on UI
                        val checkedId = when (theme) {
                            AppThemeSetting.SYSTEM -> R.id.theme_system
                            AppThemeSetting.LIGHT -> R.id.theme_light
                            AppThemeSetting.DARK -> R.id.theme_dark
                        }
                        if (binding.themeRadioGroup.checkedRadioButtonId != checkedId) {
                            binding.themeRadioGroup.check(checkedId)
                        }
                    }
                }

                // 3. Observe Language setting in radio check (to sync preference UI back)
                launch {
                    viewModel.languageSetting.collect { lang ->
                        val checkedId = when (lang) {
                            AppLanguageSetting.AUTO -> R.id.lang_auto
                            AppLanguageSetting.EN -> R.id.lang_en
                            AppLanguageSetting.RU -> R.id.lang_ru
                            AppLanguageSetting.UK -> R.id.lang_uk
                        }
                        if (binding.langRadioGroup.checkedRadioButtonId != checkedId) {
                            binding.langRadioGroup.check(checkedId)
                        }
                    }
                }

                // 4. Observe Connection Fullscreen overlay states
                launch {
                    combine(
                        viewModel.isConnecting,
                        viewModel.connectionProgress,
                        viewModel.connectionState
                    ) { isConnecting, progress, state ->
                        Triple(isConnecting, progress, state)
                    }.collect { (isConnecting, progress, state) ->
                        if (isConnecting) {
                            binding.fullscreenLoadingLayout.visibility = View.VISIBLE
                            
                            // Map state enum to local text
                            val lang = viewModel.languageSetting.value
                            val stateString = when (state) {
                                ConnectionState.INITIALIZING -> Loc.get("state_init", lang)
                                ConnectionState.LOADING_RESOURCES -> Loc.get("state_resources", lang)
                                ConnectionState.CONNECTING_SERVER -> Loc.get("state_connecting", lang)
                                ConnectionState.GETTING_STATIONS -> Loc.get("state_stations", lang)
                                ConnectionState.READY -> Loc.get("state_ready", lang)
                            }
                            binding.loadingStatusText.text = stateString
                            
                            // Width layout update
                            val progressRatio = progress / 100f
                            val lp = binding.loadingProgressBar.layoutParams
                            val maxWidthPx = binding.loadingProgressBar.parent?.let { (it as View).width } ?: 200
                            lp.width = (maxWidthPx * progressRatio).toInt()
                            binding.loadingProgressBar.layoutParams = lp
                        } else {
                            binding.fullscreenLoadingLayout.visibility = View.GONE
                        }
                    }
                }

                // 5. Observe Active Tab
                launch {
                    viewModel.activeTab.collect { tab ->
                        binding.tabRadioContainer.visibility = if (tab == AppTab.RADIO) View.VISIBLE else View.GONE
                        binding.tabFavoritesContainer.visibility = if (tab == AppTab.FAVORITES) View.VISIBLE else View.GONE
                        binding.tabSettingsContainer.visibility = if (tab == AppTab.SETTINGS) View.VISIBLE else View.GONE
                        
                        binding.appHeader.visibility = View.VISIBLE // Header is always visible
                        binding.searchBarContainer.visibility = if (tab == AppTab.RADIO) View.VISIBLE else View.GONE

                        val navId = when (tab) {
                            AppTab.RADIO -> R.id.nav_radio
                            AppTab.FAVORITES -> R.id.nav_favorites
                            AppTab.SETTINGS -> R.id.nav_settings
                        }
                        if (binding.bottomNavigation.selectedItemId != navId) {
                            binding.bottomNavigation.selectedItemId = navId
                        }
                    }
                }

                // 6. Observe Search Query in text edit matching ViewModel
                launch {
                    viewModel.searchQuery.collect { query ->
                        if (binding.searchInput.text.toString() != query) {
                            binding.searchInput.setText(query)
                        }
                    }
                }

                // 7. Observe unified reactive Radio Stations list
                launch {
                    combine(
                        viewModel.stations,
                        viewModel.favorites,
                        viewModel.playerManager.currentUrl
                    ) { stations, favorites, activeUrl ->
                        val favoritesUrls = favorites.map { it.urlResolved }.toSet()
                        stations.map { s ->
                            DisplayStation(
                                name = s.name,
                                favicon = s.favicon,
                                tags = s.tags,
                                urlResolved = s.url_resolved,
                                isFavorite = favoritesUrls.contains(s.url_resolved),
                                isActive = s.url_resolved == activeUrl,
                                origin = s
                            )
                        }
                    }.collect { displayStations ->
                        radioAdapter.submitList(displayStations)
                        
                        // Handle list Empty Placeholder visibility
                        val isLoading = viewModel.isLoading.value
                        val isEmpty = displayStations.isEmpty()
                        binding.radioEmptyPlaceholder.visibility = if (isEmpty && !isLoading) View.VISIBLE else View.GONE
                    }
                }

                // 8. Observe unified reactive Favorites list
                launch {
                    combine(
                        viewModel.favorites,
                        viewModel.playerManager.currentUrl
                    ) { favorites, activeUrl ->
                        favorites.map { s ->
                            DisplayStation(
                                name = s.name,
                                favicon = s.favicon,
                                tags = s.tags,
                                urlResolved = s.urlResolved,
                                isFavorite = true,
                                isActive = s.urlResolved == activeUrl,
                                origin = s
                            )
                        }
                    }.collect { displayFavorites ->
                        favAdapter.submitList(displayFavorites)
                        binding.favEmptyPlaceholder.visibility = if (displayFavorites.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                // 9. Observe Pagination loader state
                launch {
                    viewModel.isLoading.collect { loading ->
                        // Show bottom list loader if not empty list
                        val holdsItems = radioAdapter.itemCount > 0
                        binding.radioPaginationLoader.visibility = if (loading && holdsItems) View.VISIBLE else View.GONE
                        
                        // Hide main list empty placeholder when loading
                        if (loading) {
                            binding.radioEmptyPlaceholder.visibility = View.GONE
                        }
                    }
                }

                // 10. Observe Player Manager Core details (Url, Name, playbackState)
                launch {
                    combine(
                        viewModel.playerManager.currentName,
                        viewModel.playerManager.playbackState
                    ) { name, state ->
                        Pair(name, state)
                    }.collect { (currentName, playbackState) ->
                        val lang = viewModel.languageSetting.value
                        
                        // Title
                        binding.playingStationTitle.text = currentName ?: Loc.get("select_station", lang)
                        binding.playingStationTitle.isSelected = true // Trigger marquee scrolling

                        // Play pause icon change
                        val actionIcon = when (playbackState) {
                            PlaybackState.PLAYING -> R.drawable.ic_pause
                            else -> R.drawable.ic_play
                        }
                        binding.playToggleIcon.setImageResource(actionIcon)

                        // Playback status dynamic states
                        val statusText = when (playbackState) {
                            PlaybackState.PLAYING -> Loc.get("playing", lang)
                            PlaybackState.BUFFERING -> Loc.get("buffering", lang)
                            PlaybackState.PAUSED -> Loc.get("paused", lang)
                            PlaybackState.ERROR -> Loc.get("error", lang)
                            PlaybackState.IDLE -> Loc.get("idle", lang)
                        }
                        binding.statusText.text = statusText

                        val statusColor = when (playbackState) {
                            PlaybackState.PLAYING -> 0xFF4CAF50.toInt()
                            PlaybackState.BUFFERING -> ContextCompat.getColor(this@MainActivity, R.color.light_pink)
                            PlaybackState.PAUSED -> 0xFFFFA000.toInt()
                            PlaybackState.ERROR -> 0xFFF44336.toInt()
                            PlaybackState.IDLE -> ContextCompat.getColor(this@MainActivity, R.color.text_secondary)
                        }
                        binding.statusText.setTextColor(statusColor)

                        // Status Icon change
                        val statusIcon = when (playbackState) {
                            PlaybackState.PLAYING -> R.drawable.ic_volume_up
                            PlaybackState.BUFFERING -> R.drawable.ic_sync
                            PlaybackState.PAUSED -> R.drawable.ic_pause
                            PlaybackState.ERROR -> R.drawable.ic_error
                            PlaybackState.IDLE -> R.drawable.ic_radio
                        }
                        binding.statusIndicatorIcon.setImageResource(statusIcon)
                        binding.statusIndicatorIcon.imageTintList = ColorStateList.valueOf(statusColor)

                        // Clear prior rotations
                        binding.statusIndicatorIcon.clearAnimation()

                        // If buffering, spin rotation animation infinitely
                        if (playbackState == PlaybackState.BUFFERING) {
                            val rotate = RotateAnimation(
                                0f, 360f,
                                Animation.RELATIVE_TO_SELF, 0.5f,
                                Animation.RELATIVE_TO_SELF, 0.5f
                            ).apply {
                                duration = 1200
                                repeatCount = Animation.INFINITE
                                interpolator = android.view.animation.LinearInterpolator()
                            }
                            binding.statusIndicatorIcon.startAnimation(rotate)
                        }
                    }
                }
            }
        }
    }

    private fun updateLocalizedTexts(lang: AppLanguageSetting) {
        // App header search box hint
        binding.searchInput.hint = Loc.get("search_placeholder", lang)

        // Tab captions uppercase
        binding.radioSectionTitle.text = Loc.get("all_stations", lang).uppercase()
        binding.favSectionTitle.text = Loc.get("tab_favorites", lang).uppercase()
        binding.settingsSectionTitle.text = Loc.get("tab_settings", lang).uppercase()

        // Placeholders empty lists
        binding.radioEmptyText.text = Loc.get("nothing_found", lang)
        binding.favEmptyText.text = Loc.get("empty_favorites", lang)

        // Radio buttons Lang options
        binding.settingLanguageTitle.text = Loc.get("setting_language", lang)
        binding.langAuto.text = Loc.get("language_auto", lang)
        binding.langEn.text = Loc.get("language_en", lang)
        binding.langRu.text = Loc.get("language_ru", lang)
        binding.langUk.text = Loc.get("language_uk", lang)

        // Radio buttons Theme options
        binding.settingThemeTitle.text = Loc.get("setting_theme", lang)
        binding.themeSystem.text = Loc.get("theme_system", lang)
        binding.themeLight.text = Loc.get("theme_light", lang)
        binding.themeDark.text = Loc.get("theme_dark", lang)

        // About desc & links
        binding.settingsAboutDesc.text = Loc.get("about_text", lang)
        binding.settingsSocialsHeading.text = Loc.get("socials_heading", lang).uppercase()
        binding.developerApiBtn.text = Loc.get("link_dev_api", lang)

        // Loading tagline text
        binding.loadingTagline.text = Loc.get("loading_tagline", lang)

        // Playback default state
        if (viewModel.playerManager.currentUrl.value == null) {
            binding.playingStationTitle.text = Loc.get("select_station", lang)
        }

        // Bottom Navigation menus item texts
        val menu = binding.bottomNavigation.menu
        menu.findItem(R.id.nav_radio).title = Loc.get("tab_radio", lang)
        menu.findItem(R.id.nav_favorites).title = Loc.get("tab_favorites", lang)
        menu.findItem(R.id.nav_settings).title = Loc.get("tab_settings", lang)
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startContext(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startContext(intent: Intent) {
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback always run browser
            startActivity(intent)
        }
    }
}
