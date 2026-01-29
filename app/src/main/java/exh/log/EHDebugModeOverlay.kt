package exh.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.ui.player.PlayerStats
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun InterpolationStatsOverlay() {
    val videoFps by PlayerStats.estimatedVfFps.collectAsState(0.0)
    val sourceFps by PlayerStats.videoParamsFps.collectAsState(0.0)
    val isInterpolating by PlayerStats.isInterpolating.collectAsState(false)

    if (videoFps <= 0.0) return

    val format = remember {
        DecimalFormat(
            "0.0",
            DecimalFormatSymbols.getInstance(Locale.ENGLISH),
        )
    }

    Column(
        Modifier
            .padding(12.dp)
            .padding(top = 180.dp), // Offset to appear below the main track info in Page 1
    ) {
        val shadow = Shadow(color = Color.Black, blurRadius = 4f)
        val style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color.White,
            shadow = shadow,
        )

        Text(
            text = "Interpolation: ${if (isInterpolating) "Active" else "Inactive"}",
            style = style,
        )
        Text(
            text = "Display FPS: ${format.format(videoFps)}",
            style = style,
        )
        Text(
            text = "Source FPS: ${format.format(sourceFps)}",
            style = style,
        )
    }
}