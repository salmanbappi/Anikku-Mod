package eu.kanade.presentation.util

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter

@Composable
fun rememberDrawablePainter(drawable: Drawable?): Painter {
    return remember(drawable) {
        when (drawable) {
            null -> ColorPainter(Color.Transparent)
            is ColorDrawable -> ColorPainter(Color(drawable.color))
            is BitmapDrawable -> BitmapPainter(drawable.bitmap.asImageBitmap())
            else -> {
                // Fallback for other drawable types
                ColorPainter(Color.Transparent)
            }
        }
    }
}
