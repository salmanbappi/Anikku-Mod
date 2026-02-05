package eu.kanade.tachiyomi.ui.home

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource

import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

fun feedTab(): Tab = FeedTab

data object FeedTab : Tab {
// ... same options ...
    @Composable
    override fun Content() {
        Content(PaddingValues(0.dp))
    }

    @Composable
    fun Content(contentPadding: PaddingValues) {
        val navigator = LocalNavigator.currentOrThrow
        val tabNavigator = LocalTabNavigator.current
        val scope = rememberCoroutineScope()
        val screenModel = rememberScreenModel { FeedScreenModel() }
        
        FeedScreen(
            screenModel = screenModel,
            onAnimeClick = { navigator.push(AnimeScreen(it.id)) },
            onAddSourceClick = { 
                scope.launch {
                    tabNavigator.current = BrowseTab
                    // BrowseTab is already at index 0 (sourcesTab)
                }
            },
            contentPadding = contentPadding,
        )
    }
}
