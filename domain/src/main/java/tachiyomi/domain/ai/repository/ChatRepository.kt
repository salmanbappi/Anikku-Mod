package tachiyomi.domain.ai.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.ai.model.ChatSession
import tachiyomi.domain.ai.model.ChatMessage

interface ChatRepository {
    fun getSessions(): Flow<List<ChatSession>>
    suspend fun getSessionById(id: Long): ChatSession?
    fun getMessagesBySessionId(sessionId: Long): Flow<List<ChatMessage>>
    suspend fun insertSession(title: String): Long
    suspend fun updateSessionTitle(id: Long, title: String)
    suspend fun updateSessionLastMessageAt(id: Long, lastMessageAt: Long)
    suspend fun insertMessage(sessionId: Long, role: String, content: String)
    suspend fun deleteSession(id: Long)
    suspend fun deleteAllSessions()
}
