package tachiyomi.domain.category.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.anime.repository.AnimeRepository

class SetAnimeCategories(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(mangaId: Long, categoryIds: List<Long>) {
        try {
            animeRepository.setAnimeCategories(mangaId, categoryIds)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
