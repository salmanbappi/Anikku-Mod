package eu.kanade.tachiyomi.ui.anime

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.data.download.model.Download
import tachiyomi.domain.episode.model.Episode
import java.io.Serializable

@Immutable
sealed class EpisodeList : Serializable {

    @Immutable data class MissingCount(val id: String, val count: Int) : EpisodeList()

    @Immutable data class Item(
        val episode: Episode,
        val downloadState: Download.State,
        val downloadProgress: Int,
        var fileSize: Long? = null,
        val selected: Boolean = false,
    ) : EpisodeList() {

        val id = episode.id

        val isDownloaded = downloadState == Download.State.DOWNLOADED
    }
}
