package com.anand.echolink.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object HostNotifications {
    private const val CHANNEL_ID = "host_events"
    private const val CHANNEL_NAME = "EchoLink Host"
    private const val JOIN_ID = 1001
    private const val LEAVE_ID = 1002

    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = ctx.getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Device join/leave notifications"
                enableLights(true)
                lightColor = Color.CYAN
            }
            mgr.createNotificationChannel(ch)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notifyJoin(ctx: Context, deviceName: String, ip: String) {
        ensureChannel(ctx)
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth) // ✅ use built-in icon
            .setContentTitle("Device joined")
            .setContentText("$deviceName • $ip")
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(JOIN_ID, n)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notifyLeave(ctx: Context, deviceName: String, ip: String) {
        ensureChannel(ctx)
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth) // ✅ use built-in icon
            .setContentTitle("Device left")
            .setContentText("$deviceName • $ip")
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(LEAVE_ID, n)
    }
}
