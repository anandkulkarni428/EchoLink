package com.anand.echolink.domain


import android.app.Activity
import android.content.Intent
import com.anand.echolink.data.audio.PlaybackCaptureController
import com.anand.echolink.data.net.NsdController

class StartBroadcast(
    private val capture: PlaybackCaptureController,
    private val nsd: NsdController
) {
    fun requestProjectionIntent(activity: Activity) = capture.requestProjectionIntent(activity)
    fun startFromProjectionResult(resultCode: Int, data: Intent) {
        capture.start(resultCode, data)
        nsd.registerService()
    }
}
