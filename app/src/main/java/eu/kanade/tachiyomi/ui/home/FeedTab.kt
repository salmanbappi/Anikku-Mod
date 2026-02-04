import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.runtime.Composable

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
