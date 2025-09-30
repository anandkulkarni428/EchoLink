package com.anand.echolink.data.net

object NetProtocol {
    // Reuse your audio port for data; control uses port+1
    const val CONTROL_OFFSET = 1

    // Single-byte opcodes
    const val OP_HELLO: Byte = 0x01
    const val OP_GOODBYE: Byte = 0x02
    const val OP_PING: Byte = 0x03
    const val OP_PONG: Byte = 0x04

    // Simple payloads:
    // HELLO: [OP_HELLO][u16 nameLen][bytes name]
    // GOODBYE: [OP_GOODBYE]
    // PING: [OP_PING][i64 epochMillis]
    // PONG: [OP_PONG][i64 echo]
}
