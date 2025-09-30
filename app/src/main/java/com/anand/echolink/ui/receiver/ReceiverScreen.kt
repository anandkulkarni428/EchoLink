package com.anand.echolink.ui.receiver

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anand.echolink.R
import com.anand.echolink.util.DeviceNames

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiverScreen(
    vm: ReceiverViewModel = viewModel(factory = receiverVmFactory(LocalContext.current.applicationContext))
) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current

    // --- (Optional) location permission to show SSID as fallback name ---
    val locLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignore */ }
    LaunchedEffect(Unit) {
        // Ask once on T+ if you want SSID fallback for the “Connected to” label
        if (Build.VERSION.SDK_INT >= 29) {
            locLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val state = vm.state.collectAsState().value
    val level = vm.audioLevel.collectAsState().value

    val connectedLabel = remember(state.discovered) {
        // If you store host device name in state later, use it. For now, fallback to SSID.
        DeviceNames.currentSsid(ctx) ?: state.discovered.ifBlank { "—" }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Join a host", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.surface,
                    titleContentColor = cs.onSurface
                ),
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.echo_logo),
                        contentDescription = "logo",
                        modifier = Modifier.size(50.dp).padding(5.dp)
                    )
                }
            )
        }
    ) { insets ->
        Column(
            Modifier
                .fillMaxSize()
                .background(cs.background)
                .padding(insets)
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Status + connected-to
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 6.dp,
                color = cs.surface
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Headset, contentDescription = null, tint = cs.primary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (state.isListening) "Listening" else "Ready to Join",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Link, contentDescription = null, tint = cs.secondary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Connected to: $connectedLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurface.copy(alpha = 0.8f)
                        )
                    }

                    ReactiveVisualizer(
                        level = level,
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        color = cs.primary,
                        secondary = cs.secondary
                    )
                }
            }

            // Error
            state.error?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 2.dp,
                    color = cs.errorContainer
                ) {
                    Text(
                        text = it,
                        color = cs.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // CTA
            if (!state.isListening) {
                StartPill(
                    text = "Join",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) { vm.listen() }
            } else {
                StopPill(
                    text = "Leave",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) { vm.stopListening() }
            }
        }
    }
}

/* Pretty CTA pills (full-width) */
@Composable
private fun StartPill(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val grad = Brush.linearGradient(listOf(cs.primary, cs.secondary, cs.tertiary))
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        modifier = modifier.background(grad, RoundedCornerShape(28.dp))
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Outlined.PlayCircle, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun StopPill(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val grad = Brush.horizontalGradient(listOf(cs.error.copy(0.98f), cs.error.copy(0.78f)))
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        modifier = modifier.background(grad, RoundedCornerShape(28.dp))
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Outlined.StopCircle, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}
