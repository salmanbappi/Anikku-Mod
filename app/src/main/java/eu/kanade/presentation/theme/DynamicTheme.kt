package eu.kanade.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle

@Composable
fun CoverBasedTheme(
    seedColor: Color?,
    isAmoled: Boolean,
    content: @Composable () -> Unit,
) {
    if (seedColor != null) {
        DynamicMaterialTheme(
            seedColor = seedColor,
            useDarkTheme = isSystemInDarkTheme(),
            style = PaletteStyle.TonalSpot, // Default style, can be made configurable
            withAmoled = isAmoled,
            content = content
        )
    } else {
        // Fallback to default theme if no seed color
        MaterialTheme(
            content = content
        )
    }
}
