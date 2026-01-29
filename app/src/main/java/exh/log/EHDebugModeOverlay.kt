package exh.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
    val actualFps by PlayerStats.estimatedDisplayFps.collectAsState(0.0)
    
    val isInterpolating by PlayerStats.isInterpolating.collectAsState(false)
    val videoSync by PlayerStats.videoSync.collectAsState("")
    val tscale by PlayerStats.tscale.collectAsState("")
    val delayedFrames by PlayerStats.delayedFrames.collectAsState(0L)
    val mistime by PlayerStats.mistime.collectAsState(0.0)
    val voPasses by PlayerStats.voPasses.collectAsState(0L)
    
    val hwdec = MPVLib.getPropertyString("hwdec-current") ?: "no"
    val videoW by PlayerStats.videoW.collectAsState(0L)
    val videoH by PlayerStats.videoH.collectAsState(0L)
    val videoOutW by PlayerStats.videoOutW.collectAsState(0L)
    val videoOutH by PlayerStats.videoOutH.collectAsState(0L)

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
        Text(text = "INTERPOLATION PIPELINE (PAGE 6)", style = baseStyle.copy(color = Color(0xFF33BBFF)))
        Spacer(Modifier.height(8.dp))

        // Pipeline Logic
        val isDirect = hwdec == "mediacodec"
        val isWorking = isInterpolating && !isDirect && voPasses > 1
        
        val statusText = when {
            isWorking -> "ACTIVE (Vulkan + Copy)"
            isDirect -> "BYPASSED (Direct HWDEC)"
            isInterpolating && voPasses <= 1 -> "STALLED (No output frames)"
            else -> "OFF"
        }
        
        StatLine("Pipeline", statusText, baseStyle.copy(color = if (isDirect) Color.Red else if (isWorking) Color.Green else Color.Unspecified))
        StatLine("Sync Mode", videoSync, baseStyle)
        StatLine("Algorithm", tscale.ifEmpty { "none" }, baseStyle)
        
        Spacer(Modifier.height(12.dp))

        // FPS Details with better labels
        val finalSourceFps = if (sourceFps > 0) sourceFps else if (containerFps > 0) containerFps else vfFps
        
        StatLine("Source Rate", "${format.format(finalSourceFps)} fps", baseStyle)
        StatLine("Filter Output", "${format.format(vfFps)} fps", baseStyle)
        StatLine("Actual Display", "${format.format(actualFps)} fps", baseStyle.copy(color = if (actualFps >= 58 && isWorking) Color.Green else Color.Unspecified))
        StatLine("Refresh Rate", "${format.format(displayFps)} Hz", baseStyle)
        
        Spacer(Modifier.height(8.dp))
        
        // Performance
        StatLine("Mistime", "${(mistime * 1000).toInt()} ms", baseStyle)
        StatLine("Dropped", "$delayedFrames frames", baseStyle.copy(color = if (delayedFrames > 0) Color.Red else Color.Unspecified))

        Spacer(Modifier.height(12.dp))

        // Hardware details
        val finalW = if (videoW > 0) videoW else videoOutW
        val finalH = if (videoH > 0) videoH else videoOutH
        Row {
            StatLine("Res", "${finalW}x${finalH}", baseStyle)
            Text(" | ", style = baseStyle)
            StatLine("HW", hwdec, baseStyle)
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