package com.anand.echolink.ui.sender

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anand.echolink.ui.host.UiListenerItem

/**
 * Panel showing connected listeners and latency.
 */
@Composable
fun ListenersPanel(
    items: List<UiListenerItem>,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp,
        color = cs.surface,
        contentColor = cs.onSurface
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Devices, contentDescription = null, tint = cs.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Listeners (${items.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(8.dp))
            if (items.isEmpty()) {
                Text("Waiting for devices to join…", color = cs.onSurface.copy(alpha = 0.7f))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items) { li ->
                        ListItem(
                            headlineContent = { Text(li.displayName) },
                            supportingContent = {
                                val rtt = li.rttMs?.let { "$it ms" } ?: "measuring…"
                                Text("Latency: $rtt")
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Outlined.SignalCellularAlt,
                                    contentDescription = null,
                                    tint = cs.secondary
                                )
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}
