package eu.kanade.tachiyomi.data.ai

import android.content.Context
import eu.kanade.domain.ai.AiPreferences
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.BufferedReader
import java.io.InputStreamReader

class AiManager(
    private val context: Context,
    private val networkHelper: NetworkHelper = Injekt.get(),
    private val aiPreferences: AiPreferences = Injekt.get(),
    private val json: Json = Injekt.get(),
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun chatWithAssistant(query: String, history: List<ChatMessage>): String? {
        if (!aiPreferences.enableAi().get() || !aiPreferences.enableAiAssistant().get()) return null
        
        val engine = aiPreferences.aiEngine().get()
        val apiKey = if (engine == "gemini") {
            aiPreferences.geminiApiKey().get()
        } else {
            aiPreferences.groqApiKey().get()
        }.ifBlank { return "Please set an API Key in Settings > Advanced Analytics" }

        val logs = if (aiPreferences.aiAssistantLogs().get()) getRecentLogs() else "Logs access disabled by user."
        
        val systemInstruction = """
            You are the 'Anikku System Assistant', a technical support core for the Anikku anime platform.
            You have full knowledge of the application's internal structure and configuration.
            
            APP KNOWLEDGE BASE:
            ${getAppKnowledgeBase()}
            
            USER ENVIRONMENT:
            ${getDeviceInfo()}
            
            RECENT ERROR LOGS:
            $logs
            
            OPERATIONAL PROTOCOLS:
            1. NAVIGATION: Direct users to specific settings (e.g., "Settings > Player > Shaders").
            2. TROUBLESHOOTING: Analyze logs for HTTP codes (403: Forbidden, 404: Not Found, 503: Source Down).
            3. FEATURES: Explain engineering details like the Semaphore-based chunked downloader and Anime4K upscaling.
            4. TONE: Senior Technical Engineer. Precise, helpful, and concise. No fluff or generic AI marketing.
        """.trimIndent()

        val messages = history.toMutableList()
        messages.add(ChatMessage(role = "user", content = query))

        return if (engine == "gemini") {
            callGemini(messages, apiKey, systemInstruction)
        } else {
            callGroq(messages, apiKey, systemInstruction)
        }
    }

    suspend fun getStatisticsAnalysis(statsSummary: String): String? {
        if (!aiPreferences.enableAi().get() || !aiPreferences.enableAiStatistics().get()) return null
        
        val engine = aiPreferences.aiEngine().get()
        val apiKey = if (engine == "gemini") {
            aiPreferences.geminiApiKey().get()
        } else {
            aiPreferences.groqApiKey().get()
        }.ifBlank { return null }

        val prompt = """
            Analyze the following user data and generate a 'Behavioral Profile' and 'Technical Summary'.
            
            STATISTICS SUMMARY:
            $statsSummary
            
            REPORT STRUCTURE:
            - **User Classification**: Assign a technical archetype based on their consumption data.
            - **Genre Distribution**: Deep analysis of their genre affinity and preferences.
            - **Engagement Metrics**: Insights on their watch time and library management habits.
            - **Strategic Projections**: 3-5 specific anime recommendations tailored to their unique behavioral profile.
            
            Format with clear Markdown headers and a professional 'Data Scientist' tone.
        """.trimIndent()

        return if (engine == "gemini") {
            callGemini(listOf(ChatMessage(role = "user", content = prompt)), apiKey, "You are a senior data analyst specialized in behavioral trends.")
        } else {
            callGroq(listOf(ChatMessage(role = "user", content = prompt)), apiKey, "You are a senior data analyst specialized in behavioral trends.")
        }
    }

    private fun getDeviceInfo(): String {
        return "Model: ${android.os.Build.MODEL}, SDK: ${android.os.Build.VERSION.SDK_INT}, App: Anikku"
    }

    private fun getAppKnowledgeBase(): String {
        return """
            DETAILED SYSTEM & SETTINGS MAP:
            
            1. **UI & Appearance (UiPreferences)**:
                - Themes: `pref_theme_mode_key` (System/Light/Dark), `pref_app_theme` (Monet, Nord, Doom, etc.).
                - AMOLED: `pref_theme_dark_amoled_key` for pure black dark mode.
                - Navigation: `bottom_rail_nav_style` (Show All, Small, Lean), `pref_show_bottom_bar_labels`.
                - Layout: `tablet_ui_mode` (Automatic/Landscape/Never), `start_screen` (Library/Updates/History/Browse).
                
            2. **Library & Metadata (LibraryPreferences)**:
                - Updates: `pref_library_update_interval_key` (Off to 48h), `auto_update_metadata`.
                - Restrictions: `library_update_restriction` (WiFi only, Charging, Not metered).
                - Logic: Fetch Interval exponential backoff logic (doubles interval if updates are missed for 10 consecutive cycles).
                - Display: `pref_display_mode_library` (Compact Grid, Comfortable Grid, List, Cover Only), `pref_animelib_columns_portrait_key`.
                - Filtering: `pref_filter_animelib_downloaded_v2`, `pref_filter_animelib_unread_v2`, `pref_filter_animelib_started_v2`.
                - Interaction: `pref_episode_swipe_start_action` (Toggle Seen/Bookmark/Fillermark/Download).
                
            3. **High-Performance Player (PlayerPreferences)**:
                - Orientation: `pref_default_player_orientation_type_key` (Sensor Landscape, Locked, etc.).
                - Precision: `pref_progress_preference` (Mark as seen at % threshold, default 85%).
                - Controls: `pref_allow_gestures_in_panels`, `pref_show_loading`, `pref_player_time_to_disappear` (default 4000ms).
                - Memory: `pref_panel_opacity`, `pref_reduce_motion`.
                - Automation: `pref_enable_skip_intro`, `pref_enable_auto_skip_ani_skip`, `pref_waiting_time_aniskip` (default 5s).
                - Upscaling (Anime4K): Real-time CNN upscaling profiles (Settings > Player > Shaders).
                - Gestures: Left side (Brightness), Right side (Volume), Center (Seek/2x Speed Long-press).
                
            4. **Multi-threaded Downloader (DownloadPreferences)**:
                - Threads: `download_threads` (Max 30, memory-aware semaphore bounds RAM to ~80MB).
                - Logic: Chunked multi-threading for BDIX saturation; parallel segment fetching with sequential disk channel writing.
                - Cache: `auto_clear_chapter_cache` to manage storage pressure.
                
            5. **Advanced Analytics & Core Engine**:
                - Architecture: Clean Architecture, DI (Injekt), DB (SQLDelight), Media (MPV via JNI).
                - BDIX optimization: Network stack prioritizes peering bandwidth for low-latency streaming.
                - Diagnostics: `AiAssistantScreen` integrates Logcat extraction for troubleshooting stack traces.
                
            INTERNAL KEYS & VALUES:
            - `pref_preserve_watching_position`: Ensures resume accuracy across sessions.
            - `pref_display_vol_as_per`: Controls whether volume is shown as a percentage overlay.
            - `pref_enable_netflixStyle_aniskip`: Changes the skip UI to a modern Netflix-style overlay.
        """.trimIndent()
    }

    private suspend fun getRecentLogs(): String = withIOContext {
        try {
            val process = Runtime.getRuntime().exec("logcat -d -t 100 *:E")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val log = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                log.append(line).append("\n")
            }
            log.toString().takeLast(5000)
        } catch (e: Exception) {
            "Failed to fetch logs: ${e.message}"
        }
    }

    private suspend fun callGemini(messages: List<ChatMessage>, apiKey: String, systemInstruction: String? = null): String? = withIOContext {
        val geminiContents = messages.map { msg ->
            GeminiContent(
                parts = listOf(GeminiPart(text = msg.content)),
                role = if (msg.role == "user") "user" else "model"
            )
        }

        val requestBody = GeminiRequest(
            contents = geminiContents,
            systemInstruction = systemInstruction?.let {
                GeminiContent(parts = listOf(GeminiPart(text = it)))
            }
        )

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=$apiKey")
            .header("Content-Type", "application/json")
            .post(json.encodeToString(GeminiRequest.serializer(), requestBody).toRequestBody(jsonMediaType))
            .build()

        try {
            networkHelper.client.newCall(request).execute().use { response ->
                val bodyString = response.body.string()
                if (!response.isSuccessful) return@withIOContext "Error ${response.code}: ${response.message}\n$bodyString"
                val geminiResponse = json.decodeFromString(GeminiResponse.serializer(), bodyString)
                geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            }
        } catch (e: Exception) {
            "Exception: ${e.message}"
        }
    }

    private suspend fun callGroq(messages: List<ChatMessage>, apiKey: String, systemInstruction: String? = null): String? = withIOContext {
        val groqMessages = mutableListOf<GroqMessage>()
        if (systemInstruction != null) {
            groqMessages.add(GroqMessage(role = "system", content = systemInstruction))
        }
        messages.forEach { msg ->
            groqMessages.add(GroqMessage(role = if (msg.role == "user") "user" else "assistant", content = msg.content))
        }

        val requestBody = GroqRequest(
            messages = groqMessages,
            model = "groq/compound-mini"
        )

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(json.encodeToString(GroqRequest.serializer(), requestBody).toRequestBody(jsonMediaType))
            .build()

        try {
            networkHelper.client.newCall(request).execute().use { response ->
                val bodyString = response.body.string()
                if (!response.isSuccessful) return@withIOContext "Error ${response.code}: ${response.message}\n$bodyString"
                val groqResponse = json.decodeFromString(GroqResponse.serializer(), bodyString)
                groqResponse.choices.firstOrNull()?.message?.content?.trim()
            }
        } catch (e: Exception) {
            "Exception: ${e.message}"
        }
    }

    @Serializable
    data class ChatMessage(val role: String, val content: String)

    @Serializable
    private data class GeminiRequest(
        val contents: List<GeminiContent>,
        @kotlinx.serialization.SerialName("system_instruction")
        val systemInstruction: GeminiContent? = null
    )

    @Serializable
    private data class GeminiContent(val parts: List<GeminiPart>, val role: String? = null)

    @Serializable
    private data class GeminiPart(val text: String)

    @Serializable
    private data class GeminiResponse(val candidates: List<GeminiCandidate>)

    @Serializable
    private data class GeminiCandidate(val content: GeminiContent)

    @Serializable
    private data class GroqRequest(
        val messages: List<GroqMessage>,
        val model: String
    )

    @Serializable
    private data class GroqMessage(val role: String, val content: String)

    @Serializable
    private data class GroqResponse(val choices: List<GroqChoice>)

    @Serializable
    private data class GroqChoice(val message: GroqMessage)
}
