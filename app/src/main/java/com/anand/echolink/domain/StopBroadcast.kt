package com.anand.echolink.domain

import com.anand.echolink.data.audio.PlaybackCaptureController
import com.anand.echolink.data.net.NsdController

class StopBroadcast(
    private val capture: PlaybackCaptureController,
    private val nsd: NsdController
) {
    fun stop() {
        capture.stop()
        nsd.unregister()
    }
}
