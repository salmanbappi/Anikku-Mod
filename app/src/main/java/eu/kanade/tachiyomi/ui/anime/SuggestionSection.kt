package eu.kanade.tachiyomi.ui.anime

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.domain.anime.model.Anime
import java.io.Serializable

@Immutable
data class SuggestionSection(
    val title: String,
    val items: ImmutableList<Anime>,
    val type: Type,
) : Serializable {
    enum class Type {
        Franchise,
        Similarity,
        Tag,
        Source,
        Author,
        Community,
    }
}
