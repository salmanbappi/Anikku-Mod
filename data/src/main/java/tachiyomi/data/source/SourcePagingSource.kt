package tachiyomi.data.source

import androidx.paging.PagingState
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.episode.model.NoEpisodesException
import tachiyomi.domain.source.repository.AnimeSourcePagingSourceType

class SourceSearchPagingSource(
    source: AnimeCatalogueSource,
    val query: String,
    val filters: AnimeFilterList,
) : SourcePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): AnimesPage {
        return source.getSearchAnime(currentPage, query, filters)
    }
}

class SourcePopularPagingSource(source: AnimeCatalogueSource) : SourcePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): AnimesPage {
        return source.getPopularAnime(currentPage)
    }
}

class SourceLatestPagingSource(source: AnimeCatalogueSource) : SourcePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): AnimesPage {
        return source.getLatestUpdates(currentPage)
    }
}

abstract class SourcePagingSource(
    protected val source: AnimeCatalogueSource,
) : AnimeSourcePagingSourceType() {

    abstract suspend fun requestNextPage(currentPage: Int): AnimesPage

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, SAnime> {
        val page = params.key ?: 1

        val animesPage = try {
            withIOContext {
                requestNextPage(page.toInt())
                    .takeIf { it.animes.isNotEmpty() }
                    ?: throw NoEpisodesException()
            }
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }

        return LoadResult.Page(
            data = animesPage.animes,
            prevKey = null,
            nextKey = if (animesPage.hasNextPage) page + 1 else null,
        )
    }

    override fun getRefreshKey(state: PagingState<Long, SAnime>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }
}
