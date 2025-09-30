package com.anand.echolink.ui.host

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

/**
 * Requests RECORD_AUDIO + POST_NOTIFICATIONS (T+) once, if not already granted.
 * Call this inside HostScreen() before starting hosting.
 */
@Composable
fun HostPermissionsGate() {
    val ctx = LocalContext.current

    var asked by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Results come back here; user can also enable later in system Settings
    }

    LaunchedEffect(Unit) {
        if (!asked) {
            val perms = mutableListOf<String>()

            // Microphone
            val micGranted = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!micGranted) perms += Manifest.permission.RECORD_AUDIO

            // Notifications (Android 13+ only)
            if (Build.VERSION.SDK_INT >= 33) {
                val nm = NotificationManagerCompat.from(ctx)
                if (!nm.areNotificationsEnabled()) {
                    perms += Manifest.permission.POST_NOTIFICATIONS
                }
            }

            if (perms.isNotEmpty()) {
                asked = true
                launcher.launch(perms.toTypedArray())
            }
        }
    }
}
