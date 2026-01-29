package eu.kanade.tachiyomi.ui.player

import kotlinx.coroutines.flow.MutableStateFlow

object PlayerStats {
    val estimatedVfFps = MutableStateFlow(0.0)
    val isInterpolating = MutableStateFlow(false)
    val videoParamsFps = MutableStateFlow(0.0)
    val videoOutParamsFps = MutableStateFlow(0.0)
    val containerFps = MutableStateFlow(0.0)
    val displayFps = MutableStateFlow(0.0)
    val estimatedDisplayFps = MutableStateFlow(0.0)
    val videoSync = MutableStateFlow("")
    val tscale = MutableStateFlow("")
    val videoW = MutableStateFlow(0L)
    val videoH = MutableStateFlow(0L)
    val videoCodec = MutableStateFlow("")
    val videoBitrate = MutableStateFlow(0L)
    val videoPixFmt = MutableStateFlow("")
    val videoLevels = MutableStateFlow("")
    val videoPrimaries = MutableStateFlow("")

    fun reset() {
        estimatedVfFps.value = 0.0
        isInterpolating.value = false
        videoParamsFps.value = 0.0
        videoW.value = 0L
        videoH.value = 0L
        videoCodec.value = ""
        videoBitrate.value = 0L
        videoPixFmt.value = ""
        videoLevels.value = ""
        videoPrimaries.value = ""
    }
}
