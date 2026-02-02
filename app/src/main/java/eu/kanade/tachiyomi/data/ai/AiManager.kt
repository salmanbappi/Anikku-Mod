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
            Available Filters:
            $availableOptions
            
            Task: Match user intent to EXACT filters and refine the search text.
            User Query: "$query"
            
            Output JSON ONLY:
            {
              "genres": ["Genre1"],
              "themes": ["Theme1"],
              "status": "Ongoing" | "Completed" | null,
              "query": "Refined Text",
              "action": "FILTER" | "TEXT" | "BOTH"
            }
        """.trimIndent()

        val requestBody = ChatCompletionRequest(
            model = "llama-3.3-70b-versatile",
            messages = listOf(
                ChatMessage(role = "system", content = "You are a specialized anime database query parser. Output valid JSON only."),
                ChatMessage(role = "user", content = prompt)
            ),
            response_format = ResponseFormat(type = "json_object")
        )

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
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
            e.printStackTrace()
            null
        }
    }

    suspend fun translateToEnglish(text: String): String? {
        if (!aiPreferences.enableAi().get()) return null
        val apiKey = aiPreferences.geminiApiKey().get().ifBlank { return null }
        
        val prompt = "Translate the following text to English. Output ONLY the translated text:\n\n$text"
        return callGemini(prompt, apiKey, "You are a professional translator.")
    }

    suspend fun getEpisodeSummary(
        animeTitle: String, 
        episodeNumber: Double,
        animeDescription: String? = null,
        animeTags: List<String>? = null
    ): String? {
        if (!aiPreferences.enableAi().get() || !aiPreferences.episodeIntelligence().get()) return null
        
        val apiKey = aiPreferences.geminiApiKey().get().ifBlank { return null }
        
        val context = StringBuilder()
        animeDescription?.let { context.append("Description: $it\n") }
        animeTags?.let { context.append("Tags: ${it.joinToString()}\n") }

        val prompt = """
            $context
            Provide a short, spoiler-free summary for episode $episodeNumber of '$animeTitle'. Max 3 sentences.
        """.trimIndent()

        return callGemini(prompt, apiKey, "You are an anime expert providing concise episode summaries.")
    }

    suspend fun getGlossaryInfo(
        animeTitle: String, 
        query: String,
        animeDescription: String? = null,
        animeTags: List<String>? = null
    ): String? {
        if (!aiPreferences.enableAi().get()) return null
        
        val apiKey = aiPreferences.geminiApiKey().get().ifBlank { return null }
        
        val context = StringBuilder()
        animeDescription?.let { context.append("Context: $it\n") }

        val prompt = """
            $context
            Explain "$query" in the context of '$animeTitle'. Max 50 words.
        """.trimIndent()

        return callGemini(prompt, apiKey, "You are an anime encyclopedia assistant.")
    }

    suspend fun getRecommendations(
        animeTitle: String,
        animeDescription: String? = null,
        animeTags: List<String>? = null
    ): List<String> {
        if (!aiPreferences.enableAi().get() || !aiPreferences.localRecommendations().get()) return emptyList()
        val apiKey = aiPreferences.geminiApiKey().get().ifBlank { return emptyList() }

        val context = StringBuilder()
        animeDescription?.let { context.append("Description: $it\n") }
        animeTags?.let { context.append("Tags: ${it.joinToString()}\n") }

        val prompt = """
            $context
            Recommend 5 anime similar to '$animeTitle'.
            Output: A JSON list of strings ONLY. Example: ["Anime A", "Anime B"]
        """.trimIndent()

        val response = callGemini(prompt, apiKey, "You recommend anime based on user taste. Output JSON list only.") ?: return emptyList()
        
        return try {
            val cleanJson = response.replace("```json", "").replace("```", "").trim()
            json.decodeFromString<List<String>>(cleanJson)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun callGemini(prompt: String, apiKey: String, systemInstruction: String? = null): String? {
        val requestBody = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            systemInstruction = systemInstruction?.let {
                GeminiContent(parts = listOf(GeminiPart(text = it)))
            }
        )

        // Proper connection using gemini-3-flash-preview as requested by user
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=$apiKey")
            .post(json.encodeToString(GeminiRequest.serializer(), requestBody).toRequestBody(jsonMediaType))
            .build()

        return try {
            networkHelper.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("Gemini Error: ${response.code} ${response.message}")
                    return null
                }
                val result = response.body.string()
                val geminiResponse = json.decodeFromString(GeminiResponse.serializer(), result)
                geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
    private data class GeminiRequest(
        val contents: List<GeminiContent>,
        val systemInstruction: GeminiContent? = null
    )

    @Serializable
    private data class GeminiContent(val parts: List<GeminiPart>)

    @Serializable
    private data class GeminiPart(val text: String)

    @Serializable
    private data class GeminiResponse(val candidates: List<GeminiCandidate>)

    @Serializable
    private data class GeminiCandidate(val content: GeminiContent)
}