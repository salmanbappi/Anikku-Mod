package eu.kanade.tachiyomi.ui.player

import kotlinx.coroutines.flow.MutableStateFlow

object PlayerStats {
    val estimatedDisplayFps = MutableStateFlow(0.0)
    val isInterpolating = MutableStateFlow(false)
    val videoParamsFps = MutableStateFlow(0.0)

    fun reset() {
        estimatedDisplayFps.value = 0.0
        isInterpolating.value = false
        videoParamsFps.value = 0.0
    }
}
