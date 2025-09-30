package com.anand.echolink.ui.sender

import android.app.Activity
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anand.echolink.ui.common.StopDangerButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenderScreen(
    appCtx: android.content.Context = LocalContext.current.applicationContext,
    vm: SenderViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SenderViewModel(appCtx) as T
        }
    })
) {
    val cs = MaterialTheme.colorScheme
    val state by vm.state.collectAsState()
    val activity: Activity? = LocalActivity.current

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            vm.startBroadcast(res.resultCode, res.data!!)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Host") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.surface,
                    titleContentColor = cs.onSurface
                )
            )
        }
    ) { insets ->
        Column(
            Modifier
                .fillMaxSize()
                .background(cs.background)
                .padding(insets)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status panel
            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 6.dp,
                color = cs.surface,
                contentColor = cs.onSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(18.dp)
                ) {
                    Icon(
                        imageVector = if (state.isBroadcasting) Icons.Outlined.Bolt else Icons.Outlined.PlayCircle,
                        contentDescription = null,
                        tint = cs.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (state.isBroadcasting) "Broadcasting" else "Ready",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (state.isBroadcasting)
                                "Your audio is being sent over the network."
                            else
                                "Tap Start and allow the capture dialog.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // ★ Animated gradient pill for Start, bold destructive for Stop
            if (!state.isBroadcasting) {
                StartGradientButton(
                    text = "Start Hosting",
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    onClick = {
                        val act = activity ?: return@StartGradientButton
                        val mpm = act.getSystemService(android.media.projection.MediaProjectionManager::class.java)
                        projectionLauncher.launch(mpm.createScreenCaptureIntent())
                    }
                )
            } else {
                StopDangerButton(
                    text = "Stop Hosting",
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    onClick = { vm.stopBroadcast() }
                )
            }

            state.error?.let { Text(it, color = cs.error) }

            Spacer(Modifier.height(6.dp))
            Text(
                "Tip: enable Hotspot on this phone for a more stable link.",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

/* ───────────────────────── Buttons ───────────────────────── */

@Composable
private fun StartGradientButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    // Animated sweeping gradient + subtle pulsing glow (same style as your Listen button)
    val infinite = rememberInfiniteTransition(label = "startAnim")
    val sweep by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2500, easing = LinearEasing)),
        label = "sweep"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.85f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Color band for Start button (feel free to tweak)
    val gradColors = remember(cs.primary, cs.secondary, cs.tertiary) {
        listOf(
            cs.primary.copy(alpha = 0.98f),
            cs.secondary.copy(alpha = 0.98f),
            cs.tertiary.copy(alpha = 0.98f)
        )
    }
    val brush = Brush.linearGradient(
        colors = gradColors,
        start = Offset(0f, 0f),
        end = Offset(x = 800f * (0.25f + sweep), y = 0f)
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .drawBehind {
                // Soft glow halo
                drawCircle(
                    color = cs.primary.copy(alpha = 0.20f),
                    radius = size.minDimension * 0.62f * pulse,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }
            .clip(RoundedCornerShape(28.dp))
            .background(brush)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun StopButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = cs.error,
            contentColor = cs.onError
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
    ) {
        Icon(Icons.Outlined.StopCircle, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
