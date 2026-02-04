package eu.kanade.tachiyomi.data.ai

import android.content.Context
import android.os.Build
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
            
            STRICT OUTPUT RULES:
            1. NO TABLES: Never use Markdown tables. They are difficult to read on mobile screens. 
               Use bulleted lists or bold key-value pairs instead.
            2. NAVIGATION: Direct users to specific settings using the "Settings > [Category] > [Setting]" format.
            3. TROUBLESHOOTING: Analyze logs for HTTP codes (403: Forbidden, 404: Not Found, 503: Source Down).
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
            COMPLETE SETTINGS & NAVIGATION MAP:
            
            1. **General (Settings > General)**:
                - App language, Locale selection.
                - Installer settings: APK installation method.
                
            2. **Appearance (Settings > Appearance)**:
                - Theme: `pref_theme_mode_key` (System/Light/Dark), `pref_app_theme` (Monet, Nord, Doom, etc.).
                - AMOLED: `pref_theme_dark_amoled_key` for pure black.
                - Navigation: `bottom_rail_nav_style` (Show All, Small, Lean).
                - Labels: `pref_show_bottom_bar_labels` (Toggle tab text).
                - Layout: `tablet_ui_mode`, `start_screen`.
                
            3. **Library (Settings > Library)**:
                - Updates: `pref_library_update_interval_key` (None to 48h), `auto_update_metadata`.
                - Display: `pref_display_mode_library` (Grid types/List).
                - Columns: `pref_animelib_columns_portrait_key` (Slider in Library > Display Settings).
                - Fetch Logic: Exponential backoff (Doubles every 10 cycles if updates fail).
                
            4. **Player (Settings > Player)**:
                - Orientation: `pref_default_player_orientation_type_key`.
                - Controls: `pref_show_loading`, `pref_player_time_to_disappear` (Control timeout).
                - Performance: `pref_panel_opacity`, `pref_reduce_motion`.
                - Shaders (Anime4K): Real-time CNN upscaling profiles.
                - Gestures: Left (Brightness), Right (Volume), Center (Seek/2x Speed).
                - Automation: Skip intro (`pref_enable_skip_intro`), Auto-skip (`pref_enable_auto_skip_ani_skip`).
                
            5. **Downloads (Settings > Downloads)**:
                - Concurrency: `download_threads` (Max 30, Semaphore-managed).
                - Logic: Multi-threaded chunked downloader for BDIX optimization.
                
            6. **Advanced Analytics & Diagnostics (Settings > Advanced Analytics)**:
                - Engine: LLM Backend (Gemini/Groq API Keys).
                - Support: Diagnostic Assistant, Data Summarization.
                - Logs: Logcat integration for troubleshooting.
                
            7. **Advanced (Settings > Advanced)**:
                - Log Viewer (Extract logs here), Clear Database, Manage Cache.
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
