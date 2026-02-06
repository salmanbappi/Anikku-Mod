package eu.kanade.tachiyomi.ui.more.settings.screen.ai

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.ai.AiPreferences
import eu.kanade.tachiyomi.data.ai.AiManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.ai.model.ChatMessage
import tachiyomi.domain.ai.model.ChatSession
import tachiyomi.domain.ai.repository.ChatRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant

class AiAssistantScreenModel(
    private val chatRepository: ChatRepository = Injekt.get(),
    private val aiPreferences: AiPreferences = Injekt.get(),
    private val aiManager: AiManager = Injekt.get(),
) : StateScreenModel<AiAssistantScreenModel.State>(State()) {

    val sessions: StateFlow<ImmutableList<ChatSession>> = chatRepository.getSessions()
        .map { it.toImmutableList() }
        .stateIn(screenModelScope, SharingStarted.Lazily, persistentListOf())

    init {
        screenModelScope.launchIO {
            aiPreferences.activeSessionId().get().let { sessionId ->
                if (sessionId != -1L) {
                    loadSession(sessionId)
                } else {
                    createNewSession()
                }
            }
        }
    }

    private fun loadSession(sessionId: Long) {
        screenModelScope.launchIO {
            chatRepository.getMessagesBySessionId(sessionId).collectLatest { messages ->
                mutableState.update { it.copy(activeSessionId = sessionId, messages = messages.toImmutableList()) }
            }
        }
    }

    fun createNewSession() {
        screenModelScope.launchIO {
            val sessionId = chatRepository.insertSession("New Analytic Session")
            aiPreferences.activeSessionId().set(sessionId)
            loadSession(sessionId)
        }
    }

    fun switchSession(sessionId: Long) {
        aiPreferences.activeSessionId().set(sessionId)
        loadSession(sessionId)
    }

    fun deleteSession(sessionId: Long) {
        screenModelScope.launchIO {
            chatRepository.deleteSession(sessionId)
            if (state.value.activeSessionId == sessionId) {
                aiPreferences.activeSessionId().set(-1L)
                createNewSession()
            }
        }
    }

    fun sendMessage(query: String) {
        val sessionId = state.value.activeSessionId ?: return
        if (query.isBlank() || state.value.isLoading) return

        screenModelScope.launchIO {
            // 1. Save User Message
            chatRepository.insertMessage(sessionId, "user", query)
            
            // 2. Update Loading State
            mutableState.update { it.copy(isLoading = true) }

            // 3. Call AI
            val history = state.value.messages.map { AiManager.ChatMessage(it.role, it.content) }
            val response = aiManager.chatWithAssistant(query, history)

            // 4. Save AI Response
            if (response != null) {
                chatRepository.insertMessage(sessionId, "model", response)
                
                // 5. Update Session Title if it's the first message
                if (state.value.messages.size <= 2) {
                    updateSessionTitle(sessionId, query)
                }
            } else {
                chatRepository.insertMessage(sessionId, "model", "Neural link unstable. Check connection or API Key.")
            }

            mutableState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun updateSessionTitle(sessionId: Long, query: String) {
        val title = if (query.length > 30) query.take(27) + "..." else query
        chatRepository.updateSessionTitle(sessionId, title)
    }

    fun clearAllSessions() {
        screenModelScope.launchIO {
            chatRepository.deleteAllSessions()
            aiPreferences.activeSessionId().set(-1L)
            createNewSession()
        }
    }

    data class State(
        val activeSessionId: Long? = null,
        val messages: ImmutableList<ChatMessage> = persistentListOf(),
        val isLoading: Boolean = false,
    )
}
