package eu.kanade.tachiyomi.util.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object CoverColorObserver {
    private val _vibrantColors = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val vibrantColors = _vibrantColors.asStateFlow()

    fun update(animeId: Long, color: Int) {
        _vibrantColors.update { it + (animeId to color) }
    }

    fun get(animeId: Long): Int? {
        return _vibrantColors.value[animeId]
    }
}
