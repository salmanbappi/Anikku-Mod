package exh.log

<<<<<<< HEAD
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun InterpolationStatsOverlay() {
    LaunchedEffect(Unit) {
        while (true) {
            PlayerStats.estimatedVfFps.value = MPVLib.getPropertyDouble("estimated-vf-fps") ?: 0.0
            PlayerStats.videoParamsFps.value = MPVLib.getPropertyDouble("video-params/fps") ?: 0.0
            PlayerStats.containerFps.value = MPVLib.getPropertyDouble("container-fps") ?: 0.0
            PlayerStats.displayFps.value = MPVLib.getPropertyDouble("display-fps") ?: 0.0
            PlayerStats.estimatedDisplayFps.value = MPVLib.getPropertyDouble("estimated-display-fps") ?: 0.0

            PlayerStats.isInterpolating.value = MPVLib.getPropertyBoolean("interpolation") ?: false
            PlayerStats.videoSync.value = MPVLib.getPropertyString("video-sync") ?: ""
            PlayerStats.tscale.value = MPVLib.getPropertyString("tscale") ?: ""
            PlayerStats.delayedFrames.value = MPVLib.getPropertyInt("vo-delayed-frame-count")?.toLong() ?: 0L
            PlayerStats.mistime.value = MPVLib.getPropertyDouble("mistime") ?: 0.0
            PlayerStats.voPasses.value = MPVLib.getPropertyInt("vo-passes")?.toLong() ?: 0L

            PlayerStats.hwdec.value = MPVLib.getPropertyString("hwdec-current") ?: ""
            PlayerStats.videoW.value = MPVLib.getPropertyInt("video-params/w")?.toLong() ?: 0L
            PlayerStats.videoH.value = MPVLib.getPropertyInt("video-params/h")?.toLong() ?: 0L
            PlayerStats.videoOutW.value = MPVLib.getPropertyInt("video-out-params/w")?.toLong() ?: 0L
            PlayerStats.videoOutH.value = MPVLib.getPropertyInt("video-out-params/h")?.toLong() ?: 0L
            PlayerStats.dwidth.value = MPVLib.getPropertyInt("dwidth")?.toLong() ?: 0L
            PlayerStats.dheight.value = MPVLib.getPropertyInt("dheight")?.toLong() ?: 0L

            delay(1000)
        }
    }
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
    
    val hwdec by PlayerStats.hwdec.collectAsState("")
    val videoW by PlayerStats.videoW.collectAsState(0L)
    val videoH by PlayerStats.videoH.collectAsState(0L)
    val videoOutW by PlayerStats.videoOutW.collectAsState(0L)
    val videoOutH by PlayerStats.videoOutH.collectAsState(0L)
    val dwidth by PlayerStats.dwidth.collectAsState(0L)
    val dheight by PlayerStats.dheight.collectAsState(0L)

    val format = remember {
        DecimalFormat(
            "0.0",
=======
import android.content.Context
import android.view.Choreographer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.util.system.isDebugBuildType
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.time.Duration.Companion.nanoseconds

@Composable
fun DebugModeOverlay() {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                        .only(WindowInsetsSides.Bottom.plus(WindowInsetsSides.Start)),
                )
                .align(Alignment.BottomStart)
                .background(Color(0x7F000000))
                .padding(4.dp),
        ) {
            FpsDebugModeOverlay()
            EHDebugModeOverlay()
        }
    }
}

@Composable
private fun FpsDebugModeOverlay() {
    val fps by remember { FpsState(FpsState.DEFAULT_INTERVAL) }
    val format = remember {
        DecimalFormat(
            "'fps:' 0.0",
>>>>>>> official/master
            DecimalFormatSymbols.getInstance(Locale.ENGLISH),
        )
    }

<<<<<<< HEAD
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

        // Pipeline Logic
        val isDirect = hwdec == "mediacodec"
        // Improved detection: Check if output frames > 1 OR display FPS is high OR algorithm is active
        val isWorking = isInterpolating && !isDirect && (voPasses > 1 || actualFps > (vfFps + 5) || (tscale.isNotEmpty() && tscale != "none"))
        
        val statusText = when {
            isWorking -> "ACTIVE"
            isDirect -> "BYPASSED (Direct HWDEC)"
            isInterpolating && !isWorking -> "WAITING (Preparing frames)"
            else -> "OFF"
        }
        StatLine("Status", statusText, baseStyle.copy(color = if (isDirect) Color.Red else if (statusText == "ACTIVE") Color.Green else Color.Unspecified))
        StatLine("Sync Mode", videoSync, baseStyle)
        StatLine("Scaler", tscale.ifEmpty { "none" }, baseStyle)
        
        Spacer(Modifier.height(12.dp))

        // FPS Details with fallbacks
        val finalSourceFps = listOf(sourceFps, containerFps, vfFps).firstOrNull { it > 0.0 } ?: 0.0
        val finalActualFps = if (actualFps > 0) actualFps else vfFps
        
        StatLine("Source Rate", "${format.format(finalSourceFps)} fps", baseStyle)
        StatLine("Actual Display", "${format.format(finalActualFps)} fps", baseStyle.copy(color = if (finalActualFps >= 58) Color.Green else Color.Unspecified))
        StatLine("Refresh Rate", "${format.format(displayFps)} Hz", baseStyle)
        
        Spacer(Modifier.height(8.dp))
        
        StatLine("Mistime", "${(mistime * 1000).toInt()} ms", baseStyle)
        StatLine("Dropped", "$delayedFrames frames", baseStyle.copy(color = if (delayedFrames > 0) Color.Red else Color.Unspecified))

        Spacer(Modifier.height(12.dp))

        // Hardware details
        val finalW = listOf(dwidth, videoW, videoOutW).firstOrNull { it > 0L } ?: 0L
        val finalH = listOf(dheight, videoH, videoOutH).firstOrNull { it > 0L } ?: 0L
        Row {
            StatLine("Res", "${finalW}x${finalH}", baseStyle)
            Text(" | ", style = baseStyle)
            StatLine("HW", hwdec.ifEmpty { "no" }, baseStyle)
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
=======
    Text(
        text = remember(fps) {
            format.format(fps)
        },
        color = Color.White,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
    )
}

@Composable
private fun EHDebugModeOverlay() {
    val context = LocalContext.current
    Text(
        text = buildInfo(context),
        color = Color.White,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.1f.sp,
    )
}

private fun buildInfo(context: Context) = buildAnnotatedString {
    withStyle(SpanStyle(color = Color.Green)) {
        append("===[ ")
        append(context.stringResource(MR.strings.app_name))
        append(" ]===")
    }
    append('\n')
    appendItem("Build type:", BuildConfig.BUILD_TYPE)
    appendItem("Debug mode:", isDebugBuildType.asEnabledString())
    appendItem("Version code:", BuildConfig.VERSION_CODE.toString())
    appendItem("Commit SHA:", BuildConfig.COMMIT_SHA)
    appendItem("Log level:", EHLogLevel.currentLogLevel.name.lowercase(Locale.getDefault()))
}

fun AnnotatedString.Builder.appendItem(title: String, item: String, newLine: Boolean = true) {
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
        append(title)
    }
    append(' ')
    append(item)
    if (newLine) {
        append('\n')
    }
}

private fun Boolean.asEnabledString() = if (this) "enabled" else "disabled"

private class FpsState(private val interval: Int) :
    Choreographer.FrameCallback,
    RememberObserver,
    MutableState<Double> by mutableDoubleStateOf(0.0) {
    private val choreographer = Choreographer.getInstance()
    private var startFrameTimeMillis: Long = 0
    private var numFramesRendered = 0

    override fun onRemembered() {
        choreographer.postFrameCallback(this)
    }

    override fun onAbandoned() {
        choreographer.removeFrameCallback(this)
    }

    override fun onForgotten() {
        choreographer.removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        val currentFrameTimeMillis = frameTimeNanos.nanoseconds.inWholeMilliseconds
        if (startFrameTimeMillis > 0) {
            val duration = currentFrameTimeMillis - startFrameTimeMillis
            numFramesRendered++
            if (duration > interval) {
                value = (numFramesRendered * 1000f / duration).toDouble()
                startFrameTimeMillis = currentFrameTimeMillis
                numFramesRendered = 0
            }
        } else {
            startFrameTimeMillis = currentFrameTimeMillis
        }
        choreographer.postFrameCallback(this)
    }

    companion object {
        const val DEFAULT_INTERVAL = 1000
    }
}
>>>>>>> official/master
