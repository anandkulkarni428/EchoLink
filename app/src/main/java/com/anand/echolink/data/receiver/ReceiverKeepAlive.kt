package com.anand.echolink.data.receiver

import com.anand.echolink.NetConfig.UDP_PORT
import com.anand.echolink.data.net.NetProtocol
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer

/**
 * Listens on control port (UDP_PORT+1) for PING from Host and replies PONG.
 * Also re-sends HELLO every 15s as a safety.
 */
class ReceiverKeepAlive {

    private var job: Job? = null
    private var sock: DatagramSocket? = null
    @Volatile private var host: String? = null

    fun start(hostIp: String) {
        if (job?.isActive == true) return
        host = hostIp
        job = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            // Control socket bound to any port; we'll receive PINGs addressed to our ip:UDP_PORT+1
            sock = DatagramSocket(UDP_PORT + NetProtocol.CONTROL_OFFSET).apply {
                soTimeout = 1000
                reuseAddress = true
            }

            val buf = ByteArray(256)
            var lastHello = 0L

            while (isActive) {
                // 1) Answer PING → PONG
                try {
                    val dp = DatagramPacket(buf, buf.size)
                    sock?.receive(dp)

                    val bb = ByteBuffer.wrap(dp.data, 0, dp.length)
                    val op = bb.get()
                    if (op == NetProtocol.OP_PING) {
                        val echo = bb.long
                        val pong = ByteBuffer.allocate(1 + 8)
                            .put(NetProtocol.OP_PONG)
                            .putLong(echo)
                            .array()
                        // reply to host control port
                        sock?.send(
                            DatagramPacket(
                                pong, pong.size,
                                InetSocketAddress(dp.address, UDP_PORT + NetProtocol.CONTROL_OFFSET)
                            )
                        )
                    }
                } catch (_: java.net.SocketTimeoutException) {
                    // normal idle tick
                } catch (_: Throwable) {
                    // ignore and keep loop
                }

                // 2) Periodic HELLO (safety, helps if host restarted)
                val now = System.currentTimeMillis()
                if (now - lastHello > 15_000 && host != null) {
                    sendHelloOnce(host!!)
                    lastHello = now
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        runCatching { sock?.close() }
        sock = null
        host = null
    }

    private fun sendHelloOnce(host: String) {
        runCatching {
            DatagramSocket().use { s ->
                val name = "Android"
                val nameBytes = name.toByteArray(Charsets.UTF_8)
                val bb = ByteBuffer.allocate(1 + 2 + nameBytes.size)
                    .put(NetProtocol.OP_HELLO)
                    .putShort(nameBytes.size.toShort())
                    .put(nameBytes)
                    .array()
                s.send(DatagramPacket(bb, bb.size, InetAddress.getByName(host), UDP_PORT + NetProtocol.CONTROL_OFFSET))
            }
        }
    }
}
