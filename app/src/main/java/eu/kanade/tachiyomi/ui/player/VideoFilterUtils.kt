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
    val lavfiList = mutableListOf<String>()

    val sharpen = decoderPreferences.sharpenFilter().get()
    val blur = decoderPreferences.blurFilter().get()
    val deband = decoderPreferences.videoDebanding().get()

    // If any filter requires CPU processing, we MUST ensure a stable pixel format
    // to prevent green tint/alignment issues on the right side of the screen.
    // This now respects the user's preference setting.
    if (decoderPreferences.useYUV420P().get() && (deband == Debanding.CPU || sharpen > 0 || blur > 0)) {
        vfList.add("format=yuv420p")
    }

    if (deband == Debanding.CPU) {
        lavfiList.add("deband=1:1:64:16")
    }

    if (sharpen > 0) {
        val amount = (sharpen / 100f) * 1.5f
        lavfiList.add("unsharp=5:5:$amount:5:5:0")
    }

    if (blur > 0) {
        val luma = blur / 10f
        lavfiList.add("boxblur=$luma:1")
    }

    if (lavfiList.isNotEmpty()) {
        vfList.add("lavfi=[${lavfiList.joinToString(",")}]")
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

fun applyTheme(theme: VideoFilterTheme, prefs: DecoderPreferences) {
    prefs.brightnessFilter().set(theme.brightness)
    prefs.contrastFilter().set(theme.contrast)
    prefs.saturationFilter().set(theme.saturation)
    prefs.gammaFilter().set(theme.gamma)
    prefs.hueFilter().set(theme.hue)
    prefs.sharpenFilter().set(theme.sharpen)
    prefs.blurFilter().set(0)
    
    // Reset deband
    prefs.debandFilter().set(0)
    prefs.grainFilter().set(0)
    prefs.debandThreshold().set(32)
    prefs.debandRange().set(16)

    // Update copy mode based on new theme values
    checkAndSetCopyMode(prefs)

    // Apply direct properties
    MPVLib.setPropertyInt("brightness", theme.brightness)
    MPVLib.setPropertyInt("contrast", theme.contrast)
    MPVLib.setPropertyInt("saturation", theme.saturation)
    MPVLib.setPropertyInt("gamma", theme.gamma)
    MPVLib.setPropertyInt("hue", theme.hue)
    
    // Apply VF chain once
    MPVLib.setPropertyString("vf", buildVFChain(prefs))
    
    // Reset deband engine properties
    MPVLib.setPropertyBoolean("deband", false)
    MPVLib.setPropertyInt("deband-iterations", 1)
    MPVLib.setPropertyInt("deband-threshold", 32)
    MPVLib.setPropertyInt("deband-range", 16)
    MPVLib.setPropertyInt("deband-grain", 48)
}

fun applyAnime4K(prefs: DecoderPreferences, manager: Anime4KManager, isInit: Boolean = false) {
    val enabled = prefs.enableAnime4K().get()
    
    // DEFENSIVE: Anime4K is incompatible with gpu-next in current builds
    val gpuNext = prefs.gpuNext().get()
    if (enabled && gpuNext) {
        logcat("Anime4K", LogPriority.WARN) { "Anime4K is incompatible with gpu-next. Skipping." }
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
    logcat("Anime4K", LogPriority.DEBUG) { "Applying Anime4K chain (enabled=$enabled): $chain" }
    
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