package eu.kanade.tachiyomi.util.system

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImagePainter
import tachiyomi.domain.anime.model.AnimeCover

object CoverColorExtractor {

    fun extract(
        cover: AnimeCover,
        state: AsyncImagePainter.State.Success
    ) {
        val drawable = state.painter.let { it as? BitmapDrawable } ?: return
        val bitmap = drawable.bitmap ?: return
        
        if (cover.vibrantCoverColor != null) return

        Palette.from(bitmap).generate { palette ->
            palette?.let {
                val color = it.getVibrantColor(it.getMutedColor(0))
                if (color != 0) {
                    cover.vibrantCoverColor = color
                    CoverColorObserver.update(cover.animeId, color)
                }
            }
        }
    }
}
