package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import eu.kanade.domain.ai.AiPreferences
import eu.kanade.presentation.more.settings.Preference
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

        return listOf(
            getMainGroup(aiPreferences = aiPreferences),
            getFeaturesGroup(aiPreferences = aiPreferences),
            getApiKeysGroup(aiPreferences = aiPreferences),
            getCustomApiGroup(aiPreferences = aiPreferences),
        )
    }

    @Composable
    private fun getMainGroup(aiPreferences: AiPreferences): Preference.PreferenceGroup {
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
            ),
        )
    }

    @Composable
    private fun getFeaturesGroup(aiPreferences: AiPreferences): Preference.PreferenceGroup {
        val enableAi by aiPreferences.enableAi().collectAsState()

        return Preference.PreferenceGroup(
            title = "AI Features",
            enabled = enableAi,
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = aiPreferences.smartSearch(),
                    title = stringResource(MR.strings.pref_ai_smart_search),
                    subtitle = stringResource(MR.strings.pref_ai_smart_search_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = aiPreferences.episodeIntelligence(),
                    title = stringResource(MR.strings.pref_ai_episode_intelligence),
                    subtitle = stringResource(MR.strings.pref_ai_episode_intelligence_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = aiPreferences.localRecommendations(),
                    title = stringResource(MR.strings.pref_ai_local_recommendations),
                    subtitle = stringResource(MR.strings.pref_ai_local_recommendations_summary),
                ),
            ),
        )
    }

    @Composable
    private fun getApiKeysGroup(aiPreferences: AiPreferences): Preference.PreferenceGroup {
        val enableAi by aiPreferences.enableAi().collectAsState()

        return Preference.PreferenceGroup(
            title = "Default Providers (Groq/Gemini)",
            enabled = enableAi,
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.EditTextPreference(
                    pref = aiPreferences.groqApiKey(),
                    title = stringResource(MR.strings.pref_ai_groq_api_key),
                    subtitle = stringResource(MR.strings.pref_ai_groq_api_key_summary),
                ),
                Preference.PreferenceItem.EditTextPreference(
                    pref = aiPreferences.geminiApiKey(),
                    title = stringResource(MR.strings.pref_ai_gemini_api_key),
                    subtitle = stringResource(MR.strings.pref_ai_gemini_api_key_summary),
                ),
            ),
        )
    }

    @Composable
    private fun getCustomApiGroup(aiPreferences: AiPreferences): Preference.PreferenceGroup {
        val enableAi by aiPreferences.enableAi().collectAsState()

        return Preference.PreferenceGroup(
            title = "Custom Provider (OpenAI Compatible)",
            enabled = enableAi,
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.EditTextPreference(
                    pref = aiPreferences.customBaseUrl(),
                    title = "Base URL",
                    subtitle = "e.g. https://api.context7.com/v1",
                ),
                Preference.PreferenceItem.EditTextPreference(
                    pref = aiPreferences.customModel(),
                    title = "Model Name",
                    subtitle = "e.g. gpt-4o, llama-3, gemini-2.5-flash",
                ),
            ),
        )
    }
}
