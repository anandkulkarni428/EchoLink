package com.anand.echolink.data.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.anand.echolink.NetConfig.HELLO_MAGIC
import com.anand.echolink.NetConfig.SERVICE_NAME
import com.anand.echolink.NetConfig.SERVICE_TYPE
import com.anand.echolink.NetConfig.UDP_PORT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class NsdController(private val context: Context) {
    private val nsd = context.getSystemService(NsdManager::class.java)
    private var reg: NsdManager.RegistrationListener? = null
    private var disc: NsdManager.DiscoveryListener? = null

    /** Sender side: advertise the service so receiver can find it. */
    fun registerService(port: Int = UDP_PORT) {
        unregister()
        val info = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        reg = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {}
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }
        nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, reg)
    }

    fun unregister() {
        reg?.let { runCatching { nsd.unregisterService(it) } }
        reg = null
    }

    /** Receiver side: discover and resolve one sender; returns IP + port. */
    fun discover(onResolved: (host: String, port: Int) -> Unit) {
        stopDiscovery()
        disc = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType == SERVICE_TYPE && service.serviceName.startsWith(SERVICE_NAME)) {
                    nsd.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val host = info.host?.hostAddress ?: return
                            onResolved(host, info.port)
                        }
                    })
                }
            }
            override fun onServiceLost(service: NsdServiceInfo) {}
        }
        nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, disc)
    }

    fun stopDiscovery() {
        disc?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        disc = null
    }

    /**
     * Receiver side: send a HELLO to the sender (from an ephemeral local port).
     * Sender will ignore HELLO's source port and stream back to fixed UDP_PORT.
     */
    suspend fun sendHello(senderHost: String, senderPort: Int = UDP_PORT) {
        withContext(Dispatchers.IO) {
            val socket = DatagramSocket() // NO bind → ephemeral port (prevents EADDRINUSE)
            try {
                val addr = InetAddress.getByName(senderHost)
                val payload = byteArrayOf(HELLO_MAGIC)
                val dp = DatagramPacket(payload, payload.size, addr, senderPort)
                socket.send(dp)
            } finally {
                runCatching { socket.close() }
            }
        }
    }
}
