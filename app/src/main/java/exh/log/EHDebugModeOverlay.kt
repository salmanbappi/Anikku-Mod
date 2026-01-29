package exh.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    val vfFps by PlayerStats.estimatedVfFps.collectAsState(0.0)
    val sourceFps by PlayerStats.videoParamsFps.collectAsState(0.0)
    val containerFps by PlayerStats.containerFps.collectAsState(0.0)
    val displayFps by PlayerStats.displayFps.collectAsState(0.0)
    val estimatedDisplayFps by PlayerStats.estimatedDisplayFps.collectAsState(0.0)
    
    val isInterpolating by PlayerStats.isInterpolating.collectAsState(false)
    val videoSync by PlayerStats.videoSync.collectAsState("")
    val hwdec = MPVLib.getPropertyString("hwdec-current") ?: "no"
    
    val videoW by PlayerStats.videoW.collectAsState(0L)
    val videoH by PlayerStats.videoH.collectAsState(0L)
    val bitrate by PlayerStats.videoBitrate.collectAsState(0L)

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
        Text(text = "SMOOTH MOTION DEBUG (PAGE 6)", style = baseStyle.copy(color = Color(0xFF33BBFF)))
        Spacer(Modifier.height(8.dp))

        // Hardware Warning
        val isHardwareBlocking = hwdec == "mediacodec"
        if (isHardwareBlocking) {
            Text(
                text = "WARNING: HW Decoder is blocking Interpolation!",
                style = baseStyle.copy(color = Color.Red)
            )
            Text(
                text = "Current: $hwdec (Must be copy or no)",
                style = baseStyle.copy(color = Color.Red, fontSize = 11.sp)
            )
            Spacer(Modifier.height(8.dp))
        }

        // Status logic
        val statusText = when {
            isInterpolating && !isHardwareBlocking -> "Active (Working)"
            isInterpolating && isHardwareBlocking -> "Active (But blocked by HW)"
            else -> "Inactive"
        }
        StatLine("Status", statusText, baseStyle)
        StatLine("Sync Mode", videoSync, baseStyle)
        
        Spacer(Modifier.height(12.dp))

        // FPS Details with fallbacks
        val finalSourceFps = if (sourceFps > 0) sourceFps else containerFps
        val finalActualFps = if (estimatedDisplayFps > 0) estimatedDisplayFps else vfFps
        
        StatLine("Source Rate", "${format.format(finalSourceFps)} fps", baseStyle)
        StatLine("Filter Output", "${format.format(vfFps)} fps", baseStyle)
        StatLine("Actual Display", "${format.format(finalActualFps)} fps", baseStyle)
        StatLine("Refresh Rate", "${format.format(displayFps)} Hz", baseStyle)
        
        Spacer(Modifier.height(12.dp))

        // Video Details
        if (videoW > 0) {
            StatLine("Resolution", "${videoW}x${videoH}", baseStyle)
        }
        if (bitrate > 0) {
            StatLine("Bitrate", "${bitrate / 1000} kbps", baseStyle)
        }
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
