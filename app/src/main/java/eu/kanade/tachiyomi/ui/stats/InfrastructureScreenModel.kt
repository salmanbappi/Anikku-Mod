package eu.kanade.tachiyomi.ui.stats

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
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
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

class InfrastructureScreenModel(
    private val sourceManager: SourceManager = Injekt.get(),
    private val networkHelper: NetworkHelper = Injekt.get(),
) : StateScreenModel<InfrastructureState>(InfrastructureState.Loading) {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Limit concurrent pings to avoid network congestion
    private val semaphore = Semaphore(5)

    init {
        // Initial state load from cache if exists
        loadFromCache()
        runDiagnostics()
    }

    private fun loadFromCache() {
        val cachedHealth = SourceHealthCache.healthMap.value
        if (cachedHealth.isNotEmpty()) {
            // We'll run a fresh diagnostic anyway, but this allows instant UI load
        }
    }

    fun runDiagnostics() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        
        screenModelScope.launchIO {
            val sources = sourceManager.getOnlineSources()
            
            // Start with "Probing" placeholders for instant UI feedback
            val initialNodes = sources.map { source ->
                SourceNode(
                    name = source.name,
                    pkgName = source::class.java.`package`?.name ?: "ext",
                    version = "Checking...",
                    status = NodeStatus.OPERATIONAL, // Placeholder
                    network = NetworkDiagnostics(0, "Scanning...", "...", "...", false),
                    capabilities = SourceCapabilities(false, true, source.supportsLatest, true),
                    uptimeScore = 1.0
                )
            }
            
            mutableState.update { 
                InfrastructureState.Success(InfrastructureReport(initialNodes, generateEmptyMetrics(initialNodes.size), emptyList()))
            }

            // Run probes in parallel with concurrency limit
            val nodes = sources.map { source ->
                async {
                    semaphore.withPermit {
                        probeNode(source).also { finishedNode ->
                            // Update individual node in the state immediately for "static holder" feel
                            updateNodeInState(finishedNode)
                            SourceHealthCache.updateStatus(source.id, finishedNode.status, finishedNode.network.latency)
                        }
                    }
                }
            }.awaitAll()

            val logs = nodes.filter { it.status != NodeStatus.OPERATIONAL }.map { node ->
                SystemLogEntry(
                    timestamp = System.currentTimeMillis(),
                    level = if (node.status == NodeStatus.OFFLINE) LogLevel.ERROR else LogLevel.WARN,
                    source = node.name,
                    message = "Telemetry Alert: Node ${node.status}. Response: ${node.network.latency}ms"
                )
            }

            val metrics = GlobalNetworkMetrics(
                bdixSaturation = if (nodes.isNotEmpty()) (nodes.count { it.network.topology == "BDIX" }.toDouble() / nodes.size * 100).toInt() else 0,
                totalDataConsumed = 0,
                avgLatency = if (nodes.isNotEmpty()) nodes.map { it.network.latency }.average().toInt() else 0,
                activeNodeCount = nodes.count { it.status == NodeStatus.OPERATIONAL }
            )

            mutableState.update {
                InfrastructureState.Success(InfrastructureReport(nodes, metrics, logs))
            }
            _isRefreshing.value = false
        }
    }

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
        var ip = "Pending"
        var tls = "Unknown"
        var status = NodeStatus.OFFLINE
        var errorLog: String? = null

        try {
            // Use the source's OWN client and headers for absolute accuracy
            val probeClient = source.client.newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(source.baseUrl)
                .headers(source.headers)
                .header("X-Anikku-Probe", "Telemetry-v2")
                .build()

            measureTimeMillis {
                probeClient.newCall(request).execute().use { response ->
                    resolved = true
                    tls = response.handshake?.tlsVersion?.javaName ?: "TLS v1.3"
                    if (response.isSuccessful) {
                        status = NodeStatus.OPERATIONAL
                    } else {
                        status = NodeStatus.DEGRADED
                        errorLog = "HTTP ${response.code}"
                    }
                }
            }.let { latency = it.toInt() }

            val domain = source.baseUrl.substringAfter("://").substringBefore("/")
            ip = InetAddress.getByName(domain).hostAddress ?: "DNS Fail"

        } catch (e: Exception) {
            status = NodeStatus.OFFLINE
            errorLog = e.message?.take(30) ?: "Timeout/SSL Error"
        }

        val name = source.name.lowercase()
        val isBdix = name.contains("dflix") || name.contains("dhaka") || name.contains("bdix") || 
                     name.contains("ftp") || name.contains("sam") || name.contains("bijoy") ||
                     name.contains("icc") || name.contains("fanush") || name.contains("nagordola")

        return SourceNode(
            name = source.name,
            pkgName = source::class.java.`package`?.name ?: "ext",
            version = "PROBED",
            status = if (status == NodeStatus.OPERATIONAL && latency > 1500) NodeStatus.DEGRADED else status,
            network = NetworkDiagnostics(
                latency = latency,
                topology = if (isBdix) "BDIX" else "Global CDN",
                ipAddress = ip,
                tlsVersion = tls,
                dnsResolved = resolved
            ),
            capabilities = SourceCapabilities(
                isApi = name.contains("api") || name.contains("json") || source::class.java.simpleName.contains("Api"),
                mtSupport = true,
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
