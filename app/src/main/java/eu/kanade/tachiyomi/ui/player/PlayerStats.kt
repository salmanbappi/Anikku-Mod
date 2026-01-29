package eu.kanade.tachiyomi.ui.player

import kotlinx.coroutines.flow.MutableStateFlow

object PlayerStats {
    val estimatedVfFps = MutableStateFlow(0.0)
    val isInterpolating = MutableStateFlow(false)
    val videoParamsFps = MutableStateFlow(0.0)

    fun reset() {
        estimatedVfFps.value = 0.0
        isInterpolating.value = false
        videoParamsFps.value = 0.0
    }
}
