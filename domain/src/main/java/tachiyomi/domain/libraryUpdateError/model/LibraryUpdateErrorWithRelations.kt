package tachiyomi.domain.libraryUpdateError.model

import tachiyomi.domain.anime.model.AnimeCover

data class LibraryUpdateErrorWithRelations(
    val animeId: Long,
    val animeTitle: String,
    val animeSource: Long,
    val animeCover: AnimeCover,
    val errorId: Long,
    val messageId: Long,
)
