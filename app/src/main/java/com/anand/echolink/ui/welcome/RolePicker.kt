// file: RolePicker.kt
package com.anand.echolink.ui.welcome

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anand.echolink.R
import com.anand.echolink.ui.common.GradientCard
import java.time.Year

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolePicker(
    onChooseSender: () -> Unit,
    onChooseReceiver: () -> Unit,
    onOpenAttributions: () -> Unit = {} // <— call to open the copyrights screen
) {
    val cs = MaterialTheme.colorScheme
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("EchoLink", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.surface,
                    titleContentColor = cs.onSurface
                ),
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.echo_logo),
                        contentDescription = "logo",
                        modifier = Modifier
                            .size(50.dp)
                            .padding(5.dp),
                    )
                },
                actions = {
                    IconButton(onClick = onOpenAttributions) {
                        Icon(Icons.Outlined.Info, contentDescription = "Attributions & Copyrights")
                    }
                }
            )
        }
    ) { insets ->
        Column(
            Modifier
                .fillMaxSize()
                .background(cs.background)
                .padding(insets)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Pick a role for this device",
                style = MaterialTheme.typography.titleMedium,
                color = cs.onBackground.copy(alpha = 0.85f)
            )

            // Sender card ...
            ElevatedCard(
                onClick = onChooseSender,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
            ) {
                GradientCard(
                    gradient = Brush.linearGradient(
                        colors = listOf(cs.primary.copy(alpha = 0.18f), Color.Transparent)
                    ),
                    borderColor = cs.outline.copy(alpha = 0.5f),
                    radius = 20.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Speaker,
                            contentDescription = null,
                            tint = cs.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Host", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Capture this phone’s audio and broadcast it.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurface.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(10.dp))
                            AssistChip(
                                onClick = onChooseSender,
                                label = { Text("Hotspot recommended") },
                                leadingIcon = { Icon(Icons.Outlined.WifiTethering, contentDescription = null) }
                            )
                        }
                        Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = cs.primary.copy(alpha = 0.8f))
                    }
                }
            }

            // Receiver card ...
            ElevatedCard(
                onClick = onChooseReceiver,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
            ) {
                GradientCard(
                    gradient = Brush.linearGradient(
                        colors = listOf(cs.tertiary.copy(alpha = 0.18f), Color.Transparent)
                    ),
                    borderColor = cs.outline.copy(alpha = 0.5f),
                    radius = 20.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Headphones,
                            contentDescription = null,
                            tint = cs.tertiary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Join", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Discover the sender and play the stream here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurface.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(10.dp))
                            AssistChip(
                                onClick = onChooseReceiver,
                                label = { Text("Use your speakers or headphones") },
                                leadingIcon = { Icon(Icons.Outlined.Headphones, contentDescription = null) }
                            )
                        }
                        Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = cs.tertiary.copy(alpha = 0.9f))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "You can switch roles anytime.",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onBackground.copy(alpha = 0.65f)
            )

            // --- Footer copyright ---
            Spacer(Modifier.height(24.dp))
            Divider()
            Text(
                text = "© ${Year.now().value} EchoLink • All rights reserved",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onBackground.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Attributions & Licenses",
                style = MaterialTheme.typography.labelLarge,
                color = cs.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable(onClick = onOpenAttributions)
            )
        }
    }
}
