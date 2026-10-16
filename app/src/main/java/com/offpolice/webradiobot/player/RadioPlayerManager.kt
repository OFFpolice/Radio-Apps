package com.offpolice.webradiobot.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.extractor.metadata.icy.IcyInfo
import java.util.ArrayList
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

@OptIn(UnstableApi::class)
class RadioPlayerManager private constructor(private val context: Context) {

    private val appContext = context.applicationContext
    private var exoPlayer: ExoPlayer? = null

    companion object {
        @Volatile
        private var instance: RadioPlayerManager? = null

        fun getInstance(context: Context): RadioPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: RadioPlayerManager(context.applicationContext).also {
                    instance = it
                }
            }
        }

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

    private val _currentArtist = MutableStateFlow<String?>(null)
    val currentArtist: StateFlow<String?> = _currentArtist.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow<String?>(null)
    val currentTrackTitle: StateFlow<String?> = _currentTrackTitle.asStateFlow()

    // Navigation callbacks for next/previous station
    var onNextStation: (() -> Unit)? = null
    var onPreviousStation: (() -> Unit)? = null

    fun playNextStation() {
        onNextStation?.invoke()
    }

    fun playPreviousStation() {
        onPreviousStation?.invoke()
    }

    // Flag to track explicit manual pause from user
    private var isManuallyPaused = false

    fun initStationWithoutPlayback(url: String, name: String, favicon: String?) {
        if (_currentUrl.value == null) {
            _currentUrl.value = url
            _currentName.value = name
            _currentFavicon.value = favicon
            _currentArtist.value = null
            _currentTrackTitle.value = null
            _playbackState.value = PlaybackState.PAUSED
            isManuallyPaused = true
        }
    }

    fun getOrInitPlayer(): ExoPlayer {
        exoPlayer?.let { return it }

        // Low buffering limits for near-instant radio stream start
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

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("WebRadioBot/1.0 (Android)")
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))

        val mediaSourceFactory = DefaultMediaSourceFactory(appContext)
            .setDataSourceFactory(httpDataSourceFactory)

        // Custom MediaCodecSelector prioritizing OMX software decoders to avoid CCodec hardware query errors
        val safeCodecSelector = MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            val decoders = MediaCodecSelector.DEFAULT.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder
            )
            decoders.sortedWith(
                compareByDescending<androidx.media3.exoplayer.mediacodec.MediaCodecInfo> { it.name.startsWith("OMX.google.") }
                    .thenByDescending { it.softwareOnly }
                    .thenByDescending { it.name.startsWith("c2.android.") }
            )
        }

        // Audio-only renderer factory skips video renderers to avoid emulator Codec2 errors
        val renderersFactory = object : DefaultRenderersFactory(appContext) {
            override fun buildVideoRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                eventHandler: Handler,
                eventListener: VideoRendererEventListener,
                allowedVideoJoiningTimeMs: Long,
                out: ArrayList<Renderer>
            ) {
                // Audio-only radio streaming: skip video renderers
            }
        }.apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            setEnableDecoderFallback(true)
            setMediaCodecSelector(safeCodecSelector)
            forceDisableMediaCodecAsynchronousQueueing()
        }

        val player = ExoPlayer.Builder(appContext, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
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
                            Player.STATE_ENDED,
                            Player.STATE_IDLE -> {
                                _playbackState.value = PlaybackState.IDLE
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        error.printStackTrace()
                        _playbackState.value = PlaybackState.ERROR
                    }

                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        if (!playWhenReady) {
                            isManuallyPaused = true
                            _playbackState.value = PlaybackState.PAUSED
                        } else {
                            isManuallyPaused = false
                            if (exoPlayer?.playbackState == Player.STATE_READY) {
                                _playbackState.value = PlaybackState.PLAYING
                            } else {
                                _playbackState.value = PlaybackState.BUFFERING
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            isManuallyPaused = false
                            _playbackState.value = PlaybackState.PLAYING
                        } else if (_playbackState.value == PlaybackState.PLAYING) {
                            _playbackState.value = PlaybackState.PAUSED
                        }
                    }

                    override fun onMetadata(metadata: Metadata) {
                        for (i in 0 until metadata.length()) {
                            val entry = metadata.get(i)
                            if (entry is IcyInfo) {
                                entry.title?.let { parseAndSetTrackInfo(it) }
                            }
                        }
                    }

                    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                        updateMetadataFromExo(mediaMetadata)
                    }
                })
            }

        exoPlayer = player
        sharedPlayer = player
        return player
    }

    private fun parseAndSetTrackInfo(rawTitle: String) {
        val trimmed = rawTitle.trim()
        if (trimmed.isEmpty()) return
        val stationName = _currentName.value
        if (!stationName.isNullOrBlank() && trimmed.equals(stationName, ignoreCase = true)) {
            return
        }

        var artist: String? = null
        var trackTitle: String = trimmed
        val separator = listOf(" - ", " – ", " — ", " -", "- ").find { trimmed.contains(it) }
        if (separator != null) {
            val parts = trimmed.split(separator, limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                artist = parts[0].trim()
                trackTitle = parts[1].trim()
            }
        }
        _currentArtist.value = artist
        _currentTrackTitle.value = trackTitle

        updateSessionMetadata(artist, trackTitle)
    }

    private fun updateMetadataFromExo(mediaMetadata: MediaMetadata) {
        val title = mediaMetadata.title?.toString()
            ?: mediaMetadata.displayTitle?.toString()
        val artist = mediaMetadata.artist?.toString()
            ?: mediaMetadata.albumArtist?.toString()

        if (!artist.isNullOrBlank() && artist != "Web Radio") {
            _currentArtist.value = artist.trim()
        }
        if (!title.isNullOrBlank()) {
            parseAndSetTrackInfo(title)
        }
    }

    private fun updateSessionMetadata(artist: String?, trackTitle: String?) {
        val stationName = _currentName.value ?: "Web Radio"
        val favicon = _currentFavicon.value
        val displayTitle = if (!trackTitle.isNullOrBlank()) trackTitle else stationName
        val displayArtist = if (!trackTitle.isNullOrBlank()) {
            if (!artist.isNullOrBlank()) artist else stationName
        } else {
            artist ?: "Web Radio"
        }

        val updatedMetadata = MediaMetadata.Builder()
            .setTitle(displayTitle)
            .setDisplayTitle(displayTitle)
            .setArtist(displayArtist)
            .apply {
                if (!favicon.isNullOrBlank()) {
                    setArtworkUri(Uri.parse(favicon))
                }
            }
            .build()

        exoPlayer?.playlistMetadata = updatedMetadata
    }

    fun startPlaybackService() {
        try {
            val serviceIntent = Intent(context, RadioPlaybackService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun play(url: String, name: String, favicon: String?) {
        _currentUrl.value = url
        _currentName.value = name
        _currentFavicon.value = favicon
        _currentArtist.value = null
        _currentTrackTitle.value = null
        _playbackState.value = PlaybackState.BUFFERING
        isManuallyPaused = false

        try {
            val player = getOrInitPlayer()
            startPlaybackService()

            val metadata = MediaMetadata.Builder()
                .setTitle(name)
                .setDisplayTitle(name)
                .setArtist("Web Radio")
                .apply {
                    if (!favicon.isNullOrBlank()) {
                        setArtworkUri(Uri.parse(favicon))
                    }
                }
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(metadata)
                .build()

            player.apply {
                stop()
                clearMediaItems()
                setMediaItem(mediaItem)
                playlistMetadata = metadata
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
            val player = getOrInitPlayer()
            startPlaybackService()

            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED || player.mediaItemCount == 0) {
                play(url, _currentName.value ?: "Station", _currentFavicon.value)
            } else {
                player.playWhenReady = true
                player.prepare()
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

    fun onServiceDestroyed() {
        exoPlayer = null
        sharedPlayer = null
        _playbackState.value = PlaybackState.IDLE
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
