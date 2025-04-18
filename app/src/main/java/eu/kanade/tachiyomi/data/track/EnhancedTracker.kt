package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.track.model.Track

/**
 * An Enhanced Track Service will never prompt the user to match a manga with the remote.
 * It is expected that such Track Service can only work with specific sources and unique IDs.
 */
interface EnhancedTracker {
    /**
     * This Tracker will only work with the sources that are accepted by this filter function.
     */
    fun accept(source: AnimeSource): Boolean {
        return source::class.qualifiedName in getAcceptedSources()
    }

    /**
     * Fully qualified source classes that this track service is compatible with.
     */
    fun getAcceptedSources(): List<String>

    fun loginNoop()

    /**
     * match is similar to Tracker.search, but only return zero or one match.
     */
    suspend fun match(anime: Anime): TrackSearch?

    /**
     * Checks whether the provided source/track/anime triplet is from this AnimeTracker
     */
    fun isTrackFrom(track: Track, anime: Anime, source: AnimeSource?): Boolean

    /**
     * Migrates the given track for the anime to the newSource, if possible
     */
    fun migrateTrack(track: Track, anime: Anime, newSource: AnimeSource): Track?
}
