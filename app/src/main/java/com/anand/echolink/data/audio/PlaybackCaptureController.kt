package com.anand.echolink.data.audio


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.annotation.RequiresApi

class PlaybackCaptureController(private val context: Context) {

    fun requestProjectionIntent(activity: Activity): Intent {
        val mpm = context.getSystemService(MediaProjectionManager::class.java)
        return mpm.createScreenCaptureIntent()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun start(resultCode: Int, data: Intent) {
        val intent = Intent(context, PlaybackCaptureService::class.java).apply {
            action = PlaybackCaptureService.ACTION_START
            putExtra(PlaybackCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(PlaybackCaptureService.EXTRA_RESULT_DATA, data)
        }
        context.startForegroundService(intent)
    }

    fun stop() {
        context.startService(Intent(context, PlaybackCaptureService::class.java).apply {
            action = PlaybackCaptureService.ACTION_STOP
        })
    }
}
