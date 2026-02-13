package eu.kanade.domain.ui

import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.NavStyle
import eu.kanade.domain.ui.model.StartScreen
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isDynamicColorAvailable
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class UiPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun themeMode() = preferenceStore.getEnum("pref_theme_mode_key", ThemeMode.SYSTEM)

    fun appTheme() = preferenceStore.getEnum(
        "pref_app_theme",
        if (DeviceUtil.isDynamicColorAvailable) {
            AppTheme.MONET
        } else {
            AppTheme.DEFAULT
        },
    )

    fun colorTheme() = preferenceStore.getInt("pref_color_theme", 0)

    fun themeDarkAmoled() = preferenceStore.getBoolean("pref_theme_dark_amoled_key", false)

    fun relativeTime() = preferenceStore.getBoolean("relative_time_v2", true)

    fun dateFormat() = preferenceStore.getString("app_date_format", "")

    fun tabletUiMode() = preferenceStore.getEnum("tablet_ui_mode", TabletUiMode.AUTOMATIC)

    fun startScreen() = preferenceStore.getEnum("start_screen", StartScreen.ANIME)

    fun navStyle() = preferenceStore.getEnum("bottom_rail_nav_style", NavStyle.SHOW_ALL)

    fun enableFeed() = preferenceStore.getBoolean("enable_feed", true)

    // SY -->
    fun bottomBarLabels() = preferenceStore.getBoolean("pref_show_bottom_bar_labels", true)

    fun dynamicMangaTheme() = preferenceStore.getBoolean("pref_dynamic_manga_theme", true)

    fun autoExpandAnimeDescription() = preferenceStore.getBoolean("pref_auto_expand_anime_description", false)

    fun showSeasonsSection() = preferenceStore.getBoolean("pref_show_seasons_section", true)

    fun animeItemSpacing() = preferenceStore.getInt("pref_anime_item_spacing", 24)

    fun panoramaCover() = preferenceStore.getBoolean("pref_panorama_cover", false)

    fun containerStyles() = preferenceStore.getStringSet("pref_ui_container_styles", emptySet())
    // SY <--

    companion object {
        fun dateFormat(format: String): DateTimeFormatter = when (format) {
            "" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            else -> DateTimeFormatter.ofPattern(format, Locale.getDefault())
        }

        object ContainerStyle {
            const val LIBRARY = "library"
            const val UPDATES = "updates"
            const val HISTORY = "history"
            const val DETAILS = "details"
            const val SETTINGS = "settings"
            const val BROWSE = "browse"
        }
    }
}
