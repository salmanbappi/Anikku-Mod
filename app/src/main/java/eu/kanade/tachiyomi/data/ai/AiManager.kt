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
            You are an expert anime librarian.
            User Query: "$query"
            
            Available Filters:
            $availableOptions
            
            Task:
            1. Analyze the user's intent (e.g., "sad romance anime", "isekai with op mc", "movies from 2020").
            2. Match intent to the EXACT available filters provided above.
            3. Refine the text query if needed (e.g., translate to English, remove stop words).
            
            Output JSON ONLY:
            {
              "genres": ["Genre1", "Genre2"], // Exact matches only
              "themes": ["Theme1", "Theme2"], // Exact matches only
              "status": "Ongoing" | "Completed" | null,
              "query": "Refined Text Search",
              "action": "FILTER" | "TEXT" | "BOTH"
            }
        """.trimIndent()

        val requestBody = ChatCompletionRequest(
            model = "llama-3.3-70b-versatile",
            messages = listOf(
                ChatMessage(role = "system", content = "You are a helpful assistant that outputs only valid JSON."),
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
                if (!response.isSuccessful) {
                    println("AI Search Error: ${'$'}{response.code} ${'$'}{response.message}")
                    return null
                }
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
        
        val prompt = "Translate the following text to English. If it is already in English, return it as is. Do not add explanations. Text:\n$text"
        return callGemini(prompt, apiKey)
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
        animeDescription?.let { context.append("Anime Description: $it\n") }
        animeTags?.let { context.append("Tags: ${it.joinToString()}\n") }

        val prompt = """
            $context
            Task: Write a concise, engaging, and SPOILER-FREE summary for Episode $episodeNumber of '$animeTitle'.
            Focus on the premise of the episode if specific details are unknown, or summarize the likely events based on the series context.
            Maximum 3 sentences.
        """.trimIndent()

        return callGemini(prompt, apiKey)
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
            Question: Explain "$query" in the context of the anime '$animeTitle'.
            Answer: Provide a short, cultural or plot-relevant explanation. Max 50 words.
        """.trimIndent()

        return callGemini(prompt, apiKey)
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
            Task: Recommend 5 anime similar to '$animeTitle'.
            Output: A JSON list of strings ONLY. Example: ["Anime A", "Anime B"]
            Do not include explanations or markdown formatting.
        """.trimIndent()

        val jsonString = callGemini(prompt, apiKey) ?: return emptyList()
        
        return try {
            val cleanJson = jsonString.replace("```json", "").replace("```", "").trim()
            json.decodeFromString<List<String>>(cleanJson)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun callGemini(prompt: String, apiKey: String): String? {
        val requestBody = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
        )

        // Using gemini-2.5-flash as the stable model
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${'$'}apiKey")
            .post(json.encodeToString(GeminiRequest.serializer(), requestBody).toRequestBody(jsonMediaType))
            .build()

        return try {
            networkHelper.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("Gemini Error: ${'$'}{response.code} ${'$'}{response.message}")
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
