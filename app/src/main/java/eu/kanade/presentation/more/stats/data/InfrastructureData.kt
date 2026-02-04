package eu.kanade.presentation.more.stats.data

import androidx.compose.runtime.Immutable

@Immutable
data class InfrastructureReport(
    val nodes: List<SourceNode>,
    val globalMetrics: GlobalNetworkMetrics,
    val systemLogs: List<SystemLogEntry>,
)

@Immutable
data class SourceNode(
    val name: String,
    val pkgName: String,
    val version: String,
    val status: NodeStatus,
    val network: NetworkDiagnostics,
    val capabilities: SourceCapabilities,
    val uptimeScore: Double, // 0.0 to 1.0
)

enum class NodeStatus {
    OPERATIONAL, DEGRADED, CRITICAL, OFFLINE
}

@Immutable
data class NetworkDiagnostics(
    val latency: Int, // ms
    val topology: String, // BDIX, PEER, Global CDN
    val ipAddress: String,
    val tlsVersion: String,
    val dnsResolved: Boolean,
)

@Immutable
data class SourceCapabilities(
    val isApi: Boolean,
    val mtSupport: Boolean, // Multi-threaded protocol support
    val latestSupport: Boolean,
    val searchSupport: Boolean,
)

@Immutable
data class GlobalNetworkMetrics(
    val bdixSaturation: Int, // %
    val totalDataConsumed: Long, // bytes
    val avgLatency: Int,
    val activeNodeCount: Int,
)

@Immutable
data class SystemLogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val source: String,
    val message: String,
)

enum class LogLevel {
    INFO, WARN, ERROR
}

@Immutable
data class ExtensionHealth(
    val name: String,
    val isOnline: Boolean,
    val latency: Int,
    val type: String, // BDIX, API, SCRAPE
    val issue: String? = null,
)
