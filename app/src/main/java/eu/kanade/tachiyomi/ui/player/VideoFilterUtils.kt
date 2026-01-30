/*
 * Copyright 2024 Abdallah Mehiz
 * https://github.com/abdallahmehiz/mpvKt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kanade.tachiyomi.ui.player

import eu.kanade.tachiyomi.ui.player.settings.DecoderPreferences
import eu.kanade.tachiyomi.ui.player.utils.Anime4KManager
import `is`.xyz.mpv.MPVLib
import logcat.LogPriority
import logcat.logcat

fun applyFilter(filter: VideoFilters, value: Int, prefs: DecoderPreferences) {
    val property = filter.mpvProperty
    
    // Update copy mode preference based on all filters
    checkAndSetCopyMode(prefs)

    when (property) {
        "vf_sharpen", "vf_blur" -> {
            MPVLib.setPropertyString("vf", buildVFChain(prefs))
        }
        "saturation" -> {
            // saturation -100 is grayscale in mpv
            MPVLib.setPropertyInt("saturation", value)
        }
        else -> MPVLib.setPropertyInt(property, value)
    }
}

fun applyDebandMode(mode: Debanding, prefs: DecoderPreferences) {
    checkAndSetCopyMode(prefs)
    
    when (mode) {
        Debanding.None -> {
            MPVLib.setPropertyBoolean("deband", false)
            MPVLib.setPropertyString("vf", buildVFChain(prefs))
        }
        Debanding.CPU -> {
            MPVLib.setPropertyBoolean("deband", false)
            MPVLib.setPropertyString("vf", buildVFChain(prefs))
        }
        Debanding.GPU -> {
            MPVLib.setPropertyBoolean("deband", true)
            MPVLib.setPropertyString("vf", buildVFChain(prefs))
            // Apply current GPU settings
            DebandSettings.entries.forEach {
                MPVLib.setPropertyInt(it.mpvProperty, it.preference(prefs).get())
            }
        }
    }
}

fun applyDebandSetting(setting: DebandSettings, value: Int) {
    MPVLib.setPropertyInt(setting.mpvProperty, value)
}

fun buildVFChain(decoderPreferences: DecoderPreferences): String {
    val vfList = mutableListOf<String>()

    val sharpen = decoderPreferences.sharpenFilter().get()
    val blur = decoderPreferences.blurFilter().get()
    val deband = decoderPreferences.videoDebanding().get()

    if (deband == Debanding.CPU) {
        // Use a more modern deband filter for CPU if available, falling back to gradfun
        // lavfi's deband is better than gradfun
        vfList.add("lavfi=[deband=1:1:64:16]")
    }

    if (sharpen > 0) {
        val amount = (sharpen / 100f) * 1.5f
        vfList.add("unsharp=5:5:$amount:5:5:0")
    }

    if (blur > 0) {
        val size = (blur / 10f).toInt().coerceAtLeast(1)
        vfList.add("avgblur=size=$size")
    }

    return vfList.joinToString(",")
}

fun checkAndSetCopyMode(prefs: DecoderPreferences) {
    val requiresCopyMode = 
        prefs.sharpenFilter().get() > 0 ||
        prefs.blurFilter().get() > 0 ||
        prefs.videoDebanding().get() == Debanding.CPU ||
        prefs.smoothMotion().get()

    if (requiresCopyMode) {
        if (!prefs.forceMediaCodecCopy().get()) {
            prefs.forceMediaCodecCopy().set(true)
        }
        MPVLib.setPropertyString("hwdec", "mediacodec-copy")
    } else {
        if (prefs.forceMediaCodecCopy().get()) {
             prefs.forceMediaCodecCopy().set(false)
             MPVLib.setPropertyString("hwdec", "auto")
        }
    }
}

fun applyAnime4K(prefs: DecoderPreferences, manager: Anime4KManager, isInit: Boolean = false) {
    val enabled = prefs.enableAnime4K().get()
    
    // DEFENSIVE: Anime4K is incompatible with gpu-next in current builds
    val gpuNext = prefs.gpuNext().get()
    if (enabled && gpuNext) {
        logcat(LogPriority.WARN) { "Anime4K is incompatible with gpu-next. Skipping." }
        if (!isInit) MPVLib.setPropertyString("glsl-shaders", "")
        return
    }

    val mode = try {
        Anime4KManager.Mode.valueOf(prefs.anime4kMode().get())
    } catch (e: Exception) {
        Anime4KManager.Mode.OFF
    }
    val quality = try {
        Anime4KManager.Quality.valueOf(prefs.anime4kQuality().get())
    } catch (e: Exception) {
        Anime4KManager.Quality.BALANCED
    }

    // Ensure initialization happened
    manager.initialize()

    val chain = if (enabled) manager.getShaderChain(mode, quality) else ""
    logcat(LogPriority.DEBUG) { "Applying Anime4K chain (enabled=$enabled): $chain" }
    
    if (chain.isNotEmpty()) {
        // Optimized settings for GLSL shaders found in mpvEx
        if (isInit) {
            MPVLib.setOptionString("opengl-pbo", "yes")
            MPVLib.setOptionString("vd-lavc-dr", "yes")
            MPVLib.setOptionString("opengl-early-flush", "no")
            MPVLib.setOptionString("glsl-shaders", chain)
        } else {
            MPVLib.setPropertyString("glsl-shaders", chain)
        }
    } else {
        if (isInit) {
            MPVLib.setOptionString("glsl-shaders", "")
        } else {
            MPVLib.setPropertyString("glsl-shaders", "")
        }
    }
}