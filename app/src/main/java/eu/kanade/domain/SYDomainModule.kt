package eu.kanade.domain

import android.app.Application
import tachiyomi.data.anime.CustomAnimeRepositoryImpl
import tachiyomi.domain.anime.interactor.GetCustomAnimeInfo
import tachiyomi.domain.anime.interactor.SetCustomAnimeInfo
import tachiyomi.domain.anime.repository.CustomAnimeRepository
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addFactory
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class SYDomainModule : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<CustomAnimeRepository> { CustomAnimeRepositoryImpl(get<Application>()) }
        addFactory { GetCustomAnimeInfo(get()) }
        addFactory { SetCustomAnimeInfo(get()) }
    }
}
