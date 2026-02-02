package eu.kanade.tachiyomi.ui.player.loader

import eu.kanade.domain.episode.model.toSEpisode
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.Hoster.Companion.toHosterList
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.player.controls.components.sheets.HosterState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.model.Episode
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.io.LocalSourceFileSystem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Loader used to retrieve the hosters for a given episode.
 */
class EpisodeLoader {
    companion object {
        private val hasHosterListMethod = mutableMapOf<String, Boolean>()

        /**
         * Returns a list of hosters of an [episode] based on the type of [source] used.
         *
         * @param episode the episode being parsed.
         * @param anime the anime of the episode.
         * @param source the source of the anime.
         */
        suspend fun getHosters(episode: Episode, anime: Anime, source: AnimeSource): List<Hoster> {
            val isDownloaded = isDownload(episode, anime, skipCache = false)
            return when {
                isDownloaded -> getHostersOnDownloaded(episode, anime, source)
                source is AnimeHttpSource -> getHostersOnHttp(episode, source)
                source is LocalSource -> getHostersOnLocal(episode)
                else -> error("source not supported")
            }
        }

        /**
         * Returns true if the given [episode] is downloaded.
         *
         * @param episode the episode being parsed.
         * @param anime the anime of the episode.
         */
        fun isDownload(episode: Episode, anime: Anime, skipCache: Boolean = true): Boolean {
            val downloadManager: DownloadManager = Injekt.get()
            return downloadManager.isEpisodeDownloaded(
                episode.name,
                episode.scanlator,
                anime.title,
                anime.source,
                skipCache = skipCache,
            )
        }

        /**
         * Returns a list of hosters when the [episode] is online.
         *
         * @param episode the episode being parsed.
         * @param source the online source of the episode.
         */
        private suspend fun getHostersOnHttp(episode: Episode, source: AnimeHttpSource): List<Hoster> {
            val sourceClass = source.javaClass.name
            val hasMethod = hasHosterListMethod.getOrPut(sourceClass) {
                source.javaClass.declaredMethods.any { it.name == "getHosterList" }
            }

            return try {
                kotlinx.coroutines.withTimeout(15000) {
                    if (hasMethod) {
                        source.getHosterList(episode.toSEpisode())
                    } else {
                        source.getVideoList(episode.toSEpisode()).toHosterList()
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw java.io.IOException("Connection timed out while fetching hosters")
            }
        }

        /**
         * Returns the hoster when the [episode] is downloaded.
         *
         * @param episode the episode being parsed.
         * @param anime the anime of the episode.
         * @param source the source of the anime.
         */
        private fun getHostersOnDownloaded(
            episode: Episode,
            anime: Anime,
            source: AnimeSource,
        ): List<Hoster> {
            val downloadManager: DownloadManager = Injekt.get()
            return try {
                val video = downloadManager.buildVideo(source, anime, episode)
                listOf(video).toHosterList()
            } catch (e: Throwable) {
                emptyList()
            }
        }

        /**
         * Returns the hoster when the [episode] is from local source.
         *
         * @param episode the episode being parsed.
         */
        private fun getHostersOnLocal(
            episode: Episode,
        ): List<Hoster> {
            return try {
                val (animeDirName, episodeName) = episode.url.split('/', limit = 2)
                val fileSystem: LocalSourceFileSystem = Injekt.get()
                val videoFile = fileSystem.getBaseDirectory()
                    ?.findFile(animeDirName)
                    ?.findFile(episodeName)
                val videoUri = videoFile!!.uri

                val video = Video(
                    videoUri.toString(),
                    "Local source: ${episode.url}",
                )
                listOf(video).toHosterList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        /**
         * Returns a list of videos of a [hoster] based on the type of [source] used.
         * Note that for every type of episode except non-downloaded online, `videoList`
         * will be set to null.
         *
         * @param source the source of the anime.
         * @param hoster the hoster.
         */
        private suspend fun getVideos(source: AnimeSource, hoster: Hoster): List<Video> {
            return when {
                hoster.videoList != null && source is AnimeHttpSource -> hoster.videoList!!.parseVideoUrls(source)
                hoster.videoList != null -> hoster.videoList!!
                source is AnimeHttpSource -> getVideosOnHttp(source, hoster)
                else -> error("source not supported")
            }
        }

        /**
         * Returns a list of hosters when the [episode] is online.
         *
         * @param source the online source of the episode.
         * @param hoster the hoster.
         */
        private suspend fun getVideosOnHttp(source: AnimeHttpSource, hoster: Hoster): List<Video> {
            return source.getVideoList(hoster).parseVideoUrls(source)
        }

        // Parallelized video URL parsing for faster startup
        private suspend fun List<Video>.parseVideoUrls(source: AnimeHttpSource): List<Video> {
            return coroutineScope {
                this@parseVideoUrls.map { video ->
                    async {
                        if (video.videoUrl != "null" && video.videoUrl.isNotBlank()) return@async video

                        val newVideoUrl = try {
                            source.getVideoUrl(video)
                        } catch (e: Exception) {
                            "null"
                        }
                        video.copy(videoUrl = newVideoUrl)
                    }
                }.awaitAll()
            }
        }

        suspend fun loadHosterVideos(source: AnimeSource, hoster: Hoster): HosterState {
            return try {
                val videos = getVideos(source, hoster)
                HosterState.Ready(hoster.hosterName, videos, List(videos.size) { Video.State.QUEUE })
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                }

                HosterState.Error(hoster.hosterName)
            }
        }
    }
}
