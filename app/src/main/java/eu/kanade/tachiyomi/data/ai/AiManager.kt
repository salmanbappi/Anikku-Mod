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
        }.ifBlank { return "Please set an API Key in Settings > AI Intelligence" }

        val logs = if (aiPreferences.aiAssistantLogs().get()) getRecentLogs() else "Logs access disabled by user."
        
        val systemInstruction = """
            You are 'AniZen Intelligence', the neural core of the Anikku platform. 
            Your mission is to provide elite technical support and deep anime insights.
            
            USER ENVIRONMENT:
            ${getDeviceInfo()}
            
            RECENT ERROR LOGS:
            $logs
            
            PROTOCOLS:
            1. Analyze logs for network errors (403, 404, 503), database locks, or DI failures.
            2. Explain app features like Anime4K upscaling, MPV shaders, and BDIX streaming.
            3. Provide strategic anime recommendations based on context.
            4. TONE: Professional, efficient, futuristic. Use technical terms correctly.
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
            Analyze the following user data and generate a 'Taste Profile' and 'Intelligence Report'.
            
            STATISTICS SUMMARY:
            $statsSummary
            
            REPORT STRUCTURE:
            - **The Archetype**: Assign a high-level title based on their data.
            - **Genre Signature**: Deep analysis of their genre affinity.
            - **Engagement Analysis**: Insights on their watch time and library growth.
            - **Neural Projection**: 3-5 specific anime recommendations tailored to their unique DNA.
            
            Format with clear Markdown headers and a professional 'Intelligence' tone.
        """.trimIndent()

        return if (engine == "gemini") {
            callGemini(listOf(ChatMessage(role = "user", content = prompt)), apiKey, "You are a senior anime data scientist.")
        } else {
            callGroq(listOf(ChatMessage(role = "user", content = prompt)), apiKey, "You are a senior anime data scientist.")
        }
    }

    private fun getDeviceInfo(): String {
        return "Model: ${android.os.Build.MODEL}, SDK: ${android.os.Build.VERSION.SDK_INT}, App: Anikku"
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
            GeminiContent(parts = listOf(GeminiPart(text = msg.content)), role = if (msg.role == "user") "user" else "model")
        }

        val requestBody = GeminiRequest(
            contents = geminiContents,
            systemInstruction = systemInstruction?.let {
                GeminiContent(parts = listOf(GeminiPart(text = it)))
            }
        )

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            .post(json.encodeToString(GeminiRequest.serializer(), requestBody).toRequestBody(jsonMediaType))
            .build()

        try {
            networkHelper.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withIOContext "Error: ${response.code} ${response.message}"
                val result = response.body.string()
                val geminiResponse = json.decodeFromString(GeminiResponse.serializer(), result)
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
            model = "llama3-8b-8192"
        )

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(json.encodeToString(GroqRequest.serializer(), requestBody).toRequestBody(jsonMediaType))
            .build()

        try {
            networkHelper.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withIOContext "Error: ${response.code} ${response.message}"
                val result = response.body.string()
                val groqResponse = json.decodeFromString(GroqResponse.serializer(), result)
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
