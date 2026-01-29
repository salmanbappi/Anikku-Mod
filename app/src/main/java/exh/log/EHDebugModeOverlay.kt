package exh.log

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.ui.player.PlayerStats
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun InterpolationStatsOverlay(isPageSix: Boolean = false) {
    val videoFps by PlayerStats.estimatedVfFps.collectAsState(0.0)
    val sourceFps by PlayerStats.videoParamsFps.collectAsState(0.0)
    val isInterpolating by PlayerStats.isInterpolating.collectAsState(false)
    val videoW by PlayerStats.videoW.collectAsState(0L)
    val videoH by PlayerStats.videoH.collectAsState(0L)
    val codec by PlayerStats.videoCodec.collectAsState("")
    val bitrate by PlayerStats.videoBitrate.collectAsState(0L)

    if (videoFps <= 0.0 && !isPageSix) return

    val format = remember {
        DecimalFormat(
            "0.0",
            DecimalFormatSymbols.getInstance(Locale.ENGLISH),
        )
    }

    val shadow = Shadow(color = Color.Black, blurRadius = 4f)
    val baseStyle = MaterialTheme.typography.bodySmall.copy(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        color = Color.White,
        shadow = shadow,
        lineHeight = 16.sp,
    )

    if (isPageSix) {
        Column(
            Modifier
                .padding(16.dp)
                .background(Color(0x99000000))
                .padding(8.dp),
        ) {
            Text(text = "DEBUG STATISTICS (PAGE 6)", style = baseStyle.copy(fontWeight = FontWeight.Bold, color = Color.Yellow))
            Text(text = "--------------------------", style = baseStyle)
            
            StatLine("Interpolation", if (isInterpolating) "Active" else "Inactive", baseStyle)
            StatLine("Display FPS", format.format(videoFps), baseStyle)
            StatLine("Source FPS", format.format(sourceFps), baseStyle)
            
            if (videoW > 0) {
                StatLine("Resolution", "${videoW}x${videoH}", baseStyle)
            }
            if (codec.isNotEmpty()) {
                StatLine("Codec", codec, baseStyle)
            }
            if (bitrate > 0) {
                StatLine("Bitrate", "${bitrate / 1000} kbps", baseStyle)
            }
            
            // System info
            Text(text = "--------------------------", style = baseStyle)
            val hwdec = `is`.xyz.mpv.MPVLib.getPropertyString("hwdec-current") ?: "no"
            StatLine("HW Decoder", hwdec, baseStyle)
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, style: TextStyle) {
    Text(
        text = String.format(Locale.ENGLISH, "%-15s: %s", label, value),
        style = style,
    )
}
