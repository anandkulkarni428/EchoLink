package com.anand.echolink

object NetConfig {
    const val UDP_PORT = 50005
    const val SAMPLE_RATE = 48_000
    const val CHANNELS = 2

    // NSD
    const val SERVICE_NAME = "EchoLinkSender"
    const val SERVICE_TYPE = "_echolink._udp." // trailing dot is required

    // Packet magics
    val STREAM_MAGIC: Byte = 0xAA.toByte() // stream packet magic
    val HELLO_MAGIC: Byte  = 0xAB.toByte() // handshake hello magic
}
