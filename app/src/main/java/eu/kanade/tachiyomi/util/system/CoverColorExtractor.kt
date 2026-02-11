package eu.kanade.tachiyomi.util.system

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.asDrawable
import coil3.compose.AsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.domain.anime.model.AnimeCover
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object CoverColorExtractor {

    suspend fun extract(
        cover: AnimeCover,
        state: AsyncImagePainter.State.Success
    ) = withContext(Dispatchers.Default) {
        val context = Injekt.get<Application>()
        val image = state.result.image
        
        val bitmap = when (image) {
            is BitmapImage -> image.bitmap
            else -> image.asDrawable(context.resources).toBitmap()
        }.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && it.config == Bitmap.Config.HARDWARE) {
                it.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                it
            }
        }

        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        cover.ratio = ratio
        CoverColorObserver.updateRatio(cover.animeId, ratio)

        if (cover.vibrantCoverColor != null) return@withContext

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
