package com.anand.echolink.domain

import com.anand.echolink.data.playback.ReceiverPlayer
import com.anand.echolink.data.net.NsdController

class StopListening(
    private val nsd: NsdController,
    private val player: ReceiverPlayer
) {
    fun stop() {
        nsd.stopDiscovery()
        player.stop()
    }
}
