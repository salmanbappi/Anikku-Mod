package eu.kanade.tachiyomi.ui.player

import kotlinx.coroutines.flow.MutableStateFlow

object PlayerStats {
    val estimatedVfFps = MutableStateFlow(0.0)
    val isInterpolating = MutableStateFlow(false)
    val videoParamsFps = MutableStateFlow(0.0)
    val videoW = MutableStateFlow(0L)
    val videoH = MutableStateFlow(0L)
    val videoCodec = MutableStateFlow("")
    val videoBitrate = MutableStateFlow(0L)

    fun reset() {
        estimatedVfFps.value = 0.0
        isInterpolating.value = false
        videoParamsFps.value = 0.0
        videoW.value = 0L
        videoH.value = 0L
        videoCodec.value = ""
        videoBitrate.value = 0L
    }
}
