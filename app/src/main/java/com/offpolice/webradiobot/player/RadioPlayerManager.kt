package com.offpolice.webradiobot.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.offpolice.webradiobot.data.ApiStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PlaybackState {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ERROR
}

class RadioPlayerManager(private val context: Context) {
    private val contextToUse = context.applicationContext
    private var exoPlayer: ExoPlayer? = null

    companion object {
        @JvmStatic
        var sharedPlayer: ExoPlayer? = null

        @JvmStatic
        var sharedManager: RadioPlayerManager? = null
    }

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentUrl = MutableStateFlow<String?>(null)
    val currentUrl: StateFlow<String?> = _currentUrl.asStateFlow()

    private val _currentName = MutableStateFlow<String?>(null)
    val currentName: StateFlow<String?> = _currentName.asStateFlow()

    private val _currentFavicon = MutableStateFlow<String?>(null)
    val currentFavicon: StateFlow<String?> = _currentFavicon.asStateFlow()

    private var stationList: List<ApiStation> = emptyList()

    // Flag to handle manual pause state from user
    private var isManuallyPaused = false

    init {
        sharedManager = this
        initializePlayer()
    }

    fun updateStationList(list: List<ApiStation>) {
        stationList = list
    }

    fun playNextStation() {
        if (stationList.isEmpty()) return
        val currentUrlValue = _currentUrl.value ?: return
        val currentIndex = stationList.indexOfFirst { it.url_resolved == currentUrlValue }
        if (currentIndex != -1) {
            val nextIndex = (currentIndex + 1) % stationList.size
            val nextStation = stationList[nextIndex]
            play(nextStation.url_resolved, nextStation.name, nextStation.favicon)
        }
    }

    fun playPreviousStation() {
        if (stationList.isEmpty()) return
        val currentUrlValue = _currentUrl.value ?: return
        val currentIndex = stationList.indexOfFirst { it.url_resolved == currentUrlValue }
        if (currentIndex != -1) {
            val prevIndex = (currentIndex - 1 + stationList.size) % stationList.size
            val prevStation = stationList[prevIndex]
            play(prevStation.url_resolved, prevStation.name, prevStation.favicon)
        }
    }

    private fun startService() {
        try {
            val intent = Intent(contextToUse, RadioPlaybackService::class.java)
            contextToUse.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initializePlayer() {
        if (exoPlayer != null) return

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1000, // minBufferMs
                3000, // maxBufferMs
                250,  // bufferForPlaybackMs
                500   // bufferForPlaybackAfterRebufferMs
            )
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(contextToUse)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .build()
            .apply {
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> {
                                if (!isManuallyPaused) {
                                    _playbackState.value = PlaybackState.BUFFERING
                                }
                            }
                            Player.STATE_READY -> {
                                if (playWhenReady) {
                                    _playbackState.value = PlaybackState.PLAYING
                                } else {
                                    _playbackState.value = PlaybackState.PAUSED
                                }
                            }
                            Player.STATE_ENDED -> {
                                _playbackState.value = PlaybackState.IDLE
                            }
                            Player.STATE_IDLE -> {
                                _playbackState.value = PlaybackState.IDLE
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        error.printStackTrace()
                        _playbackState.value = PlaybackState.ERROR
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            _playbackState.value = PlaybackState.PLAYING
                        } else if (_playbackState.value == PlaybackState.PLAYING) {
                            _playbackState.value = PlaybackState.PAUSED
                        }
                    }
                })
            }
        sharedPlayer = exoPlayer
        startService()
    }

    fun play(url: String, name: String, favicon: String?) {
        _currentUrl.value = url
        _currentName.value = name
        _currentFavicon.value = favicon
        _playbackState.value = PlaybackState.BUFFERING
        isManuallyPaused = false

        try {
            initializePlayer()

            val metadataBuilder = MediaMetadata.Builder()
                .setTitle(name)
                .setArtist("Web Radio")
                .setDisplayTitle(name)
                .setSubtitle("Web Radio")
                .apply {
                    if (!favicon.isNullOrBlank()) {
                        setArtworkUri(Uri.parse(favicon))
                    }
                }

            val metadata = metadataBuilder.build()

            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(metadata)
                .build()

            exoPlayer?.apply {
                stop()
                clearMediaItems()
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }

            // Asynchronously fetch artwork image bytes for System Lockscreen Media Notification
            if (!favicon.isNullOrBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val urlObj = java.net.URL(favicon)
                        val connection = urlObj.openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        connection.doInput = true
                        connection.connect()
                        val input = connection.inputStream
                        val bytes = input.readBytes()
                        input.close()
                        withContext(Dispatchers.Main) {
                            if (_currentUrl.value == url && exoPlayer != null) {
                                val updatedMetadata = metadata.buildUpon()
                                    .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                                    .build()
                                val currentItem = exoPlayer?.currentMediaItem
                                if (currentItem != null) {
                                    val updatedItem = currentItem.buildUpon()
                                        .setMediaMetadata(updatedMetadata)
                                        .build()
                                    exoPlayer?.replaceMediaItem(0, updatedItem)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore network artwork errors, fallback to default Uri
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _playbackState.value = PlaybackState.ERROR
        }
    }

    fun pause() {
        isManuallyPaused = true
        _playbackState.value = PlaybackState.PAUSED
        try {
            exoPlayer?.apply {
                playWhenReady = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resume() {
        val url = _currentUrl.value
        if (url == null) {
            _playbackState.value = PlaybackState.IDLE
            return
        }
        isManuallyPaused = false
        try {
            initializePlayer()
            exoPlayer?.apply {
                if (_playbackState.value == PlaybackState.PAUSED) {
                    playWhenReady = true
                } else {
                    play(url, _currentName.value ?: "Station", _currentFavicon.value)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _playbackState.value = PlaybackState.ERROR
        }
    }

    fun togglePlay() {
        val current = _playbackState.value
        if (current == PlaybackState.PLAYING || current == PlaybackState.BUFFERING) {
            pause()
        } else {
            resume()
        }
    }

    fun release() {
        try {
            exoPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            exoPlayer = null
            sharedPlayer = null
            sharedManager = null
            _playbackState.value = PlaybackState.IDLE
        }
    }
}
