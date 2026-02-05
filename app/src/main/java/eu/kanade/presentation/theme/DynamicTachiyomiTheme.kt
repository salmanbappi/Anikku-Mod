package eu.kanade.presentation.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeContent
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.theme.colorscheme.MonetColorScheme
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun DynamicTachiyomiTheme(
    animate: Boolean = true,
    colorSeed: Int? = null,
    contrast: Double = 0.0,
    content: @Composable () -> Unit,
) {
    val uiPreferences = Injekt.get<UiPreferences>()
    val isAmoled by uiPreferences.themeDarkAmoled().collectAsState()
    val isDark = isSystemInDarkTheme()
    val isDynamicEnabled by uiPreferences.dynamicMangaTheme().collectAsState()

    if (colorSeed != null && isDynamicEnabled) {
        val colorScheme = rememberDynamicColorScheme(colorSeed, isDark, isAmoled, contrast)
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    } else {
        // Fallback to standard theme
        TachiyomiTheme(content = content)
    }
}

@Composable
private fun rememberDynamicColorScheme(
    seed: Int,
    isDark: Boolean,
    isAmoled: Boolean,
    contrast: Double,
): ColorScheme {
    val colorScheme = generateColorSchemeFromSeed(seed, isDark, contrast)
    if (isDark && isAmoled) {
        return colorScheme.copy(
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceVariant = Color(0xFF0C0C0C),
            surfaceContainerLowest = Color(0xFF0C0C0C),
            surfaceContainerLow = Color(0xFF0C0C0C),
            surfaceContainer = Color(0xFF0C0C0C),
            surfaceContainerHigh = Color(0xFF131313),
            surfaceContainerHighest = Color(0xFF1B1B1B),
        )
    }
    return colorScheme
}

@SuppressLint("RestrictedApi")
private fun generateColorSchemeFromSeed(seed: Int, dark: Boolean, contrast: Double): ColorScheme {
    val scheme = SchemeContent(
        Hct.fromInt(seed),
        dark,
        contrast,
    )
    val dynamicColors = MaterialDynamicColors()
    return ColorScheme(
        primary = Color(dynamicColors.primary().getArgb(scheme)),
        onPrimary = Color(dynamicColors.onPrimary().getArgb(scheme)),
        primaryContainer = Color(dynamicColors.primaryContainer().getArgb(scheme)),
        onPrimaryContainer = Color(dynamicColors.onPrimaryContainer().getArgb(scheme)),
        inversePrimary = Color(dynamicColors.inversePrimary().getArgb(scheme)),
        secondary = Color(dynamicColors.secondary().getArgb(scheme)),
        onSecondary = Color(dynamicColors.onSecondary().getArgb(scheme)),
        secondaryContainer = Color(dynamicColors.secondaryContainer().getArgb(scheme)),
        onSecondaryContainer = Color(dynamicColors.onSecondaryContainer().getArgb(scheme)),
        tertiary = Color(dynamicColors.tertiary().getArgb(scheme)),
        onTertiary = Color(dynamicColors.onTertiary().getArgb(scheme)),
        tertiaryContainer = Color(dynamicColors.tertiary().getArgb(scheme)),
        onTertiaryContainer = Color(dynamicColors.onTertiaryContainer().getArgb(scheme)),
        background = Color(dynamicColors.background().getArgb(scheme)),
        onBackground = Color(dynamicColors.onBackground().getArgb(scheme)),
        surface = Color(dynamicColors.surface().getArgb(scheme)),
        onSurface = Color(dynamicColors.onSurface().getArgb(scheme)),
        surfaceVariant = Color(dynamicColors.surfaceVariant().getArgb(scheme)),
        onSurfaceVariant = Color(dynamicColors.onSurfaceVariant().getArgb(scheme)),
        surfaceTint = Color(dynamicColors.surfaceTint().getArgb(scheme)),
        inverseSurface = Color(dynamicColors.inverseSurface().getArgb(scheme)),
        inverseOnSurface = Color(dynamicColors.inverseOnSurface().getArgb(scheme)),
        error = Color(dynamicColors.error().getArgb(scheme)),
        onError = Color(dynamicColors.onError().getArgb(scheme)),
        errorContainer = Color(dynamicColors.errorContainer().getArgb(scheme)),
        onErrorContainer = Color(dynamicColors.onErrorContainer().getArgb(scheme)),
        outline = Color(dynamicColors.outline().getArgb(scheme)),
        outlineVariant = Color(dynamicColors.outlineVariant().getArgb(scheme)),
        scrim = Color.Black,
        surfaceBright = Color(dynamicColors.surfaceBright().getArgb(scheme)),
        surfaceDim = Color(dynamicColors.surfaceDim().getArgb(scheme)),
        surfaceContainer = Color(dynamicColors.surfaceContainer().getArgb(scheme)),
        surfaceContainerHigh = Color(dynamicColors.surfaceContainerHigh().getArgb(scheme)),
        surfaceContainerHighest = Color(dynamicColors.surfaceContainerHighest().getArgb(scheme)),
        surfaceContainerLow = Color(dynamicColors.surfaceContainerLow().getArgb(scheme)),
        surfaceContainerLowest = Color(dynamicColors.surfaceContainerLowest().getArgb(scheme)),
    )
}
