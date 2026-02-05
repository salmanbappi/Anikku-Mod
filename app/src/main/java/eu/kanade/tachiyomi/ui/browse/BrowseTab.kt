package eu.kanade.tachiyomi.ui.browse

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.discord.DiscordRPCService
import eu.kanade.tachiyomi.data.connections.discord.DiscordScreen
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsScreenModel
import eu.kanade.tachiyomi.ui.browse.extension.extensionsTab
import eu.kanade.tachiyomi.ui.browse.migration.sources.migrateSourceTab
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.browse.source.sourcesTab
import eu.kanade.tachiyomi.ui.home.FeedManageScreen
import eu.kanade.tachiyomi.ui.home.FeedTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data object BrowseTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current is BrowseTab
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 3u,
                title = stringResource(MR.strings.browse),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(GlobalSearchScreen())
    }

    private val switchToExtensionTabChannel = kotlinx.coroutines.channels.Channel<Unit>(1, kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)

    fun showExtension() {
        switchToExtensionTabChannel.trySend(Unit)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        val enableFeed by uiPreferences.enableFeed().collectAsState()

        // Hoisted for extensions tab's search bar
        val extensionsScreenModel = rememberScreenModel { ExtensionsScreenModel() }
        val animeExtensionsState by extensionsScreenModel.state.collectAsState()

        val sourcesTab = sourcesTab()
        val extensionsTab = extensionsTab(extensionsScreenModel)
        val migrateSourceTab = migrateSourceTab()

        val tabs = remember(enableFeed, sourcesTab, extensionsTab, migrateSourceTab) {
            buildList {
                add(sourcesTab)
                if (enableFeed) {
                    add(
                        eu.kanade.presentation.components.TabContent(
                            titleRes = SYMR.strings.feed,
                            searchEnabled = false,
                            actions = persistentListOf(
                                AppBar.Action(
                                    title = "Edit Feed",
                                    icon = Icons.Outlined.Settings,
                                    onClick = { 
                                        navigator.push(FeedManageScreen())
                                    },
                                ),
                            ),
                            content = { contentPadding, _ -> 
                                FeedTab.Content(contentPadding)
                            }
                        )
                    )
                }
                add(extensionsTab)
                add(migrateSourceTab)
            }.toPersistentList()
        }

        val state = rememberPagerState { tabs.size }

        TabbedScreen(
            titleRes = MR.strings.browse,
            tabs = tabs,
            state = state,
            searchQuery = animeExtensionsState.searchQuery,
            onChangeSearchQuery = extensionsScreenModel::search,
            scrollable = false,
        )
        LaunchedEffect(state, enableFeed) {
            switchToExtensionTabChannel.receiveAsFlow()
                .collectLatest { 
                    val targetPage = if (enableFeed) 2 else 1
                    state.scrollToPage(targetPage) 
                }
        }

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
            // AM (DISCORD) -->
            DiscordRPCService.setAnimeScreen(context, DiscordScreen.BROWSE)
            DiscordRPCService.setMangaScreen(context, DiscordScreen.BROWSE)
            // <-- AM (DISCORD)
        }
    }
}
