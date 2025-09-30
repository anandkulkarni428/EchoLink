package com.anand.echolink.ui.host

/**
 * UI model for a joined listener device.
 */
/** UI list item with toggle state. */
data class UiListenerItem(
    val displayName: String,
    val hostString: String,
    val rttMs: Long? = null,
    val enabled: Boolean = true
)
