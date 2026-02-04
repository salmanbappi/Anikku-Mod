package eu.kanade.presentation.more.settings

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AppBar
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

@Composable
fun PreferenceScaffold(
    titleRes: StringResource,
    actions: @Composable RowScope.() -> Unit = {},
    onBackPressed: (() -> Unit)? = null,
    itemsProvider: @Composable () -> List<Preference>,
) {
    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(titleRes),
                navigateUp = onBackPressed,
                actions = actions,
                scrollBehavior = it,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        content = { contentPadding ->
            PreferenceScreen(
                items = itemsProvider(),
                contentPadding = contentPadding,
            )
        },
    )
}
