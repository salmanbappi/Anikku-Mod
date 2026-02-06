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
import kotlinx.coroutines.flow.distinctUntilChanged
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

    val sessions: StateFlow<ImmutableList<ChatSession>> = combine(
        chatRepository.getSessions(),
        state.map { it.activeSessionId }.distinctUntilChanged()
    ) { list, activeSessionId ->
        list.filter { it.messageCount > 0 || it.id == activeSessionId }
            .toImmutableList()
    }
    .stateIn(screenModelScope, SharingStarted.Lazily, persistentListOf())

    init {
        screenModelScope.launchIO {
            aiPreferences.activeSessionId().get().let { sessionId ->
                if (sessionId != -1L) {
                    val session = chatRepository.getSessionById(sessionId)
                    if (session != null) {
                        loadSession(sessionId)
                    } else {
                        createNewSession()
                    }
                } else {
                    createNewSession()
                }
            }
        }

        // Auto-create session if list becomes empty (after filtering)
        screenModelScope.launchIO {
            sessions.collectLatest { list ->
                if (list.isEmpty()) {
                    createNewSession()
                }
            }
        }
        
        // Cleanup truly empty sessions on start (except active one)
        screenModelScope.launchIO {
            chatRepository.getSessions().collectLatest { list ->
                val activeId = aiPreferences.activeSessionId().get()
                val emptySessionIds = list
                    .filter { it.messageCount == 0L && it.id != activeId }
                    .map { it.id }
                if (emptySessionIds.isNotEmpty()) {
                    chatRepository.deleteSessions(emptySessionIds)
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
            // Find existing empty session to reuse instead of creating a new one
            val existingEmpty = sessions.value.firstOrNull { it.messageCount == 0L }

            if (existingEmpty != null) {
                switchSession(existingEmpty.id)
                return@launchIO
            }

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
                // The sessions collector will trigger createNewSession if empty
            }
        }
    }

    fun deleteSessions(sessionIds: List<Long>) {
        screenModelScope.launchIO {
            chatRepository.deleteSessions(sessionIds)
            if (state.value.activeSessionId in sessionIds) {
                aiPreferences.activeSessionId().set(-1L)
                // The sessions collector will trigger createNewSession if empty
            }
        }
    }

    fun toggleSessionPin(sessionId: Long, isPinned: Boolean) {
        screenModelScope.launchIO {
            chatRepository.updateSessionPinned(sessionId, !isPinned)
        }
    }

    fun sendMessage(query: String) {
        val sessionId = state.value.activeSessionId ?: return
        if (query.isBlank() || state.value.isLoading) return

        screenModelScope.launchIO {
            // Check if session still exists
            if (chatRepository.getSessionById(sessionId) == null) {
                createNewSession()
                return@launchIO
            }

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
                
                // 5. Update Session Title if it's the first message from human
                // Note: state.value.messages already includes the user query and potentially the AI response
                val userMessages = state.value.messages.filter { it.role == "user" }
                if (userMessages.size == 1) {
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

    fun toggleSessionSelection(sessionId: Long) {
        mutableState.update { state ->
            val newSelection = if (state.selectedSessionIds.contains(sessionId)) {
                state.selectedSessionIds - sessionId
            } else {
                state.selectedSessionIds + sessionId
            }
            state.copy(selectedSessionIds = newSelection)
        }
    }

    fun clearSessionSelection() {
        mutableState.update { it.copy(selectedSessionIds = emptySet()) }
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
        val selectedSessionIds: Set<Long> = emptySet(),
    )
}
