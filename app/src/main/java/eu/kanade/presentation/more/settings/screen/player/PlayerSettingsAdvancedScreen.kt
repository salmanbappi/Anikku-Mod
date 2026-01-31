package eu.kanade.presentation.more.settings.screen.player

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.tachiyomi.ui.player.settings.AdvancedPlayerPreferences
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object PlayerSettingsAdvancedScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_player_advanced

    @Composable
    override fun getPreferences(): List<Preference> {
        val advancedPlayerPreferences = remember { Injekt.get<AdvancedPlayerPreferences>() }
        val playerPreferences = remember { Injekt.get<PlayerPreferences>() }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        val enableScripts = advancedPlayerPreferences.mpvScripts()
        val mpvConf = advancedPlayerPreferences.mpvConf()
        val mpvInput = advancedPlayerPreferences.mpvInput()

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_mpv_scripts),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        title = stringResource(MR.strings.pref_mpv_scripts),
                        subtitle = stringResource(MR.strings.pref_mpv_scripts_summary),
                        pref = enableScripts,
                        onValueChanged = {
                            // Ask for external storage permission
                            if (it) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                    intent.data = Uri.fromParts("package", context.packageName, null)
                                    context.startActivity(intent)
                                }
                            }
                            true
                        },
                    ),
                    Preference.PreferenceItem.MPVConfPreference(
                        pref = mpvConf,
                        title = stringResource(MR.strings.pref_mpv_conf),
                        fileName = "mpv.conf",
                        scope = scope,
                        context = context,
                    ),
                    Preference.PreferenceItem.MPVConfPreference(
                        pref = mpvInput,
                        title = stringResource(MR.strings.pref_mpv_input),
                        fileName = "input.conf",
                        scope = scope,
                        context = context,
                    ),
                ),
            ),
            // SY -->
            Preference.PreferenceGroup(
                title = "Orientation & UI",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = playerPreferences.forceHorzSeekbar(),
                        title = stringResource(SYMR.strings.pref_force_horz_seekbar),
                        subtitle = stringResource(SYMR.strings.pref_force_horz_seekbar_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = playerPreferences.leftHandedVerticalSeekbar(),
                        title = stringResource(SYMR.strings.pref_left_handed_vertical_seekbar),
                        subtitle = stringResource(SYMR.strings.pref_left_handed_vertical_seekbar_summary),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Buffer & Cache",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = advancedPlayerPreferences.aggressivelyLoadPages(),
                        title = stringResource(SYMR.strings.aggressively_load_pages),
                        subtitle = stringResource(SYMR.strings.aggressively_load_pages_summary),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = advancedPlayerPreferences.readerCacheSize(),
                        title = stringResource(SYMR.strings.reader_cache_size),
                        subtitle = stringResource(SYMR.strings.reader_cache_size_summary),
                        entries = persistentMapOf(
                            50 to "50 MB",
                            100 to "100 MB",
                            250 to "250 MB",
                            500 to "500 MB",
                            1000 to "1 GB",
                        ),
                    ),
                ),
            ),
            // SY <--
        )
    }
}
