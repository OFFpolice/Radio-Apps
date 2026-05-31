package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
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
    private var mediaPlayer: MediaPlayer? = null

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentUrl = MutableStateFlow<String?>(null)
    val currentUrl: StateFlow<String?> = _currentUrl.asStateFlow()

    private val _currentName = MutableStateFlow<String?>(null)
    val currentName: StateFlow<String?> = _currentName.asStateFlow()

    private val _currentFavicon = MutableStateFlow<String?>(null)
    val currentFavicon: StateFlow<String?> = _currentFavicon.asStateFlow()

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        if (mediaPlayer != null) return
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener {
                start()
                _playbackState.value = PlaybackState.PLAYING
            }
            setOnErrorListener { _, what, extra ->
                _playbackState.value = PlaybackState.ERROR
                true
            }
            setOnInfoListener { _, what, _ ->
                when (what) {
                    MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                        _playbackState.value = PlaybackState.BUFFERING
                        true
                    }
                    MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                        if (isPlaying) {
                            _playbackState.value = PlaybackState.PLAYING
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    fun play(url: String, name: String, favicon: String?) {
        _currentUrl.value = url
        _currentName.value = name
        _currentFavicon.value = favicon
        _playbackState.value = PlaybackState.BUFFERING

        try {
            initializePlayer()
            mediaPlayer?.let { player ->
                player.reset()
                player.setDataSource(context, Uri.parse(url))
                player.prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _playbackState.value = PlaybackState.ERROR
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    _playbackState.value = PlaybackState.PAUSED
                }
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
        try {
            initializePlayer()
            mediaPlayer?.let { player ->
                // If it's already prepared, we can just start
                if (_playbackState.value == PlaybackState.PAUSED) {
                    player.start()
                    _playbackState.value = PlaybackState.PLAYING
                } else {
                    // Otherwise, reload/reprepare to handle potential stream timeout/disconnect
                    play(url, _currentName.value ?: "Station", _currentFavicon.value)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _playbackState.value = PlaybackState.ERROR
        }
    }

    fun togglePlay() {
        if (_playbackState.value == PlaybackState.PLAYING) {
            pause()
        } else {
            resume()
        }
    }

    fun release() {
        try {
            mediaPlayer?.let { player ->
                player.stop()
                player.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            _playbackState.value = PlaybackState.IDLE
        }
    }
}
