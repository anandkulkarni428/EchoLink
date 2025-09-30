package com.anand.echolink.data.sender

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Publishes host state (isHosting + listeners) to the UI.
 * Standardizes on List<ListenerInfo> so callers can pass snapshots.
 */
object HostBus {
    private val _isHosting = MutableStateFlow(false)
    private val _listeners = MutableStateFlow<List<HostListenerItem>>(emptyList())

    val isHosting: StateFlow<Boolean> = _isHosting.asStateFlow()
    val listeners: StateFlow<List<HostListenerItem>> = _listeners.asStateFlow()

    fun setHosting(active: Boolean) { _isHosting.value = active }

    /** Preferred API: pass a List<ListenerInfo> (e.g., from listenersSnapshot()). */
    fun updateFrom(list: List<ListenerInfo>) {
        _listeners.value = list.map {
            HostListenerItem(
                name = if (it.name.isNotBlank()) it.name else it.addr.hostString,
                hostString = it.addr.hostString,
                rttMs = it.rttMs.takeIf { r -> r >= 0 },
                enabled = it.enabled
            )
        }
    }

    /** Optional convenience, if you still have a registry in scope and it's public. */
    fun updateFromRegistry(registryAll: () -> List<ListenerInfo>) {
        updateFrom(registryAll())
    }

    fun updateEnabled(hostString: String, enabled: Boolean) {
        _listeners.value = _listeners.value.map {
            if (it.hostString == hostString) it.copy(enabled = enabled) else it
        }
    }
}

data class HostListenerItem(
    val name: String,
    val hostString: String,
    val rttMs: Long? = null,
    val enabled: Boolean = true
)
