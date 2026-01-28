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

fun applyFilter(filter: VideoFilters, value: Int, prefs: DecoderPreferences) {
    val property = filter.mpvProperty
    when (property) {
        "vf_sharpen", "vf_blur" -> {
            // These require rebuilding the full VF chain
            MPVLib.setPropertyString("vf", buildVFChain(prefs))
        }
        "deband-iterations" -> {
            if (value == 0) {
                MPVLib.setPropertyBoolean("deband", false)
            } else {
                MPVLib.setPropertyBoolean("deband", true)
                MPVLib.setPropertyInt("deband-iterations", value)
            }
        }
        "deband-grain" -> {
            if (value > 0) MPVLib.setPropertyBoolean("deband", true)
            MPVLib.setPropertyInt("deband-grain", value)
        }
        else -> MPVLib.setPropertyInt(property, value)
    }
}

fun buildVFChain(decoderPreferences: DecoderPreferences): String {
    val vfList = mutableListOf<String>()

    if (decoderPreferences.useYUV420P().get()) {
        vfList.add("format=yuv420p")
    }

    when (decoderPreferences.videoDebanding().get()) {
        Debanding.CPU -> vfList.add("gradfun=radius=12")
        else -> {}
    }

    val sharpen = decoderPreferences.sharpenFilter().get()
    if (sharpen > 0) {
        val amount = sharpen / 50f
        vfList.add("unsharp=5:5:$amount:5:5:0")
    }

    val blur = decoderPreferences.blurFilter().get()
    if (blur > 0) {
        val amount = blur / 10f
        vfList.add("boxblur=$amount:1")
    }

    return vfList.joinToString(",")
}

fun applyTheme(theme: VideoFilterTheme, prefs: DecoderPreferences) {
    prefs.brightnessFilter().set(theme.brightness)
    prefs.contrastFilter().set(theme.contrast)
    prefs.saturationFilter().set(theme.saturation)
    prefs.gammaFilter().set(theme.gamma)
    prefs.hueFilter().set(theme.hue)
    prefs.sharpenFilter().set(theme.sharpen)
    prefs.blurFilter().set(0)
    prefs.debandFilter().set(0)
    prefs.grainFilter().set(0)

    // Apply direct properties
    MPVLib.setPropertyInt("brightness", theme.brightness)
    MPVLib.setPropertyInt("contrast", theme.contrast)
    MPVLib.setPropertyInt("saturation", theme.saturation)
    MPVLib.setPropertyInt("gamma", theme.gamma)
    MPVLib.setPropertyInt("hue", theme.hue)
    
    // Apply VF chain once
    MPVLib.setPropertyString("vf", buildVFChain(prefs))
    
    // Reset deband
    MPVLib.setPropertyBoolean("deband", false)
}

fun applyAnime4K(prefs: DecoderPreferences, manager: Anime4KManager, isInit: Boolean = false) {
    val enabled = prefs.enableAnime4K().get()
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

    val chain = if (enabled) manager.getShaderChain(mode, quality) else ""
    if (isInit) {
        MPVLib.setOptionString("glsl-shaders", chain)
    } else {
        MPVLib.setPropertyString("glsl-shaders", chain)
    }
}

