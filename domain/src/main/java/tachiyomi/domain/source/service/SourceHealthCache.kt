package tachiyomi.domain.source.service

import eu.kanade.presentation.more.stats.data.NodeStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Global cache for source health status to ensure sync between Browse and Command Center.
 */
object SourceHealthCache {
    private val _healthMap = MutableStateFlow<Map<Long, NodeStatus>>(emptyMap())
    val healthMap: StateFlow<Map<Long, NodeStatus>> = _healthMap.asStateFlow()

    private val _latencyMap = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val latencyMap: StateFlow<Map<Long, Int>> = _latencyMap.asStateFlow()

    fun updateStatus(sourceId: Long, status: NodeStatus, latency: Int) {
        _healthMap.update { it + (sourceId to status) }
        _latencyMap.update { it + (sourceId to latency) }
    }

    fun getStatus(sourceId: Long): NodeStatus {
        return _healthMap.value[sourceId] ?: NodeStatus.OPERATIONAL // Default to healthy if unknown
    }
}
