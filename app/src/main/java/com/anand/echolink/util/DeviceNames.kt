package com.anand.echolink.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object DeviceNames {

    /** Try to get a friendly local device name. Falls back to model. */
    fun localDeviceName(ctx: Context): String {
        // Try Bluetooth name (readable for normal apps on most devices)
        runCatching {
            val bt = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            val n = bt?.name?.takeIf { !it.isNullOrBlank() }
            if (!n.isNullOrBlank()) return n
        }

        // Try Settings.Secure bluetooth_name (not always accessible)
        runCatching {
            val n = Settings.Secure.getString(ctx.contentResolver, "bluetooth_name")
            if (!n.isNullOrBlank()) return n
        }

        // Fallbacks
        val model = Build.MODEL ?: "Android"
        val manufacturer = Build.MANUFACTURER ?: ""
        return if (!manufacturer.isNullOrBlank() && !model.startsWith(manufacturer, true))
            "$manufacturer $model" else model
    }

    /** Current Wi-Fi SSID (requires ACCESS_FINE_LOCATION + location turned on). */
    fun currentSsid(ctx: Context): String? {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return null

        val wm = ctx.applicationContext.getSystemService(WifiManager::class.java) ?: return null
        val info = wm.connectionInfo ?: return null
        val ssid = info.ssid ?: return null
        if (ssid == "<unknown ssid>" || ssid == "0x") return null
        return ssid.trim('"')
    }
}
