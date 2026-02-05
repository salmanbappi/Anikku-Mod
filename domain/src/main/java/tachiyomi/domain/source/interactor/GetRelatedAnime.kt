package tachiyomi.domain.source.interactor

import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.toSAnime
import tachiyomi.domain.source.service.SourceManager

class GetRelatedAnime(
    private val sourceManager: SourceManager,
) {
    fun subscribe(anime: Anime) = callbackFlow {
        val source = sourceManager.get(anime.source) as? CatalogueSource ?: run {
            close()
            return@callbackFlow
        }
        try {
            source.getRelatedAnimeList(
                anime = anime.toSAnime(),
                exceptionHandler = { e ->
                    if (e is UnsupportedOperationException) {
                        close()
                    } else {
                        close(e)
                    }
                },
                pushResults = { relatedAnime, completed ->
                    trySend(relatedAnime)
                    if (completed) close()
                },
            )
        } catch (e: UnsupportedOperationException) {
            close()
        } catch (e: Exception) {
            close(e)
        }
        awaitClose()
    }
}
