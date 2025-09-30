// file: ui/legal/AttributionsScreen.kt
package com.anand.echolink.ui.legal

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.anand.echolink.R
import java.time.Year

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttributionsScreen(
    onBack: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attributions & Copyrights") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.Info, contentDescription = "Back")
                    }
                },
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
                .padding(insets)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header with Logo and Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.echo_logo),
                        contentDescription = "EchoLink Logo",
                        modifier = Modifier.size(72.dp).padding(8.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "📡 EchoLink – Turn Your Phone Into a Wireless Speaker",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "© ${Year.now().value} EchoLink. All rights reserved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            // Short blurb
            Text(
                "EchoLink is an Android app (Jetpack Compose + MVVM) that lets one device Host and other devices Join to share and play live audio over Wi-Fi or hotspot — like turning phones into wireless speakers, with multiple receivers at once.",
                style = MaterialTheme.typography.bodyMedium
            )

            Divider()

            // Features
            SectionHeader("✨ Features")
            Bulleted("🎵 Live Audio Streaming\nHost captures playback audio (YouTube Music, Spotify, local files) via MediaProjection + AudioPlaybackCapture.")
            Bulleted("📲 Multiple Receivers\nAny number of nearby devices can Join and play in sync.")
            Bulleted("📡 Wi-Fi / Hotspot Based\nNo internet needed; local network using UDP multicast + service discovery.")
            Bulleted("🔔 Connection Management\nListeners panel (latency/RTT, per-device toggle), notifications on connect, clean goodbyes.")
            Bulleted("🎨 Modern UI\nCompose + Material 3, role picker, reactive visualizer, gradients, dark mode.")
            Bulleted("🔒 Permissions Handled\nRecord Audio, Media Projection, Notifications (Android 13+).")

            // Technical Details
            SectionHeaderIcon("🛠️ Technical Details", Icons.Outlined.SettingsEthernet)
            SubHeader("Architecture")
            Bullet("MVVM with ViewModel, StateFlow, UseCases")
            SubHeader("UI")
            Bullet("Jetpack Compose, Material 3, custom animations (ReactiveVisualizer)")
            SubHeader("Networking")
            Bullet("UDP sockets for low-latency streaming")
            Bullet("NSD (Network Service Discovery) for finding hosts")
            Bullet("Simple control protocol: HELLO / GOODBYE / PING / PONG")
            SubHeader("Audio Pipeline")
            Bullet("Host: AudioRecord (AudioPlaybackCaptureConfig) → MediaCodec AAC encoder → UDP")
            Bullet("Receiver: UDP → jitter buffer → MediaCodec AAC decoder → AudioTrack")

            // Screens
            SectionHeader("📱 Screens")
            Bullet("Role Picker — choose Host or Join")
            Bullet("Host — start/stop hosting, listeners panel, connection notifications")
            Bullet("Receiver — join host and play audio, animated visualizer")

            // Roadmap
            SectionHeader("🚀 Roadmap / Ideas")
            Bullet("Volume sync across devices")
            Bullet("Fine-tuned jitter buffer for even lower latency")
            Bullet("LAN discovery fallback without NSD")
            Bullet("Background play + Quick Settings tile")

            // Notes
            SectionHeader("⚠️ Notes")
            Bullet("Requires Android 10+ (AudioPlaybackCapture API).")
            Bullet("Some OEMs block capture for DRM-protected apps (e.g., Netflix).")
            Bullet("Works best on strong local Wi-Fi / hotspot.")

            Divider()

            // Copyrights & Licenses
            SectionHeaderIcon("Copyrights & Licenses", Icons.Outlined.Policy)
            Text(
                "Branding & Logo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Logo and branding were created by the EchoLink team. Ideation assisted by AI tools (e.g., ChatGPT). Final assets are original and owned by EchoLink.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                "Open-Source Components",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            BulletLink("Jetpack Compose (AndroidX) — Apache License 2.0", "https://developer.android.com/jetpack/compose")
            BulletLink("Material 3 — Apache License 2.0", "https://m3.material.io/")
            BulletLink("Media3 — Apache License 2.0", "https://developer.android.com/guide/topics/media/media3")
            BulletLink("Material Icons / Symbols — Apache License 2.0", "https://fonts.google.com/icons")

            Text(
                "AI Assistance Disclosure",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Bullet("Some copy and non-code content drafted with assistance from ChatGPT and reviewed by humans.")

            Divider()

            // Contact / Policy / Source
            SectionHeaderIcon("Links", Icons.Outlined.Public)
            LinkLine("Project Homepage / Repository", "https://example.com/echolink") // TODO replace
            LinkLine("Privacy Policy (if applicable)", "https://example.com/echolink/privacy") // TODO replace
            LinkLine("Issue Reports / Contact", "mailto:support@example.com") // TODO replace

            Spacer(Modifier.height(8.dp))
            Text(
                "If you believe any asset has been used in error, please contact us for prompt correction.",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

/* ---------- Small UI helpers ---------- */

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SectionHeaderIcon(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        SectionHeader(text)
    }
}

@Composable
private fun SubHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun Bullet(text: String) {
    Text("• $text", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Bulleted(multiline: String) {
    // For convenience: splits on newline but renders as one bullet block visually
    Text("• $multiline", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun LinkLine(label: String, url: String) {
    val uri = LocalUriHandler.current
    TextButton(onClick = { uri.openUri(url) }) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(label) }
            }
        )
    }
}

@Composable
private fun BulletLink(label: String, url: String) {
    val uri = LocalUriHandler.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("• ", style = MaterialTheme.typography.bodyMedium)
        TextButton(
            onClick = { uri.openUri(url) },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(label) }
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
