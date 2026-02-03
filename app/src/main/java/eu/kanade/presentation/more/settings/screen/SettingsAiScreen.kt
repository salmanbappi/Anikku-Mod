package eu.kanade.presentation.more.settings.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.ai.AiPreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.ai.AiAssistantScreen
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsAiScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_ai

    @Composable
    override fun getPreferences(): List<Preference> {
        val aiPreferences = remember { Injekt.get<AiPreferences>() }
        val navigator = LocalNavigator.currentOrThrow

        return listOf(
            getMainGroup(aiPreferences, navigator),
            getAssistantGroup(aiPreferences),
            getStatisticsGroup(aiPreferences),
        )
    }

    @Composable
    private fun getMainGroup(aiPreferences: AiPreferences, navigator: cafe.adriel.voyager.navigator.Navigator): Preference.PreferenceGroup {
        val enableAiPref = aiPreferences.enableAi()
        val enableAi by enableAiPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_ai),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = enableAiPref,
                    title = stringResource(MR.strings.pref_enable_ai),
                    subtitle = stringResource(MR.strings.pref_enable_ai_summary),
                ),
                Preference.PreferenceItem.EditTextPreference(
                    pref = aiPreferences.geminiApiKey(),
                    title = stringResource(MR.strings.pref_ai_gemini_api_key),
                    subtitle = stringResource(MR.strings.pref_ai_gemini_api_key_summary),
                    enabled = enableAi,
                )
            ),
        )
    }

    @Composable
    private fun getAssistantGroup(aiPreferences: AiPreferences): Preference.PreferenceGroup {
        val enableAi by aiPreferences.enableAi().collectAsState()

        return Preference.PreferenceGroup(
            title = "AI Assistant",
            enabled = enableAi,
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = aiPreferences.enableAiAssistant(),
                    title = "Enable Assistant",
                    subtitle = "Adds a conversational assistant to settings",
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = aiPreferences.aiAssistantLogs(),
                    title = "Allow Logs Access",
                    subtitle = "Allows AI to see recent error logs for troubleshooting",
                ),
            ),
        )
    }

    @Composable
    private fun getStatisticsGroup(aiPreferences: AiPreferences): Preference.PreferenceGroup {
        val enableAi by aiPreferences.enableAi().collectAsState()

        return Preference.PreferenceGroup(
            title = "AI Statistics",
            enabled = enableAi,
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = aiPreferences.enableAiStatistics(),
                    title = "Enable AI Insights",
                    subtitle = "Show AI-generated summaries in the Stats screen",
                ),
            ),
        )
    }
}
