package exh.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.font.FontWeight
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
    val actualFps by PlayerStats.estimatedDisplayFps.collectAsState(0.0)
    
    val isInterpolating by PlayerStats.isInterpolating.collectAsState(false)
    val videoSync by PlayerStats.videoSync.collectAsState("")
    val tscale by PlayerStats.tscale.collectAsState("")
    val delayedFrames by PlayerStats.delayedFrames.collectAsState(0L)
    val mistime by PlayerStats.mistime.collectAsState(0.0)
    val voPasses by PlayerStats.voPasses.collectAsState(0L)
    
    val videoW by PlayerStats.videoW.collectAsState(0L)
    val videoH by PlayerStats.videoH.collectAsState(0L)
    val dwidth by PlayerStats.dwidth.collectAsState(0L)
    val dheight by PlayerStats.dheight.collectAsState(0L)
    val videoOutW by PlayerStats.videoOutW.collectAsState(0L)
    val videoOutH by PlayerStats.videoOutH.collectAsState(0L)
    
    val pixFmt by PlayerStats.videoPixFmt.collectAsState("")
    val levels by PlayerStats.videoLevels.collectAsState("")
    val primaries by PlayerStats.videoPrimaries.collectAsState("")
    val codec by PlayerStats.videoCodec.collectAsState("")
    val bitrate by PlayerStats.videoBitrate.collectAsState(0L)

    val format = remember {
        DecimalFormat(
            "0.00",
            DecimalFormatSymbols.getInstance(Locale.ENGLISH),
        )
    }

    val shadow = Shadow(color = Color.Black, offset = androidx.compose.ui.geometry.Offset(2f, 2f), blurRadius = 2f)
    val baseStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        color = Color.White,
        shadow = shadow,
        lineHeight = 16.sp,
    )

    // Solid background to prevent "smooching" (overlap) with Page 1
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xEE000000))
            .padding(24.dp)
    ) {
        Text(
            text = "PRO PLAYER STATISTICS (PAGE 6)", 
            style = baseStyle.copy(
                fontWeight = FontWeight.Bold, 
                fontSize = 14.sp,
                color = Color(0xFF33BBFF)
            )
        )
        Text(text = "--------------------------------------", style = baseStyle.copy(color = Color.Gray))
        Spacer(Modifier.height(8.dp))

        // MOTION PIPELINE
        val hwdec = MPVLib.getPropertyString("hwdec-current") ?: "no"
        val isDirect = hwdec == "mediacodec"
        val isWorking = isInterpolating && !isDirect && voPasses > 1
        
        Text(text = "[ Motion Pipeline ]", style = baseStyle.copy(color = Color.Yellow))
        StatLine("Status", if (isWorking) "ACTIVE (Interpolating)" else if (isDirect) "BYPASSED (Direct HWDEC)" else "INACTIVE", baseStyle.copy(color = if (isWorking) Color.Green else if (isDirect) Color.Red else Color.Unspecified))
        StatLine("Algorithm", tscale.ifEmpty { "none" }, baseStyle)
        StatLine("Sync Mode", videoSync, baseStyle)
        StatLine("VO Passes", "$voPasses", baseStyle)
        
        Spacer(Modifier.height(12.dp))

        // FRAME RATES
        val finalSourceFps = listOf(sourceFps, containerFps, vfFps).firstOrNull { it > 0.0 } ?: 0.0
        Text(text = "[ Frame Rates ]", style = baseStyle.copy(color = Color.Yellow))
        StatLine("Source Rate", "${format.format(finalSourceFps)} fps", baseStyle)
        StatLine("Render Rate", "${format.format(actualFps)} fps", baseStyle.copy(color = if (actualFps >= 58 && isWorking) Color.Green else Color.Unspecified))
        StatLine("Display Hz", "${format.format(displayFps)} Hz", baseStyle)
        
        Spacer(Modifier.height(12.dp))

        // PERFORMANCE
        Text(text = "[ Performance ]", style = baseStyle.copy(color = Color.Yellow))
        StatLine("Mistime", "${(mistime * 1000).toInt()} ms", baseStyle.copy(color = if (mistime > 0.02) Color.Yellow else Color.Unspecified))
        StatLine("Dropped", "$delayedFrames frames", baseStyle.copy(color = if (delayedFrames > 0) Color.Red else Color.Unspecified))
        StatLine("API", "Vulkan", baseStyle.copy(color = Color.Cyan))

        Spacer(Modifier.height(12.dp))

        // VIDEO DETAILS
        val finalW = listOf(dwidth, videoW, videoOutW).firstOrNull { it > 0L } ?: 0L
        val finalH = listOf(dheight, videoH, videoOutH).firstOrNull { it > 0L } ?: 0L
        
        Text(text = "[ Video Details ]", style = baseStyle.copy(color = Color.Yellow))
        StatLine("Resolution", "${finalW}x${finalH}", baseStyle)
        StatLine("Decoder", hwdec, baseStyle)
        
        if (codec.isNotEmpty() || pixFmt.isNotEmpty()) {
            val detailStr = listOfNotNull(codec, pixFmt, levels, primaries).filter { it.isNotEmpty() }.joinToString(" / ")
            Text(text = detailStr, style = baseStyle.copy(fontSize = 10.sp, color = Color.Gray))
        }
        
        if (bitrate > 0) {
            StatLine("Bitrate", "${bitrate / 1000} kbps", baseStyle)
        }
        
        Spacer(Modifier.weight(1f))
        Text(
            text = "Tip: Use 'mediacodec-copy' for Smooth Motion. Direct HWDEC blocks interpolation.",
            style = baseStyle.copy(fontSize = 10.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        )
    }
}

@Composable
private fun StatLine(label: String, value: String, style: TextStyle) {
    Row {
        Text(text = String.format(Locale.ENGLISH, "%-15s", label), style = style.copy(color = Color.Gray))
        Text(text = ": ", style = style.copy(color = Color.Gray))
        Text(text = value, style = style)
    }
}