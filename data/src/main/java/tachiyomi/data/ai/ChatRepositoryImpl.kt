package tachiyomi.data.ai

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.ai.model.ChatMessage
import tachiyomi.domain.ai.model.ChatSession
import tachiyomi.domain.ai.repository.ChatRepository
import java.time.Instant

class ChatRepositoryImpl(
    private val handler: DatabaseHandler,
) : ChatRepository {

    override fun getSessions(): Flow<List<ChatSession>> {
        return handler.subscribeToList { ai_chatQueries.getSessions(::mapSession) }
    }

    override suspend fun getSessionById(id: Long): ChatSession? {
        return handler.awaitOneOrNull { ai_chatQueries.getSessionById(id, ::mapSession) }
    }

    override fun getMessagesBySessionId(sessionId: Long): Flow<List<ChatMessage>> {
        return handler.subscribeToList { ai_chatQueries.getMessagesBySessionId(sessionId, ::mapMessage) }
    }

    override suspend fun insertSession(title: String): Long {
        return handler.awaitOneExecutable {
            ai_chatQueries.insertSession(title, Instant.now().toEpochMilli())
            ai_chatQueries.selectLastInsertedRowId()
        }
    }

    override suspend fun updateSessionTitle(id: Long, title: String) {
        handler.await { ai_chatQueries.updateSessionTitle(title, id) }
    }

    override suspend fun updateSessionLastMessageAt(id: Long, lastMessageAt: Long) {
        handler.await { ai_chatQueries.updateSessionLastMessageAt(lastMessageAt, id) }
    }

    override suspend fun updateSessionPinned(id: Long, isPinned: Boolean) {
        handler.await { ai_chatQueries.updateSessionPinned(if (isPinned) 1L else 0L, id) }
    }

    override suspend fun insertMessage(sessionId: Long, role: String, content: String) {
        val now = Instant.now().toEpochMilli()
        handler.await(inTransaction = true) {
            ai_chatQueries.insertMessage(sessionId, role, content, now)
            ai_chatQueries.updateSessionLastMessageAt(now, sessionId)
        }
    }

    override suspend fun deleteSession(id: Long) {
        handler.await { ai_chatQueries.deleteSession(id) }
    }

    override suspend fun deleteSessions(ids: List<Long>) {
        handler.await { ai_chatQueries.deleteSessions(ids) }
    }

    override suspend fun deleteAllSessions() {
        handler.await { ai_chatQueries.deleteAllSessions() }
    }

    private fun mapSession(_id: Long, title: String, last_message_at: Long, is_pinned: Long): ChatSession {
        return ChatSession(_id, title, last_message_at, is_pinned == 1L)
    }

    private fun mapMessage(_id: Long, session_id: Long, role: String, content: String, created_at: Long): ChatMessage {
        return ChatMessage(_id, session_id, role, content, created_at)
    }
}
