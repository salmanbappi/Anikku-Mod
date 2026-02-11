package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.source.model.UpdateStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.CustomAnimeInfo

@Suppress("MagicNumber")
@Serializable
data class BackupAnime(
    // in 1.x some of these values have different names
    @ProtoNumber(1) var source: Long = 0,
    // url is called key in 1.x
    @ProtoNumber(2) var url: String = "",
    @ProtoNumber(3) var title: String = "",
    @ProtoNumber(4) var artist: String? = null,
    @ProtoNumber(5) var author: String? = null,
    @ProtoNumber(6) var description: String? = null,
    @ProtoNumber(7) var genre: List<String> = emptyList(),
    @ProtoNumber(8) var status: Int = 0,
    // thumbnailUrl is called cover in 1.x
    @ProtoNumber(9) var thumbnailUrl: String? = null,
    // @ProtoNumber(10) val customCover: String = "", 1.x value, not used in 0.x
    // @ProtoNumber(11) val lastUpdate: Long = 0, 1.x value, not used in 0.x
    // @ProtoNumber(12) val lastInit: Long = 0, 1.x value, not used in 0.x
    @ProtoNumber(13) var dateAdded: Long = 0,
    // @ProtoNumber(15) val flags: Int = 0, 1.x value, not used in 0.x
    @ProtoNumber(16) var episodes: List<BackupEpisode> = emptyList(),
    @ProtoNumber(17) var categories: List<Long> = emptyList(),
    @ProtoNumber(18) var tracking: List<BackupTracking> = emptyList(),
    // Bump by 100 for values that are not saved/implemented in 1.x but are used in 0.x
    @ProtoNumber(100) var favorite: Boolean = true,
    @ProtoNumber(101) var episodeFlags: Int = 0,
    // @ProtoNumber(102) var brokenHistory, legacy history model with non-compliant proto number
    @ProtoNumber(103) var viewer_flags: Int = 0,
    @ProtoNumber(104) var history: List<BackupHistory> = emptyList(),
    @ProtoNumber(105) var updateStrategy: UpdateStrategy = UpdateStrategy.ALWAYS_UPDATE,
    @ProtoNumber(106) var lastModifiedAt: Long = 0,
    @ProtoNumber(107) var favoriteModifiedAt: Long? = null,
    // Mihon values start here
    @ProtoNumber(109) var version: Long = 0,
    @ProtoNumber(110) var notes: String = "",
    @ProtoNumber(111) var initialized: Boolean = false,

    // AM (CUSTOM_INFORMATION) -->
    // Bump values by 200
    @ProtoNumber(200) var customStatus: Int = 0,
    @ProtoNumber(201) var customTitle: String? = null,
    @ProtoNumber(202) var customArtist: String? = null,
    @ProtoNumber(203) var customAuthor: String? = null,
    @ProtoNumber(204) var customDescription: String? = null,
    @ProtoNumber(205) var customGenre: List<String>? = null,
    // <-- AM (CUSTOM_INFORMATION)

    // AY -->
    // Aniyomi specific values
    @ProtoNumber(500) var backgroundUrl: String? = null,
    @ProtoNumber(502) var parentId: Long? = null,
    @ProtoNumber(503) var id: Long? = null, // Used to associate seasons with parents.
    // <-- AY

    // J2K specific values (kept for compatibility)
    @ProtoNumber(800) var customTitleJ2K: String? = null,
    @ProtoNumber(801) var customArtistJ2K: String? = null,
    @ProtoNumber(802) var customAuthorJ2K: String? = null,
    @ProtoNumber(804) var customDescriptionJ2K: String? = null,
    @ProtoNumber(805) var customGenreJ2K: List<String>? = null,
) {
    fun getAnimeImpl(): Anime {
        return Anime.create().copy(
            url = this@BackupAnime.url,
            // SY -->
            ogTitle = this@BackupAnime.title,
            ogArtist = this@BackupAnime.artist,
            ogAuthor = this@BackupAnime.author,
            ogThumbnailUrl = this@BackupAnime.thumbnailUrl,
            ogDescription = this@BackupAnime.description,
            ogGenre = this@BackupAnime.genre,
            ogStatus = this@BackupAnime.status.toLong(),
            // SY <--
            favorite = this@BackupAnime.favorite,
            source = this@BackupAnime.source,
            dateAdded = this@BackupAnime.dateAdded,
            viewerFlags = this@BackupAnime.viewer_flags.toLong(),
            episodeFlags = this@BackupAnime.episodeFlags.toLong(),
            updateStrategy = this@BackupAnime.updateStrategy,
            lastModifiedAt = this@BackupAnime.lastModifiedAt,
            favoriteModifiedAt = this@BackupAnime.favoriteModifiedAt,
            version = this@BackupAnime.version,
        )
    }

    // SY -->
    @Suppress("ComplexCondition")
    fun getCustomAnimeInfo(): CustomAnimeInfo? {
        val title = customTitle ?: customTitleJ2K
        val author = customAuthor ?: customAuthorJ2K
        val artist = customArtist ?: customArtistJ2K
        val description = customDescription ?: customDescriptionJ2K
        val genre = customGenre ?: customGenreJ2K
        val status = customStatus.takeUnless { it == 0 }?.toLong()

        if (title != null ||
            artist != null ||
            author != null ||
            description != null ||
            genre != null ||
            status != null
        ) {
            return CustomAnimeInfo(
                id = 0L,
                title = title,
                author = author,
                artist = artist,
                description = description,
                genre = genre,
                status = status,
            )
        }
        return null
    }
    // SY <--
}
