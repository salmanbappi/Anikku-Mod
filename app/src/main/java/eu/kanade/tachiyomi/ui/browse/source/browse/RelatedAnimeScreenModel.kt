package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.source.AnimeCatalogueSource
import eu.kanade.tachiyomi.source.SourceManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.toDomainAnime
import tachiyomi.domain.source.interactor.GetRelatedAnime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RelatedAnimeScreenModel(
    private val animeId: Long,
    private val getRelatedAnime: GetRelatedAnime = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) : StateScreenModel<RelatedAnimeScreenModel.State>(State()) {

    init {
        screenModelScope.launchIO {
            val anime = getAnime.await(animeId) ?: return@launchIO
            mutableState.update { it.copy(title = anime.title) }

            var suggestionsFound = false

            // 1. Source-provided related anime
            getRelatedAnime.subscribe(anime).collect { (keyword, animes) ->
                if (animes.isNotEmpty()) {
                    val domainAnimes = animes.map {
                        async {
                            val localAnime = networkToLocalAnime.await(it.toDomainAnime(anime.source))
                            getAnime.await(localAnime.id)
                        }
                    }.awaitAll().filterNotNull()
                    
                    mutableState.update { state ->
                        state.copy(
                            items = state.items.put(keyword, domainAnimes.toImmutableList()),
                        )
                    }
                    suggestionsFound = true
                }
            }

            // 2. Hybrid Intelligence Fallback
            if (!suggestionsFound || state.value.items.isEmpty()) {
                val source = sourceManager.get(anime.source) as? AnimeCatalogueSource ?: return@launchIO
                
                val query = eu.kanade.tachiyomi.util.lang.StringSimilarity.getSearchKeywords(anime.title)
                if (query.isBlank()) return@launchIO

                try {
                    val searchResult = source.getSearchAnime(1, query, source.getFilterList())
                    val domainAnimes = searchResult.animes
                        .filter { it.url != anime.url }
                        .map { sAnime ->
                            async {
                                val localAnime = networkToLocalAnime.await(sAnime.toDomainAnime(anime.source))
                                val fullAnime = getAnime.await(localAnime.id) ?: return@async null
                                
                                val titleSim = eu.kanade.tachiyomi.util.lang.StringSimilarity.tokenSortRatio(anime.title, fullAnime.title)
                                val genreOverlap = if (!anime.genre.isNullOrEmpty() && !fullAnime.genre.isNullOrEmpty()) {
                                    val intersect = anime.genre!!.intersect(fullAnime.genre!!.toSet()).size
                                    intersect.toDouble() / anime.genre!!.size.coerceAtLeast(1)
                                } else 0.0
                                
                                // SMOOTH ADAPTIVE SCORING
                                val totalScore = eu.kanade.tachiyomi.util.lang.StringSimilarity.adaptiveScore(titleSim, genreOverlap)
                                
                                if (totalScore < 0.25) return@async null
                                fullAnime to totalScore
                            }
                        }.awaitAll().filterNotNull()
                        .sortedByDescending { it.second }
                        .map { it.first }
                        .take(24) // Show more in full screen
                    
                    if (domainAnimes.isNotEmpty()) {
                        mutableState.update { state ->
                            state.copy(
                                items = state.items.put("Recommended Intelligence", domainAnimes.toImmutableList()),
                            )
                        }
                    }
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e)
                }
            }
        }
    }

    @Immutable
    data class State(
        val title: String = "",
        val items: PersistentMap<String, ImmutableList<Anime>> = persistentMapOf(),
    )
}