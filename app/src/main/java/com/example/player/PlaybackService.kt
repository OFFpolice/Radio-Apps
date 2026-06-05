package com.example.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun attachBaseContext(newBase: Context?) {
        val nextBase = if (newBase != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            newBase.createAttributionContext("webradio")
        } else {
            newBase
        }
        super.attachBaseContext(nextBase)
    }

    override fun onCreate() {
        super.onCreate()
        
        val player = RadioPlayerManager.sharedPlayer
        if (player != null) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            try {
                mediaSession = MediaSession.Builder(this, player)
                    .setSessionActivity(pendingIntent)
                    .build()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        try {
            mediaSession?.run {
                release()
                mediaSession = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}
