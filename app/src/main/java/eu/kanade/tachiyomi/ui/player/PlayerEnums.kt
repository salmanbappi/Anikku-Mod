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

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.player.settings.DecoderPreferences
import tachiyomi.core.common.preference.Preference
import tachiyomi.i18n.MR

/**
 * Results of the set as cover feature.
 */
enum class SetAsCover {
    Success,
    AddToLibraryFirst,
    Error,
}

enum class PlayerOrientation(val titleRes: StringResource) {
    Free(MR.strings.rotation_free),
    Video(MR.strings.rotation_video),
    Portrait(MR.strings.rotation_portrait),
    ReversePortrait(MR.strings.rotation_reverse_portrait),
    SensorPortrait(MR.strings.rotation_sensor_portrait),
    Landscape(MR.strings.rotation_landscape),
    ReverseLandscape(MR.strings.rotation_reverse_landscape),
    SensorLandscape(MR.strings.rotation_sensor_landscape),
}

enum class VideoAspect(val titleRes: StringResource) {
    Crop(MR.strings.video_crop_screen),
    Fit(MR.strings.video_fit_screen),
    Stretch(MR.strings.video_stretch_screen),
}

/**
 * Action performed by a button, like double tap or media controls
 */
enum class SingleActionGesture(val stringRes: StringResource) {
    None(stringRes = MR.strings.single_action_none),
    Seek(stringRes = MR.strings.single_action_seek),
    PlayPause(stringRes = MR.strings.single_action_playpause),
    Switch(stringRes = MR.strings.single_action_switch),
    Custom(stringRes = MR.strings.single_action_custom),
}

/**
 * Key codes sent through the `Custom` option in gestures
 */
enum class CustomKeyCodes(val keyCode: String) {
    DoubleTapLeft("0x10001"),
    DoubleTapCenter("0x10002"),
    DoubleTapRight("0x10003"),
    MediaPrevious("0x10004"),
    MediaPlay("0x10005"),
    MediaNext("0x10006"),
}

enum class Decoder(val title: String, val value: String) {
    AutoCopy("Auto", "auto-copy"),
    Auto("Auto", "auto"),
    SW("SW", "no"),
    HW("HW", "mediacodec-copy"),
    HWPlus("HW+", "mediacodec"),
}

fun getDecoderFromValue(value: String?): Decoder {
    if (value == null) return Decoder.Auto
    return Decoder.entries.firstOrNull { it.value == value } ?: Decoder.Auto
}

enum class Debanding {
    None,
    CPU,
    GPU,
}

enum class Sheets {
    None,
    PlaybackSpeed,
    SubtitleTracks,
    AudioTracks,
    QualityTracks,
    Chapters,
    More,
    Screenshot,
}

enum class Panels {
    None,
    SubtitleSettings,
    SubtitleDelay,
    AudioDelay,
    VideoFilters,
}

sealed class Dialogs {
    data object None : Dialogs()
    data object EpisodeList : Dialogs()
    data class IntegerPicker(
        val defaultValue: Int,
        val minValue: Int,
        val maxValue: Int,
        val step: Int,
        val nameFormat: String,
        val title: String,
        val onChange: (Int) -> Unit,
        val onDismissRequest: () -> Unit,
    ) : Dialogs()
}

sealed class PlayerUpdates {
    data object None : PlayerUpdates()
    data object DoubleSpeed : PlayerUpdates()
    data object AspectRatio : PlayerUpdates()
    data class ShowText(val value: String) : PlayerUpdates()
    data class ShowTextResource(val textResource: StringResource) : PlayerUpdates()
}

enum class DebandSettings(
    val titleRes: StringResource,
    val preference: (DecoderPreferences) -> Preference<Int>,
    val mpvProperty: String,
    val start: Int = 0,
    val end: Int = 100,
) {
    ITERATIONS(
        MR.strings.pref_debanding_title,
        { it.debandFilter() },
        "deband-iterations",
        start = 1,
        end = 4,
    ),
    THRESHOLD(
        MR.strings.player_sheets_deband_threshold,
        { it.debandThreshold() },
        "deband-threshold",
        start = 0,
        end = 100,
    ),
    RANGE(
        MR.strings.player_sheets_deband_range,
        { it.debandRange() },
        "deband-range",
        start = 0,
        end = 100,
    ),
    GRAIN(
        MR.strings.player_sheets_filters_grain,
        { it.grainFilter() },
        "deband-grain",
        start = 0,
        end = 100,
    ),
}

enum class VideoFilters(
    val titleRes: StringResource,
    val preference: (DecoderPreferences) -> Preference<Int>,
    val mpvProperty: String,
    val min: Int = -100,
    val max: Int = 100,
) {
    BRIGHTNESS(
        MR.strings.player_sheets_filters_brightness,
        { it.brightnessFilter() },
        "brightness",
    ),
    SATURATION(
        MR.strings.player_sheets_filters_Saturation,
        { it.saturationFilter() },
        "saturation",
    ),
    CONTRAST(
        MR.strings.player_sheets_filters_contrast,
        { it.contrastFilter() },
        "contrast",
    ),
    GAMMA(
        MR.strings.player_sheets_filters_gamma,
        { it.gammaFilter() },
        "gamma",
    ),
    HUE(
        MR.strings.player_sheets_filters_hue,
        { it.hueFilter() },
        "hue",
    ),
    SHARPEN(
        MR.strings.player_sheets_filters_sharpen,
        { it.sharpenFilter() },
        "vf_sharpen",
        min = 0,
        max = 100,
    ),
    BLUR(
        MR.strings.player_sheets_filters_blur,
        { it.blurFilter() },
        "vf_blur",
        min = 0,
        max = 100,
    ),
}

enum class VideoFilterTheme(
    val titleRes: StringResource,
    val description: String = "",
    val brightness: Int = 0,
    val contrast: Int = 0,
    val saturation: Int = 0,
    val gamma: Int = 0,
    val hue: Int = 0,
    val sharpen: Int = 0,
) {
    Default(
        MR.strings.player_sheets_filters_theme_default,
        description = "No filters applied.",
    ),
    Anime(
        MR.strings.player_sheets_filters_theme_anime,
        description = "Vivid colors and sharper edges, best for modern anime.",
        contrast = 5,
        saturation = 20,
        sharpen = 15,
    ),
    Cinema(
        MR.strings.player_sheets_filters_theme_cinema,
        description = "Movie-like experience with higher contrast and lower saturation.",
        brightness = -5,
        contrast = 15,
        saturation = -10,
        gamma = -5,
    ),
    Warm(
        MR.strings.player_sheets_filters_theme_warm,
        description = "Warmer color temperature for a cozy feel.",
        hue = -5,
        saturation = 5,
    ),
    Cold(
        MR.strings.player_sheets_filters_theme_cold,
        description = "Cooler color temperature with slightly reduced saturation.",
        hue = 5,
        saturation = -5,
    ),
    Night(
        MR.strings.player_sheets_filters_theme_night,
        description = "Comfortable night viewing by reducing brightness and contrast.",
        brightness = -20,
        contrast = -10,
        gamma = -10,
    ),
    Grayscale(
        MR.strings.player_sheets_filters_theme_grayscale,
        description = "Classic black and white mode.",
        saturation = -100,
    ),
    Vibrant(
        MR.strings.player_sheets_filters_theme_vibrant,
        description = "Boosts colors for a more lively image.",
        contrast = 10,
        saturation = 30,
    ),
    Vintage(
        MR.strings.player_sheets_filters_theme_vintage,
        description = "A nostalgic look with faded colors.",
        contrast = 10,
        saturation = -30,
        gamma = -10,
        hue = -5,
    ),
    HighContrast(
        MR.strings.player_sheets_filters_theme_high_contrast,
        description = "Sharper difference between light and dark areas.",
        brightness = -10,
        contrast = 30,
    ),
}

