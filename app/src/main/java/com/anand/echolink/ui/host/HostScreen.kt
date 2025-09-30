package com.anand.echolink.ui.host

import android.app.Activity
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anand.echolink.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostScreen(
    ctx: android.content.Context = LocalContext.current.applicationContext,
    vm: HostViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HostViewModel(ctx) as T
        }
    })
) {
    // Ask for POST_NOTIFICATIONS on Android 13+ (once)
    HostPermissionsGate()

    val isHosting = vm.isHosting.collectAsState().value
    val listeners = vm.listeners.collectAsState().value

    val cs = MaterialTheme.colorScheme
    val activity: Activity? = LocalActivity.current

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            vm.startHosting(res.resultCode, res.data!!)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Host this device", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.surface,
                    titleContentColor = cs.onSurface
                )
            )
        }
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.background)
                .padding(insets)
                .padding(vertical = 20.dp),   // ⬅️ only vertical padding
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card (full width)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 6.dp,
                color = cs.surface
            ) {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.echo_logo),
                        contentDescription = "logo",
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isHosting) "Hosting" else "Ready to Host",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (isHosting) "Nearby devices can Join this host."
                        else "Start Hosting and let others Join.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurface.copy(alpha = 0.75f)
                    )
                }
            }

            // Listeners card (full width)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 6.dp,
                color = cs.surface
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Devices, contentDescription = null, tint = cs.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Listeners (${listeners.size})", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (listeners.isEmpty()) {
                        Text("Waiting for devices to join…", color = cs.onSurface.copy(alpha = 0.7f))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(listeners) { li ->
                                ListItem(
                                    headlineContent = { Text(li.name) },
                                    supportingContent = {
                                        val rtt = li.rttMs?.let { "$it ms" } ?: "measuring…"
                                        Text("Latency: $rtt")
                                    },
                                    leadingContent = {
                                        Icon(
                                            Icons.Outlined.SignalCellularAlt,
                                            contentDescription = null,
                                            tint = cs.secondary
                                        )
                                    },
                                    trailingContent = {
                                        Switch(
                                            checked = li.enabled,
                                            onCheckedChange = { vm.setEnabled(li.hostString, it) }
                                        )
                                    }
                                )
                                Divider()
                            }
                        }
                    }
                }
            }

            // CTA
            if (!isHosting) {
                StartPill(
                    text = "Start Hosting",
                    onClick = {
                        val act = activity ?: return@StartPill
                        val mpm = act.getSystemService(MediaProjectionManager::class.java)
                        projectionLauncher.launch(mpm.createScreenCaptureIntent())
                    }
                )
            } else {
                StopPill(
                    text = "Stop Hosting",
                    onClick = { vm.stopHosting() },
                )
            }
        }
    }
}

/* Mini logo & CTAs */

@Composable
fun EchoLogo(modifier: Modifier = Modifier) {
    val grad = Brush.linearGradient(listOf(Color(0xFF5E7BFF), Color(0xFF00D1B2), Color(0xFFFF7AB6)))
    androidx.compose.foundation.Canvas(modifier) {
        val c = center
        val r = size.minDimension * 0.36f
        drawArc(brush = grad, startAngle = 220f, sweepAngle = 80f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(c.x - r, c.y - r),
            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.14f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawArc(brush = grad, startAngle = 220f, sweepAngle = 80f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(c.x - r * 0.68f, c.y - r * 0.68f),
            size = androidx.compose.ui.geometry.Size(r * 1.36f, r * 1.36f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.12f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        val linkR = r * 0.38f
        drawArc(brush = grad, startAngle = 45f, sweepAngle = 270f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(c.x - linkR, c.y - linkR),
            size = androidx.compose.ui.geometry.Size(linkR * 2, linkR * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.18f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

@Composable
private fun StartPill(text: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val grad = Brush.linearGradient(listOf(cs.primary, cs.secondary, cs.tertiary))
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().height(56.dp)
            .background(grad, RoundedCornerShape(28.dp))
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Outlined.PlayCircle, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun StopPill(text: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val grad = Brush.horizontalGradient(listOf(cs.error.copy(0.98f), cs.error.copy(0.78f)))
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().height(56.dp)
            .background(grad, RoundedCornerShape(28.dp))
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Outlined.StopCircle, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}
