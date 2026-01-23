package eu.kanade.tachiyomi.ui.browse.migration.search

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.model.Anime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateSearchScreenDialogScreenModel(
    val animeId: Long,
    getAnime: GetAnime = Injekt.get(),
) : StateScreenModel<MigrateSearchScreenDialogScreenModel.State>(State()) {

    init {
        screenModelScope.launch {
            val anime = getAnime.await(animeId)!!

            mutableState.update {
                it.copy(anime = anime)
            }
        }
    }

    fun setDialog(dialog: Dialog?) {
        mutableState.update {
            it.copy(dialog = dialog)
        }
    }

    @Immutable
    data class State(
        val anime: Anime? = null,
        val dialog: Dialog? = null,
    )

    sealed interface Dialog {
        data class Migrate(val anime: Anime) : Dialog
    }
}
