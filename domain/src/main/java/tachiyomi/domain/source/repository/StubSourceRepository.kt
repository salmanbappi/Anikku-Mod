package tachiyomi.domain.source.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.StubSource

interface StubSourceRepository {
    fun subscribeAllAnime(): Flow<List<StubSource>>

    suspend fun getStubAnimeSource(id: Long): StubSource?

    suspend fun upsertStubAnimeSource(id: Long, lang: String, name: String)
}
