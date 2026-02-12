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

    fun aiAssistantLibrary() = preferenceStore.getBoolean("ai_assistant_library", true)

    fun aiSystemPrompt() = preferenceStore.getString("ai_system_prompt", "")

    // Statistics
    fun enableAiStatistics() = preferenceStore.getBoolean("enable_ai_statistics", true)

    // Profile
    fun profilePhotoUri() = preferenceStore.getString("profile_photo_uri", "")
    fun displayName() = preferenceStore.getString("display_name", "Anime Explorer")

    fun activeSessionId() = preferenceStore.getLong("active_ai_session_id", -1L)

    // Circuit Breaker
    fun lastAiRequestTime() = preferenceStore.getLong("last_ai_request_time", 0L)
    fun hourlyAiRequestCount() = preferenceStore.getInt("hourly_ai_request_count", 0)
    fun isCircuitBreakerTripped() = preferenceStore.getBoolean("ai_circuit_breaker_tripped", false)
    fun isRequestPending() = preferenceStore.getBoolean("ai_request_pending", false)
}
