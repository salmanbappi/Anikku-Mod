package eu.kanade.tachiyomi.ui.home

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource

data object FeedTab : Tab {

    @OptIn(ExperimentalAnimationGraphicsApi::class)
    override val options: TabOptions
        @Composable
        get() {
            val title = SYMR.strings.feed
            val isSelected = LocalTabNavigator.current.current.key == key
            return TabOptions(
                index = 1u,
                title = stringResource(title),
                icon = rememberAnimatedVectorPainter(
                    vector = Icons.Outlined.Explore,
                    isSelected = isSelected
                ),
            )
        }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { FeedScreenModel() }
        
        FeedScreen(
            screenModel = screenModel,
            onAnimeClick = { navigator.push(AnimeScreen(it.id)) },
        )
    }
}