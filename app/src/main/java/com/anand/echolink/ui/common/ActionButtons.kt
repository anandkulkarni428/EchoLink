package com.anand.echolink.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun StartGradientButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.PlayCircle,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val infinite = rememberInfiniteTransition(label = "startAnim")
    val sweep by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "sweep"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.85f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val grad = Brush.linearGradient(
        colors = listOf(cs.primary, cs.secondary, cs.tertiary),
        start = Offset.Zero,
        end = Offset(x = 800f * (0.25f + sweep), y = 0f)
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        modifier = modifier
            .drawBehind {
                drawCircle(
                    color = cs.primary.copy(alpha = 0.20f),
                    radius = size.minDimension * 0.62f * pulse,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }
            .clip(RoundedCornerShape(28.dp))
            .background(grad)
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
            Text(text, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** High-contrast destructive with subtle gradient & border so it pops in true-black dark mode. */
@Composable
fun StopDangerButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.StopCircle,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val brush = Brush.horizontalGradient(
        listOf(cs.error.copy(alpha = 0.98f), cs.error.copy(alpha = 0.78f))
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(brush)              // ← gradient painted reliably
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)              // ensure a tall, tappable target
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}




/** Optional: alternative neutral stop (pause/resume style). */
@Composable
fun StopNeutralButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.PauseCircle,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = cs.surfaceVariant, contentColor = cs.onSurfaceVariant)
    ) {
        Icon(icon, contentDescription = null)
        androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
