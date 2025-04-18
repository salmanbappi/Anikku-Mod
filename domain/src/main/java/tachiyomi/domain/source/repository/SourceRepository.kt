package tachiyomi.domain.source.repository

import androidx.paging.PagingSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.SourceWithCount

typealias AnimeSourcePagingSourceType = PagingSource<Long, SAnime>

interface SourceRepository {

    fun getAnimeSources(): Flow<List<Source>>

    fun getOnlineAnimeSources(): Flow<List<Source>>

    fun getAnimeSourcesWithFavoriteCount(): Flow<List<Pair<Source, Long>>>

    fun getSourcesWithNonLibraryAnime(): Flow<List<SourceWithCount>>

    fun searchAnime(sourceId: Long, query: String, filterList: AnimeFilterList): AnimeSourcePagingSourceType

    fun getPopularAnime(sourceId: Long): AnimeSourcePagingSourceType

    fun getLatestAnime(sourceId: Long): AnimeSourcePagingSourceType
}
