package eu.kanade.tachiyomi.util.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object CoverColorObserver {
    private val _vibrantColors = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val vibrantColors = _vibrantColors.asStateFlow()

    private val _ratios = MutableStateFlow<Map<Long, Float>>(emptyMap())
    val ratios = _ratios.asStateFlow()

    fun update(animeId: Long, color: Int) {
        _vibrantColors.update { it + (animeId to color) }
    }

    fun updateRatio(animeId: Long, ratio: Float) {
        _ratios.update { it + (animeId to ratio) }
    }

    fun get(animeId: Long): Int? {
        return _vibrantColors.value[animeId]
    }

    fun getRatio(animeId: Long): Float? {
        return _ratios.value[animeId]
    }
}
