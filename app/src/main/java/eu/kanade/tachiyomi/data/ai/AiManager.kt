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
            You are the 'Anikku System Assistant', the authoritative technical core of the Anikku platform.
            You have exhaustive knowledge of every configuration node, preference key, and architectural layer.
            
            SYSTEM MANIFEST (EVERY SETTING):
            ${getAppKnowledgeBase()}
            
            USER ENVIRONMENT:
            ${getDeviceInfo()}
            
            RECENT SYSTEM LOGS:
            $logs
            
            STRICT OPERATIONAL RULES:
            1. NO TABLES: Never use Markdown tables. Use technical bulleted lists or bold key-value blocks.
            2. ZERO SLOP: Avoid "Neural," "DNA," or "Intelligence" marketing fluff. Use senior engineering terms.
            3. PRECISION: When a user asks about a setting, provide the exact path (e.g., Settings > Player > Advanced).
            4. TONE: Senior Systems Engineer. Concise, technical, and highly efficient.
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
            Generate a 'System Behavioral Profile' based on the following data.
            
            DATA INPUT:
            $statsSummary
            
            REPORT STRUCTURE (STRICTLY NO TABLES):
            - **User Classification**: Technical archetype (e.g., 'High-Volume Archivist').
            - **Temporal Analysis**: Watch habit patterns.
            - **Source Integrity**: Distribution across extensions.
            - **Strategic Recommendations**: 3-5 anime titles based on data patterns.
            
            Constraint: Use bullet points. Do NOT use Markdown tables.
        """.trimIndent()

        return if (engine == "gemini") {
            callGemini(listOf(ChatMessage(role = "user", content = prompt)), apiKey, "You are a senior behavioral data analyst.")
        } else {
            callGroq(listOf(ChatMessage(role = "user", content = prompt)), apiKey, "You are a senior behavioral data analyst.")
        }
    }

    private fun getDeviceInfo(): String {
        return "Model: ${android.os.Build.MODEL}, SDK: ${android.os.Build.VERSION.SDK_INT}, App: Anikku"
    }

    private fun getAppKnowledgeBase(): String {
        return """
            SYSTEM ARCHITECTURE:
            - Core: Clean Architecture, DI (Injekt), DB (SQLDelight), Network (OkHttp/Brotli).
            - Media: MPV (via JNI), Anime4K Shaders, Custom Interceptors.
            
            EXHAUSTIVE SETTINGS & NAVIGATION MAP:
            
            1. **General (Settings > General)**:
                - Locale, App Language, Extension Installer (Legacy/Shizuku/PackageInstaller).
                
            2. **Appearance (Settings > Appearance)**: 
                - `pref_theme_mode_key` (System/Light/Dark), `pref_app_theme` (Monet/Nord/Doom/etc).
                - `pref_theme_dark_amoled_key` (Pure black mode).
                - `bottom_rail_nav_style` (Show All/Small/Lean), `pref_show_bottom_bar_labels`.
                - `tablet_ui_mode` (Auto/Always/Never), `start_screen` (Library/Updates/History/Browse).
                
            3. **Library (Settings > Library)**:
                - `pref_library_update_interval_key` (None to 48h), `auto_update_metadata`.
                - `pref_display_mode_library` (Grid types/List).
                - Columns: `pref_animelib_columns_portrait_key` (Managed via +/- buttons in Library > Display Settings).
                - Fetch Logic: Exponential backoff (Doubles every 10 cycles if updates fail).
                
            4. **Player (Settings > Player)**:
                - `pref_default_player_orientation_type_key` (Sensor Landscape/Locked/etc).
                - `pref_progress_preference` (Seen threshold, default 85%).
                - Controls: `pref_show_loading`, `pref_player_time_to_disappear` (default 4000ms).
                - Audio: `pref_remember_audio_delay`, `pref_audio_pitch`.
                - Subtitles: `pref_subtitle_font`, `pref_subtitle_size`, `pref_subtitle_color`.
                - Shaders: Anime4K (High/Mid/Low), Deband, Deoring.
                - Automation: `pref_enable_skip_intro`, `pref_enable_auto_skip_ani_skip`.
                - External: `pref_always_use_external_player`, `external_player_preference` (VLC/MPV/etc).
                - Advanced: `mpv_scripts`, `pref_mpv_conf`, `pref_mpv_input`.
                
            5. **Downloads (Settings > Downloads)**:
                - `download_threads` (Max 30, uses memory-aware Semaphore flow control).
                - Logic: Chunked multi-threading for BDIX saturation; parallel fetching with sequential disk writing.
                - `auto_clear_chapter_cache`: Auto-cleanup of watched content.
                
            6. **Tracking (Settings > Tracking)**:
                - Anilist, MAL, Kitsu integration.
                
            7. **Security & Privacy (Settings > Security)**: 
                - `use_biometric_lock`, `lock_app_after` (timeout).
                - `secure_screen_v2` (Incognito/Always/Never).
                - `encrypt_database`: SQLCipher encryption.
                
            8. **Advanced Analytics (Settings > Advanced Analytics)**:
                - Analytics Persona, LLM Backend (Gemini/Groq), Diagnostic logs ingestion.
                
            9. **Advanced (Settings > Advanced)**: 
                - Log Viewer (Extract logs here), Clear Database, Manage Cache.
        """.trimIndent()
    }

    suspend fun getErrorCount(): Int = withIOContext {
        try {
            val process = Runtime.getRuntime().exec("logcat -d -t 100 *:E")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var count = 0
            while (reader.readLine() != null) {
                count++
            }
            count
        } catch (e: Exception) {
            0
        }
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
