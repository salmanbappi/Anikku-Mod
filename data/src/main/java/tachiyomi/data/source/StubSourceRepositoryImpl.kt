package tachiyomi.data.source

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.repository.StubSourceRepository

class StubSourceRepositoryImpl(
    private val handler: DatabaseHandler,
) : StubSourceRepository {

    override fun subscribeAllAnime(): Flow<List<StubSource>> {
        return handler.subscribeToList { sourcesQueries.findAll(::mapStubSource) }
    }

    override suspend fun getStubAnimeSource(id: Long): StubSource? {
        return handler.awaitOneOrNull {
            sourcesQueries.findOne(
                id,
                ::mapStubSource,
            )
        }
    }

    override suspend fun upsertStubAnimeSource(id: Long, lang: String, name: String) {
        handler.await { sourcesQueries.upsert(id, lang, name) }
    }

    private fun mapStubSource(
        id: Long,
        lang: String,
        name: String,
    ): StubSource = StubSource(id = id, lang = lang, name = name)
}
