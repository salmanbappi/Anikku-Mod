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

    private fun getEffectiveApiKey(): String {
        return aiPreferences.groqApiKey().get().ifBlank { aiPreferences.geminiApiKey().get() }
    }

    suspend fun parseSearchQuery(query: String, availableOptions: String): AiSearchResponse? {
        if (!aiPreferences.enableAi().get() || !aiPreferences.smartSearch().get()) return null
        
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) return null

        val customBaseUrl = aiPreferences.customBaseUrl().get()
        val isCustom = customBaseUrl.isNotBlank()

        val prompt = """
            You are an expert anime librarian.
            User Query: "$query"
            
            Available Filters:
            $availableOptions
            
            Task: Match intent to EXACT filters and refine query.
            Output JSON ONLY:
            {
              "genres": ["Genre1"],
              "themes": ["Theme1"],
              "status": "Ongoing" | "Completed" | null,
              "query": "Refined Text",
              "action": "FILTER" | "TEXT" | "BOTH"
            }
        """.trimIndent()

        val model = if (isCustom) aiPreferences.customModel().get().ifBlank { "llama-3.3-70b-versatile" } else "llama-3.3-70b-versatile"
        val url = if (isCustom) "$customBaseUrl/chat/completions" else "https://api.groq.com/openai/v1/chat/completions"

        val response = callAi(url, apiKey, model, prompt, true) ?: return null
        return try {
            json.decodeFromString(AiSearchResponse.serializer(), response)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun translateToEnglish(text: String): String? {
        if (!aiPreferences.enableAi().get()) return null
        
        val customBaseUrl = aiPreferences.customBaseUrl().get()
        val prompt = "Translate to English. Output only the translated text:\n$text"

        if (customBaseUrl.isNotBlank()) {
            val model = aiPreferences.customModel().get().ifBlank { "gemini-2.5-flash" }
            return callAi("$customBaseUrl/chat/completions", getEffectiveApiKey(), model, prompt, false)
        } else {
            val apiKey = aiPreferences.geminiApiKey().get().ifBlank { return null }
            return callGemini(prompt, apiKey)
        }
    }

    suspend fun getEpisodeSummary(
        animeTitle: String, 
        episodeNumber: Double,
        animeDescription: String? = null,
        animeTags: List<String>? = null
    ): String? {
        if (!aiPreferences.enableAi().get() || !aiPreferences.episodeIntelligence().get()) return null
        
        val context = StringBuilder()
        animeDescription?.let { context.append("Description: $it\n") }
        animeTags?.let { context.append("Tags: ${it.joinToString()}\n") }

        val prompt = """
            $context
            Short spoiler-free summary for Episode $episodeNumber of '$animeTitle'. Max 3 sentences.
        """.trimIndent()

        val customBaseUrl = aiPreferences.customBaseUrl().get()
        if (customBaseUrl.isNotBlank()) {
            val model = aiPreferences.customModel().get().ifBlank { "gemini-2.5-flash" }
            return callAi("$customBaseUrl/chat/completions", getEffectiveApiKey(), model, prompt, false)
        }

        val apiKey = aiPreferences.geminiApiKey().get().ifBlank { return null }
        return callGemini(prompt, apiKey)
    }

    suspend fun getGlossaryInfo(
        animeTitle: String, 
        query: String,
        animeDescription: String? = null,
        animeTags: List<String>? = null
    ): String? {
        if (!aiPreferences.enableAi().get()) return null
        
        val context = StringBuilder()
        animeDescription?.let { context.append("Context: $it\n") }

        val prompt = """
            $context
            Explain "$query" in context of '$animeTitle'. Max 50 words.
        """.trimIndent()

        val customBaseUrl = aiPreferences.customBaseUrl().get()
        if (customBaseUrl.isNotBlank()) {
            val model = aiPreferences.customModel().get().ifBlank { "gemini-2.5-flash" }
            return callAi("$customBaseUrl/chat/completions", getEffectiveApiKey(), model, prompt, false)
        }

        val apiKey = aiPreferences.geminiApiKey().get().ifBlank { return null }
        return callGemini(prompt, apiKey)
    }

    suspend fun getRecommendations(
        animeTitle: String,
        animeDescription: String? = null,
        animeTags: List<String>? = null
    ): List<String> {
        if (!aiPreferences.enableAi().get() || !aiPreferences.localRecommendations().get()) return emptyList()

        val context = StringBuilder()
        animeDescription?.let { context.append("Description: $it\n") }
        animeTags?.let { context.append("Tags: ${it.joinToString()}\n") }

        val prompt = """
            $context
            Recommend 5 anime similar to '$animeTitle'. Output: JSON list of strings ONLY.
        """.trimIndent()

        val customBaseUrl = aiPreferences.customBaseUrl().get()
        val response = if (customBaseUrl.isNotBlank()) {
            val model = aiPreferences.customModel().get().ifBlank { "gemini-2.5-flash" }
            callAi("$customBaseUrl/chat/completions", getEffectiveApiKey(), model, prompt, false)
        } else {
            val apiKey = aiPreferences.geminiApiKey().get().ifBlank { return emptyList() }
            callGemini(prompt, apiKey)
        } ?: return emptyList()
        
        return try {
            val cleanJson = response.replace("```json", "").replace("```", "").trim()
            json.decodeFromString<List<String>>(cleanJson)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun callAi(
        url: String, 
        apiKey: String, 
        model: String, 
        prompt: String,
        jsonMode: Boolean
    ): String? {
        val requestBody = ChatCompletionRequest(
            model = model,
            messages = listOf(
                ChatMessage(role = "system", content = "You are a helpful assistant."),
                ChatMessage(role = "user", content = prompt)
            ),
            response_format = if (jsonMode) ResponseFormat(type = "json_object") else null
        )

        val authHeader = if (apiKey.startsWith("ctx7sk")) apiKey else "Bearer $apiKey"

        val request = Request.Builder()
            .url(url)
            .header("Authorization", authHeader)
            .post(json.encodeToString(ChatCompletionRequest.serializer(), requestBody).toRequestBody(jsonMediaType))
            .build()

        return try {
            networkHelper.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("AI Error: ${response.code} ${response.message}")
                    return null
                }
                val result = response.body.string()
                val chatResponse = json.decodeFromString(ChatCompletionResponse.serializer(), result)
                chatResponse.choices.firstOrNull()?.message?.content
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun callGemini(prompt: String, apiKey: String): String? {
        val requestBody = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
        )

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
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
