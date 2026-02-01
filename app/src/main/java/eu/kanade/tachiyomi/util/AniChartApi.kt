package eu.kanade.tachiyomi.util
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.data.track.simkl.Simkl
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.source.model.SAnime
import eu.kanade.tachiyomi.ui.anime.track.TrackItem
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.anime.model.Anime
import java.time.OffsetDateTime
import java.util.Calendar

class AniChartApi {
    private val client = OkHttpClient()

    internal suspend fun loadAiringTime(
        anime: Anime,
        trackItems: List<TrackItem>,
        manualFetch: Boolean,
    ): Pair<Int, Long> {
        var airingEpisodeData = Pair(anime.nextEpisodeToAir, anime.nextEpisodeAiringAt)
        if (anime.status == SAnime.COMPLETED.toLong() && !manualFetch) return airingEpisodeData

        return withIOContext {
            val matchingTrackItem = trackItems.firstOrNull {
                (it.tracker is Anilist && it.track != null) ||
                    (it.tracker is MyAnimeList && it.track != null) ||
                    (it.tracker is Simkl && it.track != null)
            } ?: return@withIOContext Pair(1, 0L)

            matchingTrackItem.let { item ->
                item.track!!.let {
                    airingEpisodeData = when (item.tracker) {
                        is Anilist -> getAnilistAiringEpisodeData(it.remoteId)
                        is MyAnimeList -> getAnilistAiringEpisodeData(getAlIdFromMal(it.remoteId))
                        is Simkl -> getSimklAiringEpisodeData(it.remoteId)
                        else -> Pair(1, 0L)
                    }
                }
            }
            return@withIOContext airingEpisodeData
        }
    }

    private suspend fun getAlIdFromMal(idMal: Long): Long {
        return withIOContext {
            val query = """
                query {
                    Media(idMal:$idMal,type: ANIME) {
                        id
                    }
                }
            """.trimMargin()

            val response = try {
                client.newCall(
                    POST(
                        "https://graphql.anilist.co",
                        body = buildJsonObject { put("query", query) }.toString()
                            .toRequestBody(jsonMime),
                    ),
                ).execute()
            } catch (e: Exception) {
                return@withIOContext 0L
            }
            return@withIOContext response.body.string().substringAfter("id\":")
                .substringBefore("}")
                .toLongOrNull() ?: 0L
        }
    }

    private suspend fun getAnilistAiringEpisodeData(id: Long): Pair<Int, Long> {
        return withIOContext {
            val query = """
                query {
                    Media(id:$id) {
                        nextAiringEpisode {
                            episode
                            airingAt
                        }
                    }
                }
            """.trimMargin()
            val response = try {
                client.newCall(
                    POST(
                        "https://graphql.anilist.co",
                        body = buildJsonObject { put("query", query) }.toString()
                            .toRequestBody(jsonMime),
                    ),
                ).execute()
            } catch (e: Exception) {
                return@withIOContext Pair(1, 0L)
            }
            val data = response.body.string()
            val episodeNumber = data.substringAfter("episode\":").substringBefore(",").toIntOrNull() ?: 1
            val airingAt = data.substringAfter("airingAt\":").substringBefore("}").toLongOrNull() ?: 0L

            return@withIOContext Pair(episodeNumber, airingAt)
        }
    }

    private suspend fun getSimklAiringEpisodeData(id: Long): Pair<Int, Long> {
        var episodeNumber = 1
        var airingAt = 0L
        return withIOContext {
            val calendarTypes = listOf("anime", "tv", "movie_release")
            calendarTypes.forEach {
                val response = try {
                    client.newCall(GET("https://data.simkl.in/calendar/$it.json")).execute()
                } catch (e: Exception) {
                    return@withIOContext Pair(1, 0L)
                }

                val body = response.body.string()

                val data = removeAiredSimkl(body)

                val malId = data.substringAfter("\"simkl_id\":$id,", "").substringAfter(
                    "\"mal\":\"",
                ).substringBefore("\"").toLongOrNull() ?: 0L
                if (malId != 0L) {
                    return@withIOContext getAnilistAiringEpisodeData(
                        getAlIdFromMal(malId),
                    )
                }

                val epNum = data.substringAfter("\"simkl_id\":$id,", "").substringBefore("\"}}").substringAfterLast(
                    "\"episode\":",
                )
                episodeNumber = epNum.substringBefore(",").toIntOrNull() ?: episodeNumber

                val date = data.substringBefore("\"simkl_id\":$id,", "").substringAfterLast(
                    "\"date\":\"",
                ).substringBefore("\"")
                airingAt = if (date.isNotBlank()) toUnixTimestamp(date) else airingAt

                if (airingAt != 0L) return@withIOContext Pair(episodeNumber, airingAt)
            }
            return@withIOContext Pair(episodeNumber, airingAt)
        }
    }

    private fun removeAiredSimkl(body: String): String {
        val currentTimeInMillis = Calendar.getInstance().timeInMillis
        val index = body.split("\"date\":\"").drop(1).indexOfFirst {
            val date = it.substringBefore("\"")
            val time = if (date.isNotBlank()) toUnixTimestamp(date) else 0L
            time.times(1000) > currentTimeInMillis
        }
        return if (index >= 0) body.substring(index) else ""
    }

    private fun toUnixTimestamp(dateFormat: String): Long {
        val offsetDateTime = OffsetDateTime.parse(dateFormat)
        val instant = offsetDateTime.toInstant()
        return instant.epochSecond
    }

    suspend fun getRecommendations(title: String): List<Anime> {
        return withIOContext {
            val query = """
                query {
                    Media(search: "$title", type: ANIME) {
                        recommendations {
                            nodes {
                                mediaRecommendation {
                                    id
                                    idMal
                                    title {
                                        romaji
                                        english
                                        native
                                    }
                                    coverImage {
                                        extraLarge
                                    }
                                    description
                                    genres
                                    status
                                }
                            }
                        }
                    }
                }
            """.trimMargin()

            val response = try {
                client.newCall(
                    POST(
                        "https://graphql.anilist.co",
                        body = buildJsonObject { put("query", query) }.toString()
                            .toRequestBody(jsonMime),
                    ),
                ).execute()
            } catch (e: Exception) {
                return@withIOContext emptyList()
            }

            if (!response.isSuccessful) return@withIOContext emptyList()

            val body = response.body.string()
            // Simplified parsing for brevity, in a real scenario use a proper JSON parser
            val nodes = body.substringAfter("nodes\":[").substringBeforeLast("]}}")
            if (nodes.isBlank()) return@withIOContext emptyList()

            val recommendedAnimes = mutableListOf<Anime>()
            nodes.split("mediaRecommendation\":{").drop(1).forEach { node ->
                val id = node.substringAfter("id\":").substringBefore(",").toLongOrNull() ?: return@forEach
                val malId = node.substringAfter("idMal\":").substringBefore(",").toLongOrNull()
                val romajiTitle = node.substringAfter("romaji\":\"").substringBefore("\"")
                val englishTitle = node.substringAfter("english\":\"").substringBefore("\"").takeIf { it != "null" }
                val coverImage = node.substringAfter("extraLarge\":\"").substringBefore("\"").replace("\\/", "/")
                val description = node.substringAfter("description\":\"").substringBefore("\"")
                val status = node.substringAfter("status\":\"").substringBefore("\"")

                recommendedAnimes.add(
                    Anime(
                        id = -1L,
                        source = -1L,
                        favorite = false,
                        lastUpdate = 0L,
                        nextUpdate = 0L,
                        fetchInterval = 0,
                        dateAdded = 0L,
                        viewerFlags = 0L,
                        episodeFlags = 0L,
                        coverLastModified = 0L,
                        url = "https://anilist.co/anime/$id",
                        ogTitle = romajiTitle,
                        ogArtist = null,
                        ogAuthor = null,
                        ogThumbnailUrl = coverImage,
                        ogDescription = description,
                        ogGenre = null,
                        ogStatus = 0L,
                        updateStrategy = eu.kanade.tachiyomi.source.model.UpdateStrategy.ALWAYS_UPDATE,
                        initialized = true,
                        lastModifiedAt = 0L,
                        favoriteModifiedAt = null,
                        version = 0L,
                    )
                )
            }
            return@withIOContext recommendedAnimes
        }
    }
}
