package eu.kanade.domain.ai

import tachiyomi.core.common.preference.PreferenceStore

class AiPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun enableAi() = preferenceStore.getBoolean("enable_ai", false)

    fun groqApiKey() = preferenceStore.getString("groq_api_key", "")

    fun geminiApiKey() = preferenceStore.getString("gemini_api_key", "")

    // Features
    fun smartSearch() = preferenceStore.getBoolean("ai_smart_search", true)

    fun episodeIntelligence() = preferenceStore.getBoolean("ai_episode_intelligence", true)

    fun subtitleEnhancer() = preferenceStore.getBoolean("ai_subtitle_enhancer", false)

    fun localRecommendations() = preferenceStore.getBoolean("ai_local_recommendations", true)

    fun performanceIntelligence() = preferenceStore.getBoolean("ai_performance_intelligence", true)

    fun personalAssistant() = preferenceStore.getBoolean("ai_personal_assistant", false)
}