package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.animesource.preferenceKey
import eu.kanade.tachiyomi.animesource.sourcePreferences

typealias ConfigurableSource = eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource

fun ConfigurableSource.preferenceKey() = preferenceKey()

fun ConfigurableSource.sourcePreferences() = sourcePreferences()

fun sourcePreferences(key: String) = sourcePreferences(key)
