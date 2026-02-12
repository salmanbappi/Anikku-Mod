package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Golden theme
 */
internal object GoldenColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFF2BF4E),
        onPrimary = Color(0xFF412D00),
        primaryContainer = Color(0xFF5D4200),
        onPrimaryContainer = Color(0xFFFFDF9E),
        inversePrimary = Color(0xFF7C5800),
        secondary = Color(0xFFF2BF4E),
        onSecondary = Color(0xFF412D00),
        secondaryContainer = Color(0xFF5D4200),
        onSecondaryContainer = Color(0xFFFFDF9E),
        tertiary = Color(0xFFB9D087),
        onTertiary = Color(0xFF253500),
        tertiaryContainer = Color(0xFF384D00),
        onTertiaryContainer = Color(0xFFD5ECA1),
        background = Color(0xFF1E1B16),
        onBackground = Color(0xFFE9E1D9),
        surface = Color(0xFF1E1B16),
        onSurface = Color(0xFFE9E1D9),
        surfaceVariant = Color(0xFF4E4639),
        onSurfaceVariant = Color(0xFFD1C5B4),
        surfaceTint = Color(0xFFF2BF4E),
        inverseSurface = Color(0xFFE9E1D9),
        inverseOnSurface = Color(0xFF1E1B16),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF9A8F80),
        outlineVariant = Color(0xFF4E4639),
        surfaceContainerLowest = Color(0xFF120F09),
        surfaceContainerLow = Color(0xFF1E1B16),
        surfaceContainer = Color(0xFF221F1A),
        surfaceContainerHigh = Color(0xFF2D2924),
        surfaceContainerHighest = Color(0xFF38342E),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF7C5800),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDF9E),
        onPrimaryContainer = Color(0xFF271900),
        inversePrimary = Color(0xFFF2BF4E),
        secondary = Color(0xFF7C5800),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDF9E),
        onSecondaryContainer = Color(0xFF271900),
        tertiary = Color(0xFF506600),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFD5ECA1),
        onTertiaryContainer = Color(0xFF161F00),
        background = Color(0xFFFFFBFF),
        onBackground = Color(0xFF1E1B16),
        surface = Color(0xFFFFFBFF),
        onSurface = Color(0xFF1E1B16),
        surfaceVariant = Color(0xFFEEE1CF),
        onSurfaceVariant = Color(0xFF4E4639),
        surfaceTint = Color(0xFF7C5800),
        inverseSurface = Color(0xFF33302A),
        inverseOnSurface = Color(0xFFF7F0E7),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFF807667),
        outlineVariant = Color(0xFFD1C5B4),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFBF2E9),
        surfaceContainer = Color(0xFFF5ECEA),
        surfaceContainerHigh = Color(0xFFF0E7E1),
        surfaceContainerHighest = Color(0xFFEBE2DB),
    )
}
