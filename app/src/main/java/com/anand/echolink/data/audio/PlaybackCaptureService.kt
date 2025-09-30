package com.anand.echolink.data.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.anand.echolink.NetConfig.UDP_PORT
import com.anand.echolink.data.net.NsdController
import com.anand.echolink.data.sender.HostBus
import com.anand.echolink.data.sender.SenderStreamer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlaybackCaptureService : Service() {

    companion object {
        const val ACTION_START = "com.anand.echolink.action.START_CAPTURE"
        const val ACTION_STOP  = "com.anand.echolink.action.STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "res_code"
        const val EXTRA_RESULT_DATA = "res_data"
        private const val CH_ID = "echolink_capture"
        private const val NOTIF_ID = 101
    }

    private var streamer: SenderStreamer? = null
    private var nsd: NsdController? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_STOP  -> stopSelf()
        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun handleStart(intent: Intent) {
        // 1) Start typed foreground service for MediaProjection + Microphone
        startForeground(
            NOTIF_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        // 2) Start advertising via NSD so receiver can discover us (fixed UDP_PORT)
        nsd = NsdController(this).also { it.registerService(port = UDP_PORT) }

        // 3) Spin up the sender streamer with the MediaProjection result
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
            ?: throw IllegalStateException("Missing projection data")

        // Safety: ensure user actually granted projection
        val mpm = getSystemService(MediaProjectionManager::class.java)
        mpm ?: throw IllegalStateException("MediaProjectionManager unavailable")

        streamer = SenderStreamer(applicationContext).apply {
            startWithProjection(resultCode, resultData)
        }
        com.anand.echolink.data.sender.SenderStreamerToggler.register { host, enabled ->
            streamer?.setListenerEnabled(host, enabled)
            HostBus.updateEnabled(host, enabled)
            HostBus.updateFrom(streamer?.listenersSnapshot().orEmpty())
        }

// mark hosting + seed list
        HostBus.setHosting(true)
        HostBus.updateFrom(streamer?.listenersSnapshot().orEmpty())

// optional: poll once a second to keep UI fresh even if no events fire

    }
    val pollJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
        while (true) {
            HostBus.updateFrom(streamer?.listenersSnapshot().orEmpty())
            delay(1000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { streamer?.stop() }
        streamer = null
        runCatching { nsd?.unregister() }
        nsd = null
        com.anand.echolink.data.sender.SenderStreamerToggler.clear()
        HostBus.setHosting(false)
        HostBus.updateFrom(emptyList())
        pollJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CH_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CH_ID,
                    "EchoLink capture",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        return NotificationCompat.Builder(this, CH_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("EchoLink")
            .setContentText("Capturing and streaming device audio")
            .setOngoing(true)
            .build()
    }
}
