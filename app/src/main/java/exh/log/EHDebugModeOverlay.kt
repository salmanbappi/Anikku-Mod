package exh.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.ui.player.PlayerStats
import `is`.xyz.mpv.MPVLib
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun InterpolationStatsOverlay() {
    val videoFps by PlayerStats.estimatedVfFps.collectAsState(0.0)
    val sourceFps by PlayerStats.videoParamsFps.collectAsState(0.0)
    val isInterpolating by PlayerStats.isInterpolating.collectAsState(false)
    val videoW by PlayerStats.videoW.collectAsState(0L)
    val videoH by PlayerStats.videoH.collectAsState(0L)
    val codec by PlayerStats.videoCodec.collectAsState("")
    val bitrate by PlayerStats.videoBitrate.collectAsState(0L)
    val pixFmt by PlayerStats.videoPixFmt.collectAsState("")
    val levels by PlayerStats.videoLevels.collectAsState("")
    val primaries by PlayerStats.videoPrimaries.collectAsState("")

    val format = remember {
        DecimalFormat(
            "0.0",
            DecimalFormatSymbols.getInstance(Locale.ENGLISH),
        )
    }

    val shadow = Shadow(color = Color.Black, offset = androidx.compose.ui.geometry.Offset(2f, 2f), blurRadius = 2f)
    val baseStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        color = Color.White,
        shadow = shadow,
        lineHeight = 18.sp,
    )

    Column(
        Modifier.padding(16.dp)
    ) {
        // Heading
        Text(text = "Interpolation Details", style = baseStyle.copy(color = Color(0xFF33BBFF)))
        Spacer(Modifier.height(8.dp))

        // Motion Section
        StatLine("Status", if (isInterpolating) "Active" else "Inactive", baseStyle)
        StatLine("Output Rate", "${format.format(videoFps)} fps", baseStyle)
        StatLine("Source Rate", "${format.format(sourceFps)} fps", baseStyle)
        
        Spacer(Modifier.height(12.dp))

        // Video Section
        if (videoW > 0) {
            StatLine("Resolution", "${videoW}x${videoH}", baseStyle)
        }
        if (codec.isNotEmpty()) {
            StatLine("Video Codec", codec, baseStyle)
        }
        if (bitrate > 0) {
            StatLine("Bitrate", "${bitrate / 1000} kbps", baseStyle)
        }

        Spacer(Modifier.height(12.dp))

        // Hardware Section
        if (pixFmt.isNotEmpty()) {
            StatLine("Format", pixFmt, baseStyle)
        }
        StatLine("Levels", levels.ifEmpty { "n/a" }, baseStyle)
        StatLine("Primaries", primaries.ifEmpty { "n/a" }, baseStyle)
        
        val hwdec = MPVLib.getPropertyString("hwdec-current") ?: "no"
        StatLine("HW Decoder", hwdec, baseStyle)
    }
}

@Composable
private fun StatLine(label: String, value: String, style: TextStyle) {
    Text(
        text = String.format(Locale.ENGLISH, "%-15s: %s", label, value),
        style = style,
    )
}