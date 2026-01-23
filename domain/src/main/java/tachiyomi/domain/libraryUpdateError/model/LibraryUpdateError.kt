package tachiyomi.domain.libraryUpdateError.model

import java.io.Serializable

data class LibraryUpdateError(
    val id: Long,
    val animeId: Long,
    val messageId: Long,
) : Serializable
