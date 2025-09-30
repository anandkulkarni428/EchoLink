package com.anand.echolink.data.receiver

import android.content.Context
import com.anand.echolink.NetConfig.UDP_PORT
import com.anand.echolink.data.net.NetProtocol
import com.anand.echolink.util.DeviceNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class ReceiverControlClient {

    suspend fun sendHello(ctx: Context, host: String) = withContext(Dispatchers.IO) {
        val name = DeviceNames.localDeviceName(ctx)
        val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
        val bb = ByteBuffer.allocate(1 + 2 + nameBytes.size)
            .put(NetProtocol.OP_HELLO)
            .putShort(nameBytes.size.toShort())
            .put(nameBytes)
            .array()
        DatagramSocket().use { sock ->
            sock.send(DatagramPacket(bb, bb.size, InetAddress.getByName(host), UDP_PORT + NetProtocol.CONTROL_OFFSET))
        }
    }

    suspend fun sendGoodbye(host: String) = withContext(Dispatchers.IO) {
        val bb = byteArrayOf(NetProtocol.OP_GOODBYE)
        DatagramSocket().use { sock ->
            sock.send(DatagramPacket(bb, bb.size, InetAddress.getByName(host), UDP_PORT + NetProtocol.CONTROL_OFFSET))
        }
    }
}
