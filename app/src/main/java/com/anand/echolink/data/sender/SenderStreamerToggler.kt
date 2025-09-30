package com.anand.echolink.data.sender

/** Simple function pointer we fill from the service so the VM can toggle devices. */
object SenderStreamerToggler {
    @Volatile private var toggleFn: ((String, Boolean) -> Unit)? = null
    fun register(fn: (hostString: String, enabled: Boolean) -> Unit) { toggleFn = fn }
    fun clear() { toggleFn = null }
    fun set(hostString: String, enabled: Boolean) { toggleFn?.invoke(hostString, enabled) }
}
