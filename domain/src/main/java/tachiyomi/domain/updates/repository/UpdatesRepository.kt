package tachiyomi.domain.updates.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.updates.model.UpdatesWithRelations

interface UpdatesRepository {

    suspend fun awaitWithSeen(seen: Boolean, after: Long, limit: Long): List<UpdatesWithRelations>

    fun subscribeAllAnimeUpdates(after: Long, limit: Long): Flow<List<UpdatesWithRelations>>

    fun subscribeWithSeen(seen: Boolean, after: Long, limit: Long): Flow<List<UpdatesWithRelations>>
}
