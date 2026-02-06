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

            // 2. Fallback search logic if source provides nothing
            if (!suggestionsFound || state.value.items.isEmpty()) {
                val source = sourceManager.get(anime.source) as? AnimeCatalogueSource ?: return@launchIO
                val query = anime.genre?.firstOrNull() ?: anime.title.split(" ").take(2).joinToString(" ")
                
                try {
                    val searchResult = source.getSearchAnime(1, query, source.getFilterList())
                    val domainAnimes = searchResult.animes
                        .filter { it.url != anime.url }
                        .sortedByDescending { calculateJaroWinklerSimilarity(anime.title, it.title) }
                        .map { sAnime ->
                            async {
                                val localAnime = networkToLocalAnime.await(sAnime.toDomainAnime(anime.source))
                                getAnime.await(localAnime.id)
                            }
                        }.awaitAll().filterNotNull()
                    
                    if (domainAnimes.isNotEmpty()) {
                        mutableState.update { state ->
                            state.copy(
                                items = state.items.put("Recommended", domainAnimes.toImmutableList()),
                            )
                        }
                    }
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e)
                }
            }
        }
    }

    private fun calculateJaroWinklerSimilarity(s1: String, s2: String): Double {
        val m = 0.1
        val jaro = calculateJaroSimilarity(s1, s2)
        val prefix = s1.commonPrefixWith(s2).length.coerceAtMost(4)
        return jaro + (prefix * m * (1 - jaro))
    }

    private fun calculateJaroSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        val len1 = s1.length
        val len2 = s2.length
        if (len1 == 0 || len2 == 0) return 0.0
        
        val matchDistance = (kotlin.math.max(len1, len2) / 2) - 1
        val s1Matches = BooleanArray(len1)
        val s2Matches = BooleanArray(len2)
        var matches = 0.0
        var transpositions = 0.0

        for (i in 0 until len1) {
            val start = kotlin.math.max(0, i - matchDistance)
            val end = kotlin.math.min(i + matchDistance + 1, len2)
            for (j in start until end) {
                if (s2Matches[j]) continue
                if (s1[i] != s2[j]) continue
                s1Matches[i] = true
                s2Matches[j] = true
                matches++
                break
            }
        }
        if (matches == 0.0) return 0.0

        var k = 0
        for (i in 0 until len1) {
            if (!s1Matches[i]) continue
            while (!s2Matches[k]) k++
            if (s1[i] != s2[k]) transpositions++
            k++
        }

        return ((matches / len1) + (matches / len2) + ((matches - transpositions / 2) / matches)) / 3.0
    }

    @Immutable
    data class State(
        val title: String = "",
        val items: PersistentMap<String, ImmutableList<Anime>> = persistentMapOf(),
    )
}