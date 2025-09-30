package com.anand.echolink.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Colorful brand-y scheme; dark uses true black but high-contrast onSurface
private val LightScheme = lightColorScheme(
    primary = Color(0xFF5E7BFF),
    onPrimary = Color.White,
    secondary = Color(0xFF00D1B2),
    onSecondary = Color(0xFF00201A),
    tertiary = Color(0xFFFF7AB6),
    onTertiary = Color(0xFF380016),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF121418),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1F),
    surfaceVariant = Color(0xFFE6E9F2),
    onSurfaceVariant = Color(0xFF444B59),
    outline = Color(0xFFCBD3E3),
    error = Color(0xFFEF5350),
    onError = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF7EA2FF),
    onPrimary = Color(0xFF001538),
    secondary = Color(0xFF58F5D8),
    onSecondary = Color(0xFF001F19),
    tertiary = Color(0xFFFFA7CF),
    onTertiary = Color(0xFF380016),
    background = Color(0xFF000000),  // true black
    onBackground = Color(0xFFEDEFF3),
    surface = Color(0xFF0B0D11),
    onSurface = Color(0xFFEDEFF3),
    surfaceVariant = Color(0xFF141821),
    onSurfaceVariant = Color(0xFFBAC2D6),
    outline = Color(0xFF2C3340),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2B0000)
)

@Composable
fun EchoLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    val scheme =
        if (darkTheme) {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                dynamicDarkColorScheme(ctx)
            else DarkScheme
        } else {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                dynamicLightColorScheme(ctx)
            else LightScheme
        }

    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
