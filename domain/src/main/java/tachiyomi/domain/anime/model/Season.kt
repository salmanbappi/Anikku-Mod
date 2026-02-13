package tachiyomi.domain.anime.model

data class Season(
    val anime: Anime,
    val seasonNumber: Double,
    val isPrimary: Boolean = false,
)
