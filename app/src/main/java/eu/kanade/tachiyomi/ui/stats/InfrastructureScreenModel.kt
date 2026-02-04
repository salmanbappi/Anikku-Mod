package eu.kanade.tachiyomi.ui.stats

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.presentation.more.stats.data.*
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.Request
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.net.InetAddress
import kotlin.system.measureTimeMillis

class InfrastructureScreenModel(
    private val sourceManager: SourceManager = Injekt.get(),
    private val networkHelper: NetworkHelper = Injekt.get(),
) : StateScreenModel<InfrastructureState>(InfrastructureState.Loading) {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        runDiagnostics()
    }

    fun runDiagnostics() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        
        screenModelScope.launchIO {
            val sources = sourceManager.getOnlineSources()
            val nodes = sources.map { source ->
                probeNode(source)
            }

            val logs = nodes.filter { it.status != NodeStatus.OPERATIONAL }.map { node ->
                SystemLogEntry(
                    timestamp = System.currentTimeMillis(),
                    level = if (node.status == NodeStatus.OFFLINE) LogLevel.ERROR else LogLevel.WARN,
                    source = node.name,
                    message = "Node status identified as ${node.status}. Latency: ${node.network.latency}ms"
                )
            }

            val metrics = GlobalNetworkMetrics(
                bdixSaturation = (nodes.count { it.network.topology == "BDIX" }.toDouble() / nodes.size * 100).toInt(),
                totalDataConsumed = 0, // Placeholder
                avgLatency = nodes.map { it.network.latency }.average().toInt(),
                activeNodeCount = nodes.count { it.status == NodeStatus.OPERATIONAL }
            )

            mutableState.update {
                InfrastructureState.Success(
                    InfrastructureReport(nodes, metrics, logs)
                )
            }
            _isRefreshing.value = false
        }
    }

    private suspend fun probeNode(source: HttpSource): SourceNode {
        var latency = 999
        var resolved = false
        var ip = "0.0.0.0"
        var tls = "Unknown"
        var status = NodeStatus.OFFLINE

        try {
            val request = Request.Builder()
                .url(source.baseUrl)
                .header("User-Agent", "Anikku-Infrastructure-Probe/1.0")
                .build()

            measureTimeMillis {
                networkHelper.client.newCall(request).execute().use { response ->
                    latency = 0 // Will be set by time elapsed
                    resolved = true
                    tls = response.handshake?.tlsVersion?.javaName ?: "TLS v1.2"
                    if (response.isSuccessful) status = NodeStatus.OPERATIONAL
                }
            }.let { latency = it.toInt() }

            // IP Heuristic
            val domain = source.baseUrl.substringAfter("://").substringBefore("/")
            ip = InetAddress.getByName(domain).hostAddress ?: "0.0.0.0"

        } catch (e: Exception) {
            status = NodeStatus.OFFLINE
        }

        val name = source.name.lowercase()
        val isBdix = name.contains("dflix") || name.contains("dhaka") || name.contains("bdix") || 
                     name.contains("ftp") || name.contains("sam") || name.contains("bijoy") ||
                     name.contains("icc") || name.contains("fanush")

        return SourceNode(
            name = source.name,
            pkgName = source::class.java.`package`?.name ?: "external.extension",
            version = "1.0",
            status = if (status == NodeStatus.OPERATIONAL && latency > 1000) NodeStatus.DEGRADED else status,
            network = NetworkDiagnostics(
                latency = latency,
                topology = if (isBdix) "BDIX" else "Global CDN",
                ipAddress = ip,
                tlsVersion = tls,
                dnsResolved = resolved
            ),
            capabilities = SourceCapabilities(
                isApi = name.contains("api") || name.contains("json") || source::class.java.simpleName.contains("Api"),
                mtSupport = true, // Anikku Engine capability
                latestSupport = source.supportsLatest,
                searchSupport = true
            ),
            uptimeScore = if (status == NodeStatus.OPERATIONAL) 0.98 else 0.0
        )
    }
}

sealed interface InfrastructureState {
    data object Loading : InfrastructureState
    data class Success(val report: InfrastructureReport) : InfrastructureState
}
