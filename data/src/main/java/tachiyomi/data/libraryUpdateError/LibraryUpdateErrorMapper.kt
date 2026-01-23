package tachiyomi.data.libraryUpdateError

import tachiyomi.domain.libraryUpdateError.model.LibraryUpdateError

val libraryUpdateErrorMapper: (Long, Long, Long) -> LibraryUpdateError = { id, animeId, messageId ->
    LibraryUpdateError(
        id = id,
        animeId = animeId,
        messageId = messageId,
    )
}
