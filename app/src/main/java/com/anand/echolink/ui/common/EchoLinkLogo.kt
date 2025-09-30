package com.anand.echolink.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun EchoLinkLogo(modifier: Modifier = Modifier) {
    val grad = Brush.linearGradient(
        colors = listOf(
            Color(0xFF5E7BFF), // indigo
            Color(0xFF00D1B2), // teal
            Color(0xFFFF7AB6)  // pink
        )
    )
    Canvas(modifier) {
        val c = center
        val r = size.minDimension * 0.36f
        // concentric arcs (echo)
        drawArc(brush = grad, startAngle = 220f, sweepAngle = 80f, useCenter = false,
            topLeft = Offset(c.x - r, c.y - r), size = androidx.compose.ui.geometry.Size(r*2, r*2),
            style = Stroke(width = r * 0.14f, cap = StrokeCap.Round))
        drawArc(brush = grad, startAngle = 220f, sweepAngle = 80f, useCenter = false,
            topLeft = Offset(c.x - r*0.68f, c.y - r*0.68f), size = androidx.compose.ui.geometry.Size(r*1.36f, r*1.36f),
            style = Stroke(width = r * 0.12f, cap = StrokeCap.Round))
        // link motif
        val linkR = r * 0.38f
        drawArc(brush = grad, startAngle = 45f, sweepAngle = 270f, useCenter = false,
            topLeft = Offset(c.x - linkR, c.y - linkR), size = androidx.compose.ui.geometry.Size(linkR*2, linkR*2),
            style = Stroke(width = r * 0.18f, cap = StrokeCap.Round))
    }
}
