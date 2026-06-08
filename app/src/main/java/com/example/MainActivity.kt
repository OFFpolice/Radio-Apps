package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.databinding.ActivityMainBinding
import com.example.ui.AppLanguageSetting
import com.example.ui.AppTab
import com.example.ui.AppThemeSetting
import com.example.ui.ConnectionState
import com.example.ui.FavoritesAdapter
import com.example.ui.Loc
import com.example.ui.RadioViewModel
import com.example.ui.StationsAdapter
import com.example.player.PlaybackState
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: RadioViewModel

    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
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

        viewModel = ViewModelProvider(this, RadioViewModel.Factory(application))[RadioViewModel::class.java]

        // Set up edge-to-edge window padding insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Pad status bar region safely at header top
            binding.headerLayout.setPadding(
                binding.headerLayout.paddingLeft,
                systemBars.top,
                binding.headerLayout.paddingRight,
                binding.headerLayout.paddingBottom
            )
            
            // Pad gesture bar region safely at bottom navigation layout
            binding.bottomBarLayout.setPadding(
                binding.bottomBarLayout.paddingLeft,
                binding.bottomBarLayout.paddingTop,
                binding.bottomBarLayout.paddingRight,
                systemBars.bottom
            )
            insets
        }

        setupLists()
        setupListeners()
        observeViewModel()
    }

    private fun setupLists() {
        // 1. Radio Stations List Setup
        val stationsAdapter = StationsAdapter(
            onStationSelect = { station ->
                viewModel.selectStation(station.url_resolved, station.name, station.favicon)
            },
            onToggleFavorite = { station ->
                viewModel.toggleFavorite(station.url_resolved, station.name, station.favicon, station.tags)
            }
        )
        binding.rvStations.layoutManager = LinearLayoutManager(this)
        binding.rvStations.adapter = stationsAdapter

        // Stations List Flow Collector combining dataset, favorites status state, and current playing url
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.stations,
                    viewModel.favoriteUrls,
                    viewModel.playerManager.currentUrl
                ) { stationsList, favSet, activeUrl ->
                    Triple(stationsList, favSet, activeUrl)
                }.collect { (stationsList, favSet, activeUrl) ->
                    stationsAdapter.submitData(stationsList, favSet, activeUrl)
                    binding.radioEmptyState.visibility = 
                        if (stationsList.isEmpty() && !viewModel.isLoading.value) View.VISIBLE else View.GONE
                }
            }
        }

        // Stations Pagination Scroll Trigger for Smooth Load-More
        binding.rvStations.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lm = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = lm.itemCount
                val lastVisibleItem = lm.findLastVisibleItemPosition()
                
                // Show floating button when scrolled deep
                val firstVisibleItem = lm.findFirstVisibleItemPosition()
                binding.fabScrollUp.visibility = if (firstVisibleItem > 4) View.VISIBLE else View.GONE
                
                // Fetch next offsets automatically before hitting boundary
                if (!viewModel.isLoading.value && viewModel.hasMore.value && totalItemCount <= (lastVisibleItem + 2)) {
                    viewModel.fetchStations(reset = false)
                }
            }
        })

        // 2. Favorites List Setup
        val favoritesAdapter = FavoritesAdapter(
            onStationSelect = { favStation ->
                viewModel.selectStation(favStation.urlResolved, favStation.name, favStation.favicon)
            },
            onToggleFavorite = { favStation ->
                viewModel.toggleFavorite(favStation.urlResolved, favStation.name, favStation.favicon, favStation.tags)
            }
        )
        binding.rvFavorites.layoutManager = LinearLayoutManager(this)
        binding.rvFavorites.adapter = favoritesAdapter

        // Favorites Flow collector combining favorites and active players URL
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.favorites,
                    viewModel.playerManager.currentUrl
                ) { favList, activeUrl ->
                    Pair(favList, activeUrl)
                }.collect { (favList, activeUrl) ->
                    favoritesAdapter.submitData(favList, activeUrl)
                    binding.favoritesEmptyState.visibility = if (favList.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        // Favorites list scroll monitoring for scroll-to-top button
        binding.rvFavorites.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lm = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItem = lm.findFirstVisibleItemPosition()
                binding.fabFavScrollUp.visibility = if (firstVisibleItem > 4) View.VISIBLE else View.GONE
            }
        })
    }

    private fun setupListeners() {
        // Scroll-to-top FAB binders
        binding.fabScrollUp.setOnClickListener {
            binding.rvStations.smoothScrollToPosition(0)
        }
        binding.fabFavScrollUp.setOnClickListener {
            binding.rvFavorites.smoothScrollToPosition(0)
        }

        // Bottom Menu Tabs Actions
        binding.navItemRadio.setOnClickListener { viewModel.selectTab(AppTab.RADIO) }
        binding.navItemFavorites.setOnClickListener { viewModel.selectTab(AppTab.FAVORITES) }
        binding.navItemSettings.setOnClickListener { viewModel.selectTab(AppTab.SETTINGS) }

        // Play/Pause Action Toggle
        binding.playToggleBtn.setOnClickListener {
            viewModel.togglePlay()
        }

        // Search text listeners
        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""
                binding.btnClearSearch.visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE
                viewModel.updateSearchQuery(text)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.edtSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Dimiss keyboard on search click
                val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.edtSearch.windowToken, 0)
                true
            } else false
        }

        binding.btnClearSearch.setOnClickListener {
            binding.edtSearch.setText("")
            viewModel.updateSearchQuery("")
        }

        // Settings Selection listeners
        binding.groupLanguage.setOnCheckedChangeListener { _, checkedId ->
            val lang = when (checkedId) {
                R.id.radio_lang_auto -> AppLanguageSetting.AUTO
                R.id.radio_lang_en -> AppLanguageSetting.EN
                R.id.radio_lang_ru -> AppLanguageSetting.RU
                R.id.radio_lang_uk -> AppLanguageSetting.UK
                else -> AppLanguageSetting.AUTO
            }
            if (viewModel.languageSetting.value != lang) {
                viewModel.setLanguageSetting(lang)
            }
        }

        binding.groupTheme.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.radio_theme_system -> AppThemeSetting.SYSTEM
                R.id.radio_theme_light -> AppThemeSetting.LIGHT
                R.id.radio_theme_dark -> AppThemeSetting.DARK
                else -> AppThemeSetting.SYSTEM
            }
            if (viewModel.themeSetting.value != theme) {
                viewModel.setThemeSetting(theme)
            }
        }

        // Link redirections
        binding.btnLinkTg1.setOnClickListener { launchUrl("https://t.me/OFFpolice") }
        binding.btnLinkX.setOnClickListener { launchUrl("https://x.com/OFFpolice2077") }
        binding.btnLinkInstagram.setOnClickListener { launchUrl("https://instagram.com/offpolice2077") }
        binding.btnLinkTg2.setOnClickListener { launchUrl("https://t.me/Web_radio_bot/app") }
        binding.linkDevApi.setOnClickListener { launchUrl("https://api.allos.cf") }
    }

    private fun observeViewModel() {
        // 1. Connection Overlay Visibility
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isConnecting.collect { connecting ->
                    binding.loadingScreenOverlay.root.visibility = if (connecting) View.VISIBLE else View.GONE
                }
            }
        }

        // 2. Connection Loader Progress animations
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.connectionProgress.collect { progress ->
                    val barParent = binding.loadingScreenOverlay.loadingProgressValue.parent as View
                    barParent.post {
                        val maxW = barParent.width
                        val targetW = (maxW * (progress / 100f)).toInt()
                        val lp = binding.loadingScreenOverlay.loadingProgressValue.layoutParams
                        lp.width = if (targetW > 0) targetW else 1
                        binding.loadingScreenOverlay.loadingProgressValue.layoutParams = lp
                    }
                }
            }
        }

        // 3. Dynamic Connection Status Labels
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.connectionState.collect { state ->
                    val lang = viewModel.languageSetting.value
                    val key = when (state) {
                        ConnectionState.INITIALIZING -> "state_init"
                        ConnectionState.LOADING_RESOURCES -> "state_resources"
                        ConnectionState.CONNECTING_SERVER -> "state_connecting"
                        ConnectionState.GETTING_STATIONS -> "state_stations"
                        ConnectionState.READY -> "state_ready"
                    }
                    binding.loadingScreenOverlay.loadingStatus.text = Loc.get(key, lang)
                }
            }
        }

        // 4. Tab selection routing
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeTab.collect { tab ->
                    binding.tabRadioContainer.visibility = if (tab == AppTab.RADIO) View.VISIBLE else View.GONE
                    binding.tabFavoritesContainer.visibility = if (tab == AppTab.FAVORITES) View.VISIBLE else View.GONE
                    binding.tabSettingsContainer.visibility = if (tab == AppTab.SETTINGS) View.VISIBLE else View.GONE
                    binding.searchContainer.visibility = if (tab == AppTab.RADIO) View.VISIBLE else View.GONE

                    val activeColor = getColor(R.color.primary_pink)
                    val passiveColor = getColor(R.color.app_text_secondary)

                    binding.navIconRadio.imageTintList = android.content.res.ColorStateList.valueOf(
                        if (tab == AppTab.RADIO) activeColor else passiveColor
                    )
                    binding.navLabelRadio.setTextColor(if (tab == AppTab.RADIO) activeColor else passiveColor)

                    binding.navIconFavorites.setImageResource(
                        if (tab == AppTab.FAVORITES) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                    )
                    binding.navIconFavorites.imageTintList = android.content.res.ColorStateList.valueOf(
                        if (tab == AppTab.FAVORITES) activeColor else passiveColor
                    )
                    binding.navLabelFavorites.setTextColor(if (tab == AppTab.FAVORITES) activeColor else passiveColor)

                    binding.navIconSettings.imageTintList = android.content.res.ColorStateList.valueOf(
                        if (tab == AppTab.SETTINGS) activeColor else passiveColor
                    )
                    binding.navLabelSettings.setTextColor(if (tab == AppTab.SETTINGS) activeColor else passiveColor)
                }
            }
        }

        // 5. App Theme configuration
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.themeSetting.collect { theme ->
                    val expectedMode = when (theme) {
                        AppThemeSetting.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        AppThemeSetting.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                        AppThemeSetting.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    }
                    if (AppCompatDelegate.getDefaultNightMode() != expectedMode) {
                        AppCompatDelegate.setDefaultNightMode(expectedMode)
                    }
                    val checkedId = when (theme) {
                        AppThemeSetting.SYSTEM -> R.id.radio_theme_system
                        AppThemeSetting.LIGHT -> R.id.radio_theme_light
                        AppThemeSetting.DARK -> R.id.radio_theme_dark
                    }
                    binding.groupTheme.check(checkedId)
                }
            }
        }

        // 6. Multi-language dynamic translations
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.languageSetting.collect { lang ->
                    updateLocalization(lang)
                }
            }
        }

        // 7. Small pagination progress spinner loading
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { loading ->
                    binding.bottomLoadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
                }
            }
        }

        // 8. Player Title Tickers
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playerManager.currentName.collect { name ->
                    val lang = viewModel.languageSetting.value
                    binding.playingStationTitle.text = name ?: Loc.get("select_station", lang)
                    binding.playingStationTitle.isSelected = name != null
                }
            }
        }

        // 9. Playback Indicators
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playerManager.playbackState.collect { state ->
                    val lang = viewModel.languageSetting.value
                    
                    binding.playToggleBtn.setImageResource(
                        if (state == PlaybackState.PLAYING) R.drawable.ic_pause else R.drawable.ic_play
                    )

                    val statusText = when (state) {
                        PlaybackState.PLAYING -> Loc.get("playing", lang)
                        PlaybackState.BUFFERING -> Loc.get("buffering", lang)
                        PlaybackState.PAUSED -> Loc.get("paused", lang)
                        PlaybackState.IDLE -> Loc.get("idle", lang)
                        PlaybackState.ERROR -> Loc.get("error", lang)
                    }
                    binding.playerStatusText.text = statusText

                    val tint = when (state) {
                        PlaybackState.PLAYING -> getColor(R.color.primary_pink)
                        PlaybackState.BUFFERING -> getColor(R.color.light_pink)
                        PlaybackState.PAUSED -> getColor(R.color.app_text_secondary)
                        PlaybackState.IDLE -> getColor(R.color.app_text_muted)
                        PlaybackState.ERROR -> Color_Parse_Red()
                    }
                    binding.playerStatusText.setTextColor(tint)
                    binding.playerStatusIndicator.imageTintList = android.content.res.ColorStateList.valueOf(tint)
                }
            }
        }
    }

    private fun Color_Parse_Red(): Int {
        return android.graphics.Color.parseColor("#E53935")
    }

    private fun updateLocalization(lang: AppLanguageSetting) {
        binding.edtSearch.hint = Loc.get("search_placeholder", lang)
        binding.txtSectionTitleRadio.text = Loc.get("all_stations", lang).uppercase()
        binding.txtSectionTitleFavorites.text = Loc.get("tab_favorites", lang).uppercase()
        binding.txtSectionTitleSettings.text = Loc.get("tab_settings", lang).uppercase()
        binding.radioEmptyText.text = Loc.get("nothing_found", lang)
        binding.favoritesEmptyText.text = Loc.get("empty_favorites", lang)
        
        binding.titleSettingLanguage.text = Loc.get("setting_language", lang)
        binding.radioLangAuto.text = Loc.get("language_auto", lang)
        binding.radioLangEn.text = Loc.get("language_en", lang)
        binding.radioLangRu.text = Loc.get("language_ru", lang)
        binding.radioLangUk.text = Loc.get("language_uk", lang)
        
        binding.titleSettingTheme.text = Loc.get("setting_theme", lang)
        binding.radioThemeSystem.text = Loc.get("theme_system", lang)
        binding.radioThemeLight.text = Loc.get("theme_light", lang)
        binding.radioThemeDark.text = Loc.get("theme_dark", lang)
        
        binding.aboutText.text = Loc.get("about_text", lang)
        binding.socialsHeading.text = Loc.get("socials_heading", lang).uppercase()
        binding.linkDevApi.text = Loc.get("link_dev_api", lang)
        binding.versionInfo.text = Loc.get("version_info", lang)
        
        binding.navLabelRadio.text = Loc.get("tab_radio", lang)
        binding.navLabelFavorites.text = Loc.get("tab_favorites", lang)
        binding.navLabelSettings.text = Loc.get("tab_settings", lang)
        
        if (viewModel.playerManager.currentUrl.value == null) {
            binding.playingStationTitle.text = Loc.get("select_station", lang)
        }

        // Set checked states inside groups safely without checking listeners loop triggers
        val langCheckedId = when (lang) {
            AppLanguageSetting.AUTO -> R.id.radio_lang_auto
            AppLanguageSetting.EN -> R.id.radio_lang_en
            AppLanguageSetting.RU -> R.id.radio_lang_ru
            AppLanguageSetting.UK -> R.id.radio_lang_uk
        }
        if (binding.groupLanguage.checkedRadioButtonId != langCheckedId) {
            binding.groupLanguage.check(langCheckedId)
        }
    }

    private fun launchUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Exception opening link: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (viewModel.activeTab.value != AppTab.RADIO) {
            viewModel.selectTab(AppTab.RADIO)
        } else if (viewModel.searchQuery.value.isNotEmpty()) {
            binding.edtSearch.setText("")
            viewModel.updateSearchQuery("")
        } else {
            super.onBackPressed()
        }
    }
}
