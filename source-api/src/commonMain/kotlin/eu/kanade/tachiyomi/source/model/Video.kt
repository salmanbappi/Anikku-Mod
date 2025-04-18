package eu.kanade.tachiyomi.source.model

import eu.kanade.tachiyomi.animesource.model.SerializableVideo.Companion.serialize
import eu.kanade.tachiyomi.animesource.model.SerializableVideo.Companion.toVideoList
import eu.kanade.tachiyomi.animesource.model.Video

typealias Video = Video
typealias Track = eu.kanade.tachiyomi.animesource.model.Track

fun List<Video>.serialize() = this.serialize()
fun String.toVideoList() = this.toVideoList()
