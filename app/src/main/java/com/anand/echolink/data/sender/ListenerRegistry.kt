package com.anand.echolink.data.sender

import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

data class ListenerInfo(
    val addr: InetSocketAddress,
    @Volatile var name: String = "",
    @Volatile var enabled: Boolean = true,
    @Volatile var lastSeenMs: Long = System.currentTimeMillis(),
    @Volatile var rttMs: Long = -1
)

/** Thread-safe registry of listeners with enable/disable. */
class ListenerRegistry {
    private val map = ConcurrentHashMap<InetSocketAddress, ListenerInfo>()

    fun upsert(addr: InetSocketAddress, name: String?) {
        val fresh = !map.containsKey(addr)
        val e = map.getOrPut(addr) { ListenerInfo(addr = addr) }
        if (!name.isNullOrEmpty()) e.name = name
        e.lastSeenMs = System.currentTimeMillis()
        if (fresh) e.enabled = true
    }

    fun remove(addr: InetSocketAddress) { map.remove(addr) }
    fun setEnabled(addr: InetSocketAddress, value: Boolean) { map[addr]?.enabled = value }
    fun setRtt(addr: InetSocketAddress, rtt: Long) { map[addr]?.rttMs = rtt }

    fun all(): List<ListenerInfo> = map.values.sortedBy { it.addr.hostString }
    fun enabledList(): List<ListenerInfo> = all().filter { it.enabled }
    fun size(): Int = map.size

    /** Addresses not seen within threshold. */
    fun stale(thresholdMs: Long): List<InetSocketAddress> {
        val now = System.currentTimeMillis()
        return map.values.filter { now - it.lastSeenMs > thresholdMs }.map { it.addr }
    }
}
