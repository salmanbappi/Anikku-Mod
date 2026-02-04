package eu.kanade.presentation.more.stats

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.network.model.*
import eu.kanade.tachiyomi.ui.stats.InfrastructureState
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun InfrastructureScreen(
    state: InfrastructureState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
) {
    if (state is InfrastructureState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val report = (state as InfrastructureState.Success).report

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        item {
            GlobalTelemetryHeader(report.globalMetrics)
        }

        item {
            InfrastructureHealthBoard(report.nodes)
        }

        item {
            Text(
                text = "SYSTEM TELEMETRY LOGS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        items(report.systemLogs.take(5)) { log ->
            LogEntryRow(log)
        }

        item {
            Text(
                text = "NODE CAPABILITY AUDIT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        items(report.nodes) { node ->
            SourceNodeAuditCard(node)
        }
    }
}

@Composable
private fun GlobalTelemetryHeader(metrics: GlobalNetworkMetrics) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Analytics, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("NETWORK COMMAND CENTER", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricSquare("BDIX SAT", "${metrics.bdixSaturation}%", Color(0xFF1E88E5))
                MetricSquare("ACTIVE", "${metrics.activeNodeCount}", Color(0xFF4CAF50))
                MetricSquare("LATENCY", "${metrics.avgLatency}ms", Color(0xFFFFC107))
            }
        }
    }
}

@Composable
private fun MetricSquare(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = color,
            fontFamily = FontFamily.Monospace
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.secondaryItemAlpha())
    }
}

@Composable
private fun InfrastructureHealthBoard(nodes: List<SourceNode>) {
    Column {
        Text(
            text = "ENDPOINT HEALTH PULSE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            nodes.forEach { node ->
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (node.status) {
                                NodeStatus.OPERATIONAL -> Color(0xFF4CAF50)
                                NodeStatus.DEGRADED -> Color(0xFFFFC107)
                                NodeStatus.CRITICAL -> Color(0xFFFF9800)
                                NodeStatus.OFFLINE -> Color(0xFFF44336)
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun LogEntryRow(log: SystemLogEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "[${log.level.name}]",
            color = when(log.level) {
                LogLevel.ERROR -> Color.Red
                LogLevel.WARN -> Color.Yellow
                else -> Color.Green
            },
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = log.source, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(text = log.message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.secondaryItemAlpha())
        }
    }
}

@Composable
private fun SourceNodeAuditCard(node: SourceNode) {
    var expanded by remember { mutableStateOf(false) }
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusIndicator(node.status)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = node.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${node.network.topology} • ${node.network.ipAddress}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.secondaryItemAlpha(),
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "${node.network.latency}ms",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (node.network.latency < 100) Color(0xFF4CAF50) else Color.Unspecified
                )
            }
            
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp).alpha(0.2f))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CapabilityBadge("API", node.capabilities.isApi)
                    CapabilityBadge("MT", node.capabilities.mtSupport)
                    CapabilityBadge("SEARCH", node.capabilities.searchSupport)
                    CapabilityBadge("LATEST", node.capabilities.latestSupport)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Security, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Encrypted via ${node.network.tlsVersion}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.secondaryItemAlpha()
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIndicator(status: NodeStatus) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(
                when (status) {
                    NodeStatus.OPERATIONAL -> Color(0xFF4CAF50)
                    NodeStatus.DEGRADED -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                }
            )
    )
}

@Composable
private fun CapabilityBadge(label: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.alpha(if (active) 1f else 0.4f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
    }
}