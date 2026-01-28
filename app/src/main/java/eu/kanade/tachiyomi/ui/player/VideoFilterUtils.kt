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
import `is`.xyz.mpv.MPVLib

fun applyFilter(filter: VideoFilters, value: Int) {
    val property = filter.mpvProperty
    when (property) {
        "vf_sharpen" -> {
            val amount = value / 50f
            if (amount == 0f) {
                MPVLib.command(arrayOf("vf", "remove", "@sharpen"))
            } else {
                MPVLib.command(arrayOf("vf", "add", "@sharpen:lavfi=[unsharp=5:5:$amount:5:5:0]"))
            }
        }
        "vf_blur" -> {
            val amount = value / 10f
            if (amount == 0f) {
                MPVLib.command(arrayOf("vf", "remove", "@blur"))
            } else {
                MPVLib.command(arrayOf("vf", "add", "@blur:lavfi=[boxblur=$amount:1]"))
            }
        }
        "deband-iterations" -> {
            if (value == 0) {
                MPVLib.setPropertyBoolean("deband", false)
            } else {
                MPVLib.setPropertyBoolean("deband", true)
                MPVLib.setPropertyInt("deband-iterations", value.toLong())
            }
        }
        "deband-grain" -> {
            if (value > 0) MPVLib.setPropertyBoolean("deband", true)
            MPVLib.setPropertyInt("deband-grain", value.toLong())
        }
        else -> MPVLib.setPropertyInt(property, value.toLong())
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
        vfList.add("@sharpen:lavfi=[unsharp=5:5:$amount:5:5:0]")
    }

    val blur = decoderPreferences.blurFilter().get()
    if (blur > 0) {
        val amount = blur / 10f
        vfList.add("@blur:lavfi=[boxblur=$amount:1]")
    }

    return vfList.joinToString(",")
}

fun applyTheme(theme: VideoFilterTheme, prefs: DecoderPreferences) {
    VideoFilters.BRIGHTNESS.let {
        prefs.brightnessFilter().set(theme.brightness)
        applyFilter(it, theme.brightness)
    }
    VideoFilters.CONTRAST.let {
        prefs.contrastFilter().set(theme.contrast)
        applyFilter(it, theme.contrast)
    }
    VideoFilters.SATURATION.let {
        prefs.saturationFilter().set(theme.saturation)
        applyFilter(it, theme.saturation)
    }
    VideoFilters.GAMMA.let {
        prefs.gammaFilter().set(theme.gamma)
        applyFilter(it, theme.gamma)
    }
    VideoFilters.HUE.let {
        prefs.hueFilter().set(theme.hue)
        applyFilter(it, theme.hue)
    }
    VideoFilters.SHARPEN.let {
        prefs.sharpenFilter().set(theme.sharpen)
        applyFilter(it, theme.sharpen)
    }
    VideoFilters.BLUR.let {
        prefs.blurFilter().set(0)
        applyFilter(it, 0)
    }
    VideoFilters.DEBAND.let {
        prefs.debandFilter().set(0)
        applyFilter(it, 0)
    }
    VideoFilters.GRAIN.let {
        prefs.grainFilter().set(0)
        applyFilter(it, 0)
    }
}
