package eu.kanade.tachiyomi.ui.stats

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.network.model.*
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import okhttp3.OkHttpClient
import okhttp3.Request
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.source.service.SourceHealthCache
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

class InfrastructureScreenModel(
    private val sourceManager: SourceManager = Injekt.get(),
    private val networkHelper: NetworkHelper = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
) : StateScreenModel<InfrastructureState>(InfrastructureState.Loading) {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val semaphore = Semaphore(5)

    init {
        runDiagnostics()
    }

    fun runDiagnostics() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        
        screenModelScope.launchIO {
            val disabledSourceIds = sourcePreferences.disabledSources().get()
            val sources = sourceManager.getOnlineSources()
                .filter { !it.isLocal() }
                .filter { it.id.toString() !in disabledSourceIds }
            
            // Initial state load
            val initialNodes = sources.map { source ->
                createPlaceholderNode(source)
            }
            
            mutableState.update { 
                InfrastructureState.Success(InfrastructureReport(initialNodes, generateEmptyMetrics(initialNodes.size), emptyList()))
            }

            // Run parallel probes
            val nodes = sources.map { source ->
                async {
                    semaphore.withPermit {
                        probeNode(source).also { finishedNode ->
                            updateNodeInState(finishedNode)
                            SourceHealthCache.updateStatus(source.id, finishedNode.status, finishedNode.network.latency)
                        }
                    }
                }
            }.awaitAll()

            // Final state with full metrics and ranking
            val sortedNodes = nodes.sortedWith(compareByDescending<SourceNode> { it.status == NodeStatus.OPERATIONAL }
                .thenBy { it.network.latency })

            val logs = nodes.filter { it.status != NodeStatus.OPERATIONAL }.map { node ->
                SystemLogEntry(
                    timestamp = System.currentTimeMillis(),
                    level = if (node.status == NodeStatus.OFFLINE) LogLevel.ERROR else LogLevel.WARN,
                    source = node.name,
                    message = "Alert: ${node.status}. Response: ${node.network.latency}ms"
                )
            }

            val metrics = GlobalNetworkMetrics(
                bdixSaturation = if (nodes.isNotEmpty()) (nodes.count { it.network.topology == "BDIX" }.toDouble() / nodes.size * 100).toInt() else 0,
                totalDataConsumed = 0,
                avgLatency = if (nodes.isNotEmpty()) nodes.filter { it.status == NodeStatus.OPERATIONAL }.map { it.network.latency }.average().toInt() else 0,
                activeNodeCount = nodes.count { it.status == NodeStatus.OPERATIONAL }
            )

            mutableState.update {
                InfrastructureState.Success(InfrastructureReport(sortedNodes, metrics, logs))
            }
            _isRefreshing.value = false
        }
    }

    private fun createPlaceholderNode(source: HttpSource) = SourceNode(
        name = source.name,
        pkgName = source::class.java.`package`?.name ?: "ext",
        version = "...",
        status = NodeStatus.OPERATIONAL,
        network = NetworkDiagnostics(0, "Scanning", "...", "...", false),
        capabilities = SourceCapabilities(false, false, source.supportsLatest, true),
        uptimeScore = 1.0
    )

    private fun updateNodeInState(node: SourceNode) {
        mutableState.update { state ->
            if (state is InfrastructureState.Success) {
                val updatedNodes = state.report.nodes.map { if (it.name == node.name) node else it }
                state.copy(report = state.report.copy(nodes = updatedNodes))
            } else state
        }
    }

    private fun generateEmptyMetrics(count: Int) = GlobalNetworkMetrics(0, 0, 0, count)

    private suspend fun probeNode(source: HttpSource): SourceNode {
        var latency = 999
        var resolved = false
        var ip = "Offline"
        var tls = "Unknown"
        var status = NodeStatus.OFFLINE

        try {
            val probeClient = source.client.newBuilder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()

            // Try standard request with headers to avoid being blocked
            val request = Request.Builder()
                .url(source.baseUrl)
                .headers(source.headers)
                .header("X-Anikku-Probe", "CommandCenter-v2")
                .build()

            measureTimeMillis {
                probeClient.newCall(request).execute().use { response ->
                    resolved = true
                    tls = response.handshake?.tlsVersion?.javaName ?: "TLS v1.3"
                    if (response.isSuccessful || response.code < 500) {
                        status = NodeStatus.OPERATIONAL
                    } else {
                        status = NodeStatus.DEGRADED
                    }
                }
            }.let { latency = it.toInt() }

            val domain = source.baseUrl.substringAfter("://").substringBefore("/")
            ip = InetAddress.getByName(domain).hostAddress ?: "DNS Fail"

        } catch (e: Exception) {
            status = NodeStatus.OFFLINE
        }

        val name = source.name.lowercase()
        val isBdix = name.contains("dflix") || name.contains("dhaka") || name.contains("bdix") || 
                     name.contains("ftp") || name.contains("sam") || name.contains("bijoy") ||
                     name.contains("icc") || name.contains("fanush") || name.contains("nagordola")

        // Enhanced API detection: Audit class names and typical API server patterns
        val className = source::class.java.simpleName
        val isApi = className.contains("Api", ignoreCase = true) || 
                    className.contains("Json", ignoreCase = true) ||
                    name.contains("api") || name.contains("json")

        return SourceNode(
            name = source.name,
            pkgName = source::class.java.name.substringBeforeLast("."),
            version = "Node Probed",
            status = if (status == NodeStatus.OPERATIONAL && latency > 2000) NodeStatus.DEGRADED else status,
            network = NetworkDiagnostics(
                latency = latency,
                topology = if (isBdix) "BDIX" else "Global CDN",
                ipAddress = ip,
                tlsVersion = tls,
                dnsResolved = resolved
            ),
            capabilities = SourceCapabilities(
                isApi = isApi,
                mtSupport = false, // Requested to remove MT
                latestSupport = source.supportsLatest,
                searchSupport = true
            ),
            uptimeScore = if (status == NodeStatus.OPERATIONAL) 1.0 else 0.0
        )
    }
}

sealed interface InfrastructureState {
    data object Loading : InfrastructureState
    data class Success(val report: InfrastructureReport) : InfrastructureState
}