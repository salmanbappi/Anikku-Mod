package eu.kanade.tachiyomi.data.ai

import android.content.Context
import eu.kanade.domain.ai.AiPreferences
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.domain.storage.service.StorageManager
import java.io.BufferedReader
import java.io.InputStreamReader

class AiManager(
    private val context: Context,
    private val networkHelper: NetworkHelper = Injekt.get(),
    private val aiPreferences: AiPreferences = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val getLibraryAnime: tachiyomi.domain.anime.interactor.GetLibraryAnime = Injekt.get(),
    private val json: Json = Injekt.get(),
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Circuit Breaker Config
    private val MAP_VERSION = 132
    private val REMOTE_KILL_SWITCH_URL = "https://raw.githubusercontent.com/salmanbappi/anikku-config/main/ai_kill_switch.json"

    fun resetCircuitBreaker() {
        aiPreferences.isCircuitBreakerTripped().set(false)
        aiPreferences.isRequestPending().set(false)
    }

    suspend fun chatWithAssistant(query: String, history: List<ChatMessage>): String? {
        if (!aiPreferences.enableAi().get() || !aiPreferences.enableAiAssistant().get()) return null
        
        // 1. Stability & Kill Switch Check
        if (isCircuitBreakerTripped()) {
            return "Stability Alert: AI temporarily disabled due to detected app instability. [RESET_REQUIRED]"
        }
        if (isRemoteKillSwitchActive()) {
            return "Service Maintenance: AI Assistant is currently offline."
        }

        val engine = aiPreferences.aiEngine().get()
        val apiKey = if (engine == "gemini") {
            aiPreferences.geminiApiKey().get()
        } else {
            aiPreferences.groqApiKey().get()
        }.ifBlank { return "Please set an API Key in Settings > Advanced Analytics" }

        val customPrompt = aiPreferences.aiSystemPrompt().get()
        val defaultSystemInstruction = """
            You are the 'AniZen System Assistant', a senior systems engineer.
            You have access to native diagnostic tools for logs, system maps, and the user's anime library.
            
            OPERATIONAL PROTOCOLS:
            1. FORMATTING: STRICTLY NO TABLES. Use bullet points or lists for structured data. NEVER output Markdown tables.
            2. SEMANTIC INTENT: Identify negative system states (e.g., "black screen", "crash", "stuck") and call get_system_diagnostics.
            3. GROUNDED NAVIGATION: Use get_app_navigation_guide. If a [STALENESS_WARNING] is present, inform the user that menu paths may have changed in their version.
            4. CRASH ANALYSIS: Prioritize "PINNED" blocks in logs as they contain the root cause of failures.
            5. LIBRARY AWARENESS: Use the [USER_LIBRARY_DATA] block to answer questions about the user's collection, recommendations, or statistics.
            6. PRIVACY: PII (Auth headers, Cookies, and URL params) is strictly redacted.
        """.trimIndent()
        
        val systemInstruction = if (customPrompt.isNotBlank()) customPrompt else defaultSystemInstruction

        val messages = history.toMutableList()
        messages.add(ChatMessage(role = "user", content = query))

        // 2. Track Request State (Backoff for crashes)
        aiPreferences.isRequestPending().set(true)
        
        val response = try {
            if (engine == "gemini") {
                callGeminiWithTools(messages, apiKey, systemInstruction)
            } else {
                callGroqWithTools(messages, apiKey, systemInstruction)
            }
        } finally {
            aiPreferences.isRequestPending().set(false)
            recordRequestSuccess()
        }
        
        return response
    }

    private suspend fun getLibrarySummary(): String {
        return try {
            val library = getLibraryAnime.await()
            if (library.isEmpty()) return "Library is empty."
            
            library.joinToString("\n") { anime ->
                "- ${anime.anime.title} [Status: ${anime.anime.status}, Episodes: ${anime.totalEpisodes}, Seen: ${anime.seenCount}, Score: ${anime.anime.score}]"
            }
        } catch (e: Exception) {
            "Failed to retrieve library: ${e.message}"
        }
    }

    suspend fun getStatisticsAnalysis(statsSummary: String): String? {
        if (!aiPreferences.enableAi().get() || !aiPreferences.enableAiStatistics().get()) return null
        
        // Circuit Breaker for Stats too
        if (isCircuitBreakerTripped()) return null

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

        aiPreferences.isRequestPending().set(true)
        val response = try {
            if (engine == "gemini") {
                callGemini(listOf(ChatMessage(role = "user", content = prompt)), apiKey, "You are a senior behavioral data analyst.")
            } else {
                callGroq(listOf(ChatMessage(role = "user", content = prompt)), apiKey, "You are a senior behavioral data analyst.")
            }
        } finally {
            aiPreferences.isRequestPending().set(false)
            recordRequestSuccess()
        }
        return response
    }

    private fun isCircuitBreakerTripped(): Boolean {
        // If the app crashed during the last request, trip the breaker
        if (aiPreferences.isRequestPending().get()) {
            aiPreferences.isCircuitBreakerTripped().set(true)
            return true
        }
        return aiPreferences.isCircuitBreakerTripped().get()
    }

    private suspend fun isRemoteKillSwitchActive(): Boolean = withIOContext {
        try {
            val request = Request.Builder().url(REMOTE_KILL_SWITCH_URL).build()
            networkHelper.client.newCall(request).execute().use {
                if (it.isSuccessful) {
                    val body = it.body.string()
                    body.contains("\"disabled\": true")
                } else false
            }
        } catch (e: Exception) {
            false // Default to enabled if network fails
        }
    }

    private fun recordRequestSuccess() {
        val count = aiPreferences.hourlyAiRequestCount().get()
        aiPreferences.hourlyAiRequestCount().set(count + 1)
        aiPreferences.lastAiRequestTime().set(System.currentTimeMillis())
    }

    private fun getSanitizedLogs(): String {
        return try {
            val logLines = mutableListOf<String>()
            
            // 1. Try Logcat (may fail on Android 13+)
            val process = Runtime.getRuntime().exec("logcat -d -b main -t 500 *:W")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            while (true) {
                val line = reader.readLine() ?: break
                logLines.add(line)
            }

            // 2. Fallback to XLog files if logcat is empty
            if (logLines.size < 5) {
                val storageManager = Injekt.get<StorageManager>()
                val logDir = storageManager.getLogsDirectory()
                val latestLog = logDir?.listFiles()
                    ?.filter { it.isFile && it.name?.endsWith(".log") == true }
                    ?.maxByOrNull { it.lastModified() }
                
                if (latestLog != null) {
                    latestLog.openInputStream().bufferedReader().useLines { lines ->
                        logLines.addAll(lines.takeLast(500).toList())
                    }
                }
            }

            if (logLines.isEmpty()) return "No system logs available. (Permission restricted)"

            val pinnedBlocks = mutableListOf<List<String>>()
            val currentBlock = mutableListOf<String>()
            
            val packagePattern = "(eu\\.kanade|app\\.anizen|mpv|ffmpeg|AndroidRuntime|libc|DEBUG|System\\.err|XLog|FileUtils)".toRegex()
            val piiRedaction = "(?i)(?:authorization|cookie|set-cookie):\\s*[^\\n\\r]+|(?<=\\?|&)[^=]+=[^&\\s]*|(?:[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})|(?:auth|token|key|password|secret|sid|session)=[a-zA-Z0-9._-]+".toRegex()
            val traceTrigger = "Exception|Error|Fatal signal|SIGSEGV|abort\\(\\)|Native crash|FAILED".toRegex(RegexOption.IGNORE_CASE)
            
            var lastLine = ""
            var repeatCount = 0
            
            for (line in logLines) {
                val sanitizedLine = line.replace(piiRedaction, "[REDACTED]")
                
                val isTraceLine = sanitizedLine.trimStart().startsWith("at ") || 
                                 sanitizedLine.contains("Caused by:") || 
                                 sanitizedLine.contains("#\\d+ pc ".toRegex())

                if (sanitizedLine.contains(traceTrigger) || (isTraceLine && currentBlock.isNotEmpty())) {
                    currentBlock.add(sanitizedLine)
                    if (currentBlock.size > 80) {
                        pinnedBlocks.add(currentBlock.toList())
                        currentBlock.clear()
                    }
                } else {
                    if (currentBlock.isNotEmpty()) {
                        pinnedBlocks.add(currentBlock.toList())
                        currentBlock.clear()
                    }
                    
                    if (sanitizedLine.contains(packagePattern)) {
                        if (sanitizedLine == lastLine) {
                            repeatCount++
                        } else {
                            if (repeatCount > 0) logLines.add("... [TRUNCATED] repeated $repeatCount times ...")
                            logLines.add(sanitizedLine)
                            lastLine = sanitizedLine
                            repeatCount = 0
                        }
                    }
                }
            }
            if (currentBlock.isNotEmpty()) pinnedBlocks.add(currentBlock.toList())
            if (repeatCount > 0) logLines.add("... [TRUNCATED] repeated $repeatCount times ...")
            
            val output = StringBuilder()
            if (pinnedBlocks.isNotEmpty()) {
                output.append("\n### CRITICAL SYSTEM EVENTS (PINNED):\n")
                pinnedBlocks.takeLast(2).forEach { output.append(it.joinToString("\n")).append("\n---\n") }
            }
            output.append("\n### SYSTEM LOG TAIL:\n")
            output.append(logLines.takeLast(60).joinToString("\n"))
            output.toString()
        } catch (e: Exception) {
            "Diagnostic retrieval failed: ${e.message}"
        }
    }

    private fun getAppMap(): String {
        val currentVersion = BuildConfig.VERSION_CODE
        val stalenessWarning = if (currentVersion != MAP_VERSION) {
            "[STALENESS_WARNING]: Navigation map version ($MAP_VERSION) differs from App Version ($currentVersion). Paths may be shifted.\n"
        } else ""

        return stalenessWarning + """
            - General: Settings > General
            - Appearance: Settings > Appearance (Theme, Monet, Dark Mode)
            - Library: Settings > Library (Update intervals, Columns)
            - Player: Settings > Player (Shaders/Anime4K, Orientation, Subtitles, External Player)
            - Downloads: Settings > Downloads (Threads, Cache)
            - Tracking: Settings > Tracking (Anilist, MAL)
            - Advanced: Settings > Advanced (Log viewer, Cache, Database)
            - Analytics: Settings > Advanced Analytics (AI Config)
        """.trimIndent()
    }

    private fun getExtensionStatusSummary(): String {
        val installed = extensionManager.installedExtensionsFlow.value
        return if (installed.isEmpty()) "No extensions installed."
        else installed.joinToString("\n") { "- ${it.name} (${it.pkgName}) v${it.versionName} [Obsolete: ${it.isObsolete}, Update: ${it.hasUpdate}]" }
    }

    private suspend fun callGeminiWithTools(messages: List<ChatMessage>, apiKey: String, systemInstruction: String): String? {
        val lastQuery = messages.last().content.lowercase()
        val toolContext = StringBuilder()
        
        if (lastQuery.contains("""log|error|fail|video|load|setting|where|how|device|black|broke|froze|slow|crash|die|dead""".toRegex())) {
            toolContext.append("\n[DIAGNOSTICS_DATA]:\n${getSanitizedLogs()}\n")
            toolContext.append("\n[NAVIGATION_MAP]:\n${getAppMap()}\n")
            toolContext.append("\n[EXTENSIONS_STATUS]:\n${getExtensionStatusSummary()}\n")
            toolContext.append("\n[ENVIRONMENT]: ${getDeviceInfo()}\n")
        }

        if (lastQuery.contains("""library|anime|watch|collection|have|my|list|recommend""".toRegex())) {
            toolContext.append("\n[USER_LIBRARY_DATA]:\n${getLibrarySummary()}\n")
        }
        
        val finalMessages = messages.dropLast(1) + ChatMessage("user", messages.last().content + "\n\n" + toolContext.toString())
        return callGemini(finalMessages, apiKey, systemInstruction)
    }

    private suspend fun callGroqWithTools(messages: List<ChatMessage>, apiKey: String, systemInstruction: String): String? {
        val lastQuery = messages.last().content.lowercase()
        val toolContext = StringBuilder()
        if (lastQuery.contains("""log|error|fail|video|load|setting|where|how|device|black|broke|froze|slow|crash""".toRegex())) {
            toolContext.append("\n[DIAGNOSTICS_DATA]:\n${getSanitizedLogs()}\n")
        }
        if (lastQuery.contains("""library|anime|watch|collection|have|my|list|recommend""".toRegex())) {
            toolContext.append("\n[USER_LIBRARY_DATA]:\n${getLibrarySummary()}\n")
        }
        val finalMessages = messages.dropLast(1) + ChatMessage("user", messages.last().content + "\n\n" + toolContext.toString())
        return callGroq(finalMessages, apiKey, systemInstruction)
    }

    private fun getDeviceInfo(): String = "Model: ${android.os.Build.MODEL}, SDK: ${android.os.Build.VERSION.SDK_INT}, App: AniZen"

    suspend fun getErrorCount(): Int = withIOContext {
        try {
            val logLines = mutableListOf<String>()
            val process = Runtime.getRuntime().exec("logcat -d -b main -t 200 *:E")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            while (true) {
                val line = reader.readLine() ?: break
                logLines.add(line)
            }

            // Fallback to files if logcat empty
            if (logLines.isEmpty()) {
                val storageManager = Injekt.get<StorageManager>()
                val logDir = storageManager.getLogsDirectory()
                val latestLog = logDir?.listFiles()
                    ?.filter { it.isFile && it.name?.endsWith(".log") == true }
                    ?.maxByOrNull { it.lastModified() }
                
                if (latestLog != null) {
                    latestLog.openInputStream().bufferedReader().useLines { lines ->
                        logLines.addAll(lines.takeLast(200).toList())
                    }
                }
            }

            var count = 0
            val criticalPatterns = listOf("FATAL EXCEPTION", "NullPointerException", "IllegalStateException", "OutOfMemoryError", "SecurityException", "ANR in", "Crash", "eu.kanade", "app.anizen", "mpv", "ffmpeg", "OkHttp", "Fatal signal", "SIGSEGV")
            val noiseTags = listOf("GmsClient", "MemoryLeakDetector", "AccessibilityBridge", "Surface", "BufferQueue", "SensorManager")
            
            for (line in logLines) {
                if (criticalPatterns.any { line.contains(it, ignoreCase = true) } && !noiseTags.any { line.contains(it, ignoreCase = true) }) {
                    count++
                }
            }
            count
        } catch (e: Exception) { 0 }
    }

    private suspend fun callGemini(messages: List<ChatMessage>, apiKey: String, systemInstruction: String? = null): String? = withIOContext {
        val geminiContents = messages.map { msg ->
            GeminiContent(parts = listOf(GeminiPart(text = msg.content)), role = if (msg.role == "user") "user" else "model")
        }
        val requestBody = GeminiRequest(
            contents = geminiContents, 
            systemInstruction = systemInstruction?.let { GeminiContent(parts = listOf(GeminiPart(text = it))) },
            safetySettings = listOf(
                GeminiSafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_NONE"),
                GeminiSafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_NONE"),
                GeminiSafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_NONE"),
                GeminiSafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_NONE")
            )
        )
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=$apiKey")
            .header("Content-Type", "application/json")
            .post(json.encodeToString(GeminiRequest.serializer(), requestBody).toRequestBody(jsonMediaType))
            .build()
        try {
            networkHelper.client.newCall(request).execute().use {
                val bodyString = it.body.string()
                if (!it.isSuccessful) return@withIOContext "Error ${it.code}: ${it.message}"
                val geminiResponse = json.decodeFromString(GeminiResponse.serializer(), bodyString)
                geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            }
        } catch (e: Exception) { "Exception: ${e.message}" }
    }

    private suspend fun callGroq(messages: List<ChatMessage>, apiKey: String, systemInstruction: String? = null): String? = withIOContext {
        val groqMessages = mutableListOf<GroqMessage>()
        if (systemInstruction != null) groqMessages.add(GroqMessage(role = "system", content = systemInstruction))
        messages.forEach { msg -> groqMessages.add(GroqMessage(role = if (msg.role == "user") "user" else "assistant", content = msg.content)) }
        val requestBody = GroqRequest(messages = groqMessages, model = "groq/compound-mini")
        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(json.encodeToString(GroqRequest.serializer(), requestBody).toRequestBody(jsonMediaType))
            .build()
        try {
            networkHelper.client.newCall(request).execute().use {
                val bodyString = it.body.string()
                if (!it.isSuccessful) return@withIOContext "Groq Error ${it.code}: ${it.message}\n$bodyString"
                val groqResponse = json.decodeFromString(GroqResponse.serializer(), bodyString)
                groqResponse.choices.firstOrNull()?.message?.content?.trim()
            }
        } catch (e: Exception) { "Groq Exception: ${e.message}" }
    }

    @Serializable
    data class ChatMessage(val role: String, val content: String)

    @Serializable
    private data class GeminiRequest(
        val contents: List<GeminiContent>, 
        @kotlinx.serialization.SerialName("system_instruction") val systemInstruction: GeminiContent? = null,
        val safetySettings: List<GeminiSafetySetting>? = null
    )

    @Serializable
    private data class GeminiSafetySetting(
        val category: String,
        val threshold: String
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
    private data class GroqRequest(val messages: List<GroqMessage>, val model: String)

    @Serializable
    private data class GroqMessage(val role: String, val content: String)

    @Serializable
    private data class GroqResponse(val choices: List<GroqChoice>)

    @Serializable
    private data class GroqChoice(val message: GroqMessage)
}
