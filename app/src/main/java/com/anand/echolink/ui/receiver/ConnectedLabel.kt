package com.anand.echolink.ui.receiver

import android.content.Context
import com.anand.echolink.util.DeviceNames

/**
 * Prefer showing the Host's device name if you know it.
 * Otherwise, show current Wi-Fi SSID as a hint of the hotspot.
 */
fun connectedLabel(ctx: Context, hostDeviceNameFromHello: String?): String {
    return hostDeviceNameFromHello?.takeIf { it.isNotBlank() }
        ?: (DeviceNames.currentSsid(ctx) ?: "Connected")
}
