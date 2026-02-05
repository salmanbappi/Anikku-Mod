package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.anime.model.toDomainAnime
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.source.interactor.GetRelatedAnime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RelatedAnimeScreenModel(
    private val animeId: Long,
    private val getRelatedAnime: GetRelatedAnime = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
) : StateScreenModel<RelatedAnimeScreenModel.State>(State()) {

    init {
        screenModelScope.launchIO {
            val anime = getAnime.await(animeId) ?: return@launchIO
            mutableState.update { it.copy(title = anime.title) }

            getRelatedAnime.subscribe(anime).collectLatest { (keyword, animes) ->
                val domainAnimes = animes.map {
                    val localAnime = networkToLocalAnime.await(it.toDomainAnime(anime.source))
                    getAnime.await(localAnime.id)!!
                }
                mutableState.update { state ->
                    state.copy(
                        items = state.items.put(keyword, domainAnimes.toImmutableList()),
                    )
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
