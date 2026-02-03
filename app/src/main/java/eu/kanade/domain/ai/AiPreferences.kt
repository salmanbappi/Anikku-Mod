package eu.kanade.domain.ai

import tachiyomi.core.common.preference.PreferenceStore

class AiPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun enableAi() = preferenceStore.getBoolean("enable_ai", false)

    fun aiEngine() = preferenceStore.getString("ai_engine", "gemini")

    fun geminiApiKey() = preferenceStore.getString("gemini_api_key", "")

    fun groqApiKey() = preferenceStore.getString("groq_api_key", "")

    // Assistant
    fun enableAiAssistant() = preferenceStore.getBoolean("enable_ai_assistant", true)
    
    fun aiAssistantLogs() = preferenceStore.getBoolean("ai_assistant_logs", true)

    // Statistics
    fun enableAiStatistics() = preferenceStore.getBoolean("enable_ai_statistics", true)
}
