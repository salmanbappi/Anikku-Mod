package eu.kanade.tachiyomi.data.ai

import eu.kanade.domain.ai.AiPreferences
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AiManager(
    private val networkHelper: NetworkHelper = Injekt.get(),
    private val aiPreferences: AiPreferences = Injekt.get(),
    private val json: Json = Injekt.get(),
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun parseSearchQuery(query: String, availableOptions: String): AiSearchResponse? {
        if (!aiPreferences.enableAi().get() || !aiPreferences.smartSearch().get()) return null
        
        val apiKey = aiPreferences.groqApiKey().get().ifBlank { return null }
        
        val prompt = """
            You are an AI search assistant for an anime app.
            Available Filter Options (Genres, Themes, etc.):
            $availableOptions
            
            Based on the user's query, output a JSON object with:
            - genres: list of genre/theme strings that EXACTLY match the available options.
            - status: "Ongoing" or "Completed" if specified, else null.
            - query: a refined search text (the anime title or specific keywords) in English. If the input is not in English, translate it.
            - action: "FILTER" if you set specific filters, "TEXT" if you only refined the query, or "BOTH".
            
            Query: "$query"
            
            Return ONLY the JSON.
        """.trimIndent()

        val requestBody = ChatCompletionRequest(
            model = "llama-3.3-70b-versatile",
            messages = listOf(
                ChatMessage(role = "system", content = "You are a specialized anime database query parser."),
                ChatMessage(role = "user", content = prompt)
            ),
            response_format = ResponseFormat(type = "json_object")
        )

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .header("Authorization", "Bearer ${'$'}apiKey")
            .post(json.encodeToString(ChatCompletionRequest.serializer(), requestBody).toRequestBody(jsonMediaType))
            .build()

        return try {
            networkHelper.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val result = response.body.string()
                val chatResponse = json.decodeFromString(ChatCompletionResponse.serializer(), result)
                val content = chatResponse.choices.firstOrNull()?.message?.content ?: return null
                json.decodeFromString(AiSearchResponse.serializer(), content)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun translateToEnglish(text: String): String? {
        if (!aiPreferences.enableAi().get()) return null
        val apiKey = aiPreferences.geminiApiKey().get().ifBlank { return null }
        
        val prompt = "Translate the following text to English. If it is already in English, return it exactly as is. Output ONLY the translated text:\n\n$text"
        return callGemini(prompt, apiKey)
    }

    suspend fun getEpisodeSummary(
        animeTitle: String, 
        episodeNumber: Double,
        animeDescription: String? = null,
        animeTags: List<String>? = null
    ): String? {
        if (!aiPreferences.enableAi().get() || !aiPreferences.episodeIntelligence().get() || aiPreferences.useLocalOnly().get()) return null
        
        val apiKey = aiPreferences.geminiApiKey().get().ifBlank { return null }
        
        val context = StringBuilder()
        animeDescription?.let { context.append("Description: $it\n") }
        animeTags?.let { context.append("Tags: ${it.joinToString()}\n") }

        val prompt = """
            $context
            Provide a short, spoiler-free summary for episode $episodeNumber of the anime '$animeTitle'. Do not include major plot twists.
        """.trimIndent()

        return callGemini(prompt, apiKey)
    }

    suspend fun getGlossaryInfo(
        animeTitle: String, 
        query: String,
        animeDescription: String? = null,
        animeTags: List<String>? = null
    ): String? {
        if (!aiPreferences.enableAi().get() || aiPreferences.useLocalOnly().get()) return null
        
        val apiKey = aiPreferences.geminiApiKey().get().ifBlank { return null }
        
        val context = StringBuilder()
        animeDescription?.let { context.append("Description: $it\n") }
        animeTags?.let { context.append("Tags: ${it.joinToString()}\n") }

        val prompt = """
            $context
            Explain the following about the anime '$animeTitle': $query. Provide cultural context if relevant. Keep it concise.
        """.trimIndent()

        return callGemini(prompt, apiKey)
    }

    private suspend fun callGemini(prompt: String, apiKey: String): String? {
        val requestBody = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
        )

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=${'$'}apiKey")
            .post(json.encodeToString(GeminiRequest.serializer(), requestBody).toRequestBody(jsonMediaType))
            .build()

        return try {
            networkHelper.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val result = response.body.string()
                val geminiResponse = json.decodeFromString(GeminiResponse.serializer(), result)
                geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            }
        } catch (e: Exception) {
            null
        }
    }

    @Serializable
    private data class ChatCompletionRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val response_format: ResponseFormat? = null,
    )

    @Serializable
    private data class ChatMessage(val role: String, val content: String)

    @Serializable
    private data class ResponseFormat(val type: String)

    @Serializable
    private data class ChatCompletionResponse(val choices: List<Choice>)

    @Serializable
    private data class Choice(val message: ChatMessageContent)

    @Serializable
    private data class ChatMessageContent(val content: String)

    @Serializable
    data class AiSearchResponse(
        val genres: List<String> = emptyList(),
        val themes: List<String> = emptyList(),
        val status: String? = null,
        val query: String? = null,
        val action: String = "TEXT"
    )

    @Serializable
    private data class GeminiRequest(val contents: List<GeminiContent>)

    @Serializable
    private data class GeminiContent(val parts: List<GeminiPart>)

    @Serializable
    private data class GeminiPart(val text: String)

    @Serializable
    private data class GeminiResponse(val candidates: List<GeminiCandidate>)

    @Serializable
    private data class GeminiCandidate(val content: GeminiContent)
}
