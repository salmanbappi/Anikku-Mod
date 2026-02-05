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

fun feedTab(): Tab = FeedTab

data object FeedTab : Tab {

    @OptIn(ExperimentalAnimationGraphicsApi::class)
    override val options: TabOptions
        @Composable
        get() {
            val title = SYMR.strings.feed
            return TabOptions(
                index = 1u,
                title = stringResource(title),
                icon = painterResource(R.drawable.ic_browse_filled_24dp),
            )
        }

    @Composable
    override fun Content() {
        Content(PaddingValues(0.dp))
    }

    @Composable
    fun Content(contentPadding: PaddingValues) {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { FeedScreenModel() }
        
        FeedScreen(
            screenModel = screenModel,
            onAnimeClick = { navigator.push(AnimeScreen(it.id)) },
            contentPadding = contentPadding,
        )
    }
}
