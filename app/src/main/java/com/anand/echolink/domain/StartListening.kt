package com.anand.echolink.domain

import com.anand.echolink.data.net.NsdController
import com.anand.echolink.data.playback.ReceiverPlayer
import com.anand.echolink.NetConfig.UDP_PORT

class StartListening(
    private val nsd: NsdController,
    private val player: ReceiverPlayer
) {
    fun discover(onResolved: (host: String, port: Int) -> Unit) = nsd.discover(onResolved)

    suspend fun sendHello(host: String, port: Int = UDP_PORT) = nsd.sendHello(host, port)

    suspend fun startPlayer(bindHost: String = "0.0.0.0", port: Int = UDP_PORT) = player.start(bindHost, port)
}
