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
    val delayedFrames = MutableStateFlow(0L)
    val mistime = MutableStateFlow(0.0)
    val voPasses = MutableStateFlow(0L)
    val videoW = MutableStateFlow(0L)
    val videoH = MutableStateFlow(0L)
    val videoOutW = MutableStateFlow(0L)
    val videoOutH = MutableStateFlow(0L)

    fun reset() {
        estimatedVfFps.value = 0.0
        isInterpolating.value = false
        videoParamsFps.value = 0.0
        containerFps.value = 0.0
        videoSync.value = ""
        tscale.value = ""
        delayedFrames.value = 0L
        mistime.value = 0.0
        voPasses.value = 0L
        videoW.value = 0L
        videoH.value = 0L
        videoOutW.value = 0L
        videoOutH.value = 0L
        videoCodec.value = ""
        videoBitrate.value = 0L
        videoPixFmt.value = ""
        videoLevels.value = ""
        videoPrimaries.value = ""
    }
}
