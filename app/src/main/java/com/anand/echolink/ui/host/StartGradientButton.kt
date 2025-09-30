package com.anand.echolink.ui.host

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Animated gradient pill for "Start Hosting".
 */
@Composable
fun StartGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.PlayCircle, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}
