package com.offpolice.webradiobot.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
import com.offpolice.webradiobot.MainActivity
import com.offpolice.webradiobot.R

@OptIn(UnstableApi::class)
class RadioPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "webradio_media_playback"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.playback_channel_name)
            .setNotificationId(NOTIFICATION_ID)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_radio_notification)
        setMediaNotificationProvider(notificationProvider)

        val playerManager = RadioPlayerManager.getInstance(applicationContext)
        val player = playerManager.getOrInitPlayer()

        val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_STOP)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
                    .add(Player.COMMAND_GET_METADATA)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return command == Player.COMMAND_SEEK_TO_NEXT ||
                        command == Player.COMMAND_SEEK_TO_PREVIOUS ||
                        command == Player.COMMAND_PLAY_PAUSE ||
                        command == Player.COMMAND_STOP ||
                        command == Player.COMMAND_GET_CURRENT_MEDIA_ITEM ||
                        command == Player.COMMAND_GET_METADATA ||
                        super.isCommandAvailable(command)
            }

            override fun play() {
                playerManager.resume()
            }

            override fun pause() {
                playerManager.pause()
            }

            override fun stop() {
                playerManager.pause()
            }

            override fun seekToNext() {
                playerManager.playNextStation()
            }

            override fun seekToPrevious() {
                playerManager.playPreviousStation()
            }
        }

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(sessionActivityIntent)
            .setCallback(MediaSessionCallback())
            .build()

        mediaSession?.let { session ->
            addSession(session)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.playback_channel_name)
            val descriptionText = getString(R.string.playback_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .build()
            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_STOP)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
                .add(Player.COMMAND_GET_METADATA)
                .build()
            return MediaSession.ConnectionResult.accept(sessionCommands, playerCommands)
        }

        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int
        ): Int {
            when (playerCommand) {
                Player.COMMAND_PLAY_PAUSE -> {
                    val player = session.player
                    if (player.playWhenReady && player.playbackState != Player.STATE_IDLE) {
                        RadioPlayerManager.getInstance(applicationContext).pause()
                    } else {
                        RadioPlayerManager.getInstance(applicationContext).resume()
                    }
                    return SessionResult.RESULT_SUCCESS
                }
                Player.COMMAND_STOP -> {
                    RadioPlayerManager.getInstance(applicationContext).pause()
                    return SessionResult.RESULT_SUCCESS
                }
                Player.COMMAND_SEEK_TO_NEXT -> {
                    RadioPlayerManager.getInstance(applicationContext).playNextStation()
                    return SessionResult.RESULT_SUCCESS
                }
                Player.COMMAND_SEEK_TO_PREVIOUS -> {
                    RadioPlayerManager.getInstance(applicationContext).playPreviousStation()
                    return SessionResult.RESULT_SUCCESS
                }
            }
            return super.onPlayerCommandRequest(session, controller, playerCommand)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            removeSession(this)
            release()
            mediaSession = null
        }
        RadioPlayerManager.getInstance(applicationContext).onServiceDestroyed()
        super.onDestroy()
    }
}
