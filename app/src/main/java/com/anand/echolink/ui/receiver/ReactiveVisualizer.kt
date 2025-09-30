package com.anand.echolink.ui.receiver

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ultra-light reactive waveform with peak bars.
 *
 * Usage:
 * ReactiveVisualizer(
 *   level = audioLevel, // 0f..1f
 *   modifier = Modifier.fillMaxWidth().height(140.dp),
 *   color = Color(0xFF5E7BFF),
 *   secondary = Color(0xFF00D1B2)
 * )
 */
@Composable
fun ReactiveVisualizer(
    level: Float,
    modifier: Modifier = Modifier,
    color: Color,
    secondary: Color,
    strokeDp: Float = 4f
) {
    // Smooth the external level to avoid jitter (fast attack, slower release).
    var smooth by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(level) {
        // simple one-pole smoothing—tune factors as needed
        val target = level.coerceIn(0f, 1f)
        smooth = if (target > smooth) {
            // quick attack
            (smooth * 0.5f + target * 0.5f)
        } else {
            // slower release
            (smooth * 0.85f + target * 0.15f)
        }
    }

    // Animated phase, speed influenced by smoothed level (more “energy” when louder)
    val phase by rememberInfiniteTransition(label = "rxPhase").animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            tween(
                durationMillis = (1400 - (smooth * 800)).toInt().coerceIn(260, 1400),
                easing = LinearEasing
            )
        ),
        label = "phase"
    )

    val density = LocalDensity.current
    val strokePx = with(density) { strokeDp.dp.toPx() }

    // Cache gradient & outline once per size/colors
    val cachedModifier = modifier.drawWithCache {
        val grad = Brush.horizontalGradient(listOf(color.copy(0.95f), secondary.copy(0.95f)))
        val shadowGrad = Brush.horizontalGradient(
            listOf(Color.Black.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.18f))
        )
        onDrawWithContent {
            // We draw everything ourselves; no child content
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // Adaptive resolution: more points for wider canvases, clamped to avoid overdraw
            val points = (w / 8f).toInt().coerceIn(48, 240)

            // Amplitude scales with smoothed level
            val amp = (h * (0.10f + 0.45f * smooth)).coerceAtMost(h * 0.45f)
            val baseFreq = 2.2f
            val harmonics = 0.35f

            // Reuse a single Path to avoid allocations
            val path = Path()

            fun yAt(t: Float): Float {
                val p = phase
                val y1 = sin((t * baseFreq * 2f * PI + p).toFloat())
                val y2 = sin((t * (baseFreq * 2.6f) * 2f * PI + p * 0.75f).toFloat())
                return (y1 * amp + y2 * amp * harmonics)
            }

            // Build waveform
            for (i in 0..points) {
                val t = i / points.toFloat()
                val x = t * w
                val yy = cy - yAt(t)
                if (i == 0) path.moveTo(x, yy) else path.lineTo(x, yy)
            }

            // Outline for readability (subtle)
            drawPath(
                path = path,
                brush = shadowGrad,
                style = Stroke(width = strokePx + 3f, cap = StrokeCap.Round)
            )

            // Main stroke
            drawPath(
                path = path,
                brush = grad,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Peak/energy bars (cheap; count adapts to width)
            val bars = (w / 28f).toInt().coerceIn(12, 28)
            val gap = w / (bars + 1)
            val barWidth = gap * 0.9f

            for (b in 0 until bars) {
                val t = (b + 1) / (bars + 1f)
                val x = gap * (b + 1)
                // pseudo-FFT feel from phase + t; weight by smooth level
                val energy = abs(sin(phase * 0.7f + t * 6f)) * (0.15f + smooth * 0.85f)
                val barH = h * energy * 0.45f
                drawLine(
                    color = secondary.copy(alpha = 0.55f + 0.35f * energy),
                    start = Offset(x, cy + barH),
                    end = Offset(x, cy - barH),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }

    Canvas(cachedModifier) { /* drawing is in drawWithCache */ }
}
