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

fun applyDebandSetting(setting: DebandSettings, value: Int) {
    MPVLib.setPropertyInt(setting.mpvProperty, value)
}

fun buildVFChain(decoderPreferences: DecoderPreferences): String {
    val vfList = mutableListOf<String>()

    // Only add format if we actually need filters, to reduce overhead
    val sharpen = decoderPreferences.sharpenFilter().get()
    val blur = decoderPreferences.blurFilter().get()
    val deband = decoderPreferences.videoDebanding().get()

    if (sharpen > 0 || blur > 0 || deband == Debanding.CPU) {
        if (decoderPreferences.useYUV420P().get()) {
            vfList.add("format=yuv420p")
        }
    }

    if (deband == Debanding.CPU) {
        vfList.add("gradfun=radius=12")
    }

    if (sharpen > 0) {
        val amount = (sharpen / 100f) * 1.5f // Scaled for 5x5 matrix
        vfList.add("unsharp=5:5:$amount:5:5:0")
    }

    if (blur > 0) {
        val amount = blur / 20f
        vfList.add("boxblur=$amount:1")
    }

    return vfList.joinToString(",")
}

fun checkAndSetCopyMode(prefs: DecoderPreferences) {
    val anyFilterActive = 
        prefs.brightnessFilter().get() != 0 ||
        prefs.contrastFilter().get() != 0 ||
        prefs.saturationFilter().get() != 0 ||
        prefs.gammaFilter().get() != 0 ||
        prefs.hueFilter().get() != 0 ||
        prefs.sharpenFilter().get() != 0 ||
        prefs.blurFilter().get() != 0

    // Automatically enable copy mode if filters are active
    if (anyFilterActive) {
        if (!prefs.forceMediaCodecCopy().get()) {
            prefs.forceMediaCodecCopy().set(true)
        }
        MPVLib.setPropertyString("hwdec", "mediacodec-copy")
    } else {
        // If no filters are active, respect the manual force switch
        // If the user manually turned it ON, keep it ON.
        // But the user asked: "if I don't use that filter... it should turn off"
        // This implies if they reset filters, they want it OFF (unless they manually forced it *without* filters?)
        // Let's assume: If filters become 0, we turn it OFF.
        // User can still manually turn it ON via switch (which sets the pref).
        // But wait, if we set it to false here, we overwrite the manual switch if it was ON.
        
        // Scenario: User turns on Switch (Force=True). Filters are 0.
        // checkAndSetCopyMode runs (maybe triggered by something else).
        // It sees filters=0. Should it turn Force=False?
        // If so, the switch automatically turns off immediately after user turns it on? NO.
        
        // We only want to auto-manage if the user *changed* a filter.
        // But this function is called inside applyFilter.
        // If user drags slider to 0, this function runs.
        // We should probably allow Manual ON even if filters are 0.
        
        // But the user request: "if I don't use that filter... it should turn off"
        // This implies they want the "Auto" behavior to dominate.
        
        // Let's try this:
        // We update the PREFERENCE to match the filter state.
        // This makes the switch "follow" the filters.
        
        if (prefs.forceMediaCodecCopy().get()) {
             // It's currently ON.
             // If filters are 0, should we turn it OFF?
             // Only if we assume the user didn't want it ON for other reasons.
             // Given the complaint "why isn't it showing... if I don't use... it should turn off",
             // it seems they treat the switch as an indicator.
             prefs.forceMediaCodecCopy().set(false)
             MPVLib.setPropertyString("hwdec", "mediacodec")
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