package com.example.player

import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlaybackState {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ERROR
}

class RadioPlayerManager(private val context: Context) {
    private val contextToUse = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        context.createAttributionContext("webradio")
    } else {
        context
    }
    private var exoPlayer: ExoPlayer? = null

    companion object {
        @JvmStatic
        var sharedPlayer: ExoPlayer? = null
    }

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentUrl = MutableStateFlow<String?>(null)
    val currentUrl: StateFlow<String?> = _currentUrl.asStateFlow()

    private val _currentName = MutableStateFlow<String?>(null)
    val currentName: StateFlow<String?> = _currentName.asStateFlow()

    private val _currentFavicon = MutableStateFlow<String?>(null)
    val currentFavicon: StateFlow<String?> = _currentFavicon.asStateFlow()

    // Flag to handle manual pause state from user
    private var isManuallyPaused = false

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        if (exoPlayer != null) return

        // Set low buffering limits so that playback starts immediately!
        // We set targets for live stream speed optimization with 0.25s start latency.
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
    }

    fun play(url: String, name: String, favicon: String?) {
        _currentUrl.value = url
        _currentName.value = name
        _currentFavicon.value = favicon
        _playbackState.value = PlaybackState.BUFFERING
        isManuallyPaused = false

        try {
            initializePlayer()

            val metadata = MediaMetadata.Builder()
                .setTitle(name)
                .setArtist("Web Radio")
                .apply {
                    if (!favicon.isNullOrBlank()) {
                        setArtworkUri(android.net.Uri.parse(favicon))
                    }
                }
                .build()

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
            _playbackState.value = PlaybackState.IDLE
        }
    }
}
