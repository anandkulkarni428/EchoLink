package com.anand.echolink.ui.shared


import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequireAudioPermission(content: @Composable () -> Unit) {
    val recordPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    if (recordPermission.status.isGranted) {
        content()
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Microphone permission is required to capture playback audio.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = { recordPermission.launchPermissionRequest() }) { Text("Grant Permission") }
        }
    }
}
