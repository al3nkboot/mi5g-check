package com.kgurgul.cpuinfo.features.tests

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.SmartButton
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Speed
import com.kgurgul.cpuinfo.shared.perf_title
import com.kgurgul.cpuinfo.shared.perf_hint
import com.kgurgul.cpuinfo.shared.used_check
import com.kgurgul.cpuinfo.shared.used_check_hint
import com.kgurgul.cpuinfo.shared.used_check_step
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextButton
import com.kgurgul.cpuinfo.shared.test_buttons
import com.kgurgul.cpuinfo.shared.test_charging
import com.kgurgul.cpuinfo.shared.test_connectivity
import com.kgurgul.cpuinfo.shared.test_fingerprint
import com.kgurgul.cpuinfo.shared.test_flashlight
import com.kgurgul.cpuinfo.shared.test_sensors
import com.kgurgul.cpuinfo.shared.test_vibration
import com.kgurgul.cpuinfo.shared.test_full
import com.kgurgul.cpuinfo.shared.test_skip
import com.kgurgul.cpuinfo.shared.report
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.kgurgul.cpuinfo.shared.tests_summary
import com.kgurgul.cpuinfo.shared.tests_summary_hint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.kgurgul.cpuinfo.shared.Res
import com.kgurgul.cpuinfo.shared.test_camera
import com.kgurgul.cpuinfo.shared.test_fail
import com.kgurgul.cpuinfo.shared.test_mic
import com.kgurgul.cpuinfo.shared.test_not_run
import com.kgurgul.cpuinfo.shared.test_pass
import com.kgurgul.cpuinfo.shared.test_screen
import com.kgurgul.cpuinfo.shared.test_screen_hint
import com.kgurgul.cpuinfo.shared.test_speaker
import com.kgurgul.cpuinfo.shared.test_touch
import com.kgurgul.cpuinfo.shared.test_touch_hint
import com.kgurgul.cpuinfo.shared.tests
import com.kgurgul.cpuinfo.ui.components.PrimaryTopAppBar
import com.kgurgul.cpuinfo.ui.theme.spacingMedium
import com.kgurgul.cpuinfo.ui.theme.spacingSmall
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Serializable
data object TestsBaseRoute {
    @SerialName("tests") @Serializable data object TestsRoute
}

fun NavGraphBuilder.testsScreen() {
    navigation<TestsBaseRoute>(startDestination = TestsBaseRoute.TestsRoute) {
        composable<TestsBaseRoute.TestsRoute> { TestsScreen() }
    }
}

enum class HwTest(val title: StringResource, val icon: ImageVector) {
    SCREEN(Res.string.test_screen, Icons.Outlined.Smartphone),
    TOUCH(Res.string.test_touch, Icons.Outlined.TouchApp),
    SPEAKER(Res.string.test_speaker, Icons.AutoMirrored.Outlined.VolumeUp),
    MIC(Res.string.test_mic, Icons.Outlined.Mic),
    CAMERA(Res.string.test_camera, Icons.Outlined.PhotoCamera),
    SENSORS(Res.string.test_sensors, Icons.Outlined.Sensors),
    VIBRATION(Res.string.test_vibration, Icons.Outlined.Vibration),
    BUTTONS(Res.string.test_buttons, Icons.Outlined.SmartButton),
    FLASHLIGHT(Res.string.test_flashlight, Icons.Outlined.FlashlightOn),
    CONNECTIVITY(Res.string.test_connectivity, Icons.Outlined.Wifi),
    FINGERPRINT(Res.string.test_fingerprint, Icons.Outlined.Fingerprint),
    CHARGING(Res.string.test_charging, Icons.Outlined.BatteryChargingFull),
}

private val PassGreen = Color(0xFF43A047)

@Composable
private fun TestsSummary(passed: Int, total: Int, done: Int, onFullTest: () -> Unit, onReport: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(spacingMedium)) {
            Text(
                text = "\u2066" + stringResource(Res.string.tests_summary, passed, total) + "\u2069",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else done / total.toFloat() },
                color = MaterialTheme.colorScheme.secondary,
                strokeCap = StrokeCap.Round,
                modifier = Modifier.fillMaxWidth().padding(vertical = spacingSmall).height(8.dp),
            )
            Text(
                text = stringResource(Res.string.tests_summary_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacingSmall),
                modifier = Modifier.fillMaxWidth().padding(top = spacingSmall),
            ) {
                Button(onClick = onFullTest, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.test_full))
                }
                FilledTonalButton(onClick = onReport, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.report))
                }
            }
        }
    }
}

@Composable
private fun TestCard(test: HwTest, result: Boolean?, onClick: () -> Unit) {
    val (chipBg, chipFg, chipIcon) = when (result) {
        true -> Triple(PassGreen.copy(alpha = 0.18f), PassGreen, Icons.Filled.CheckCircle)
        false -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Icons.Filled.Cancel)
        null -> Triple(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant, Icons.AutoMirrored.Outlined.HelpOutline)
    }
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(spacingMedium)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(test.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Spacer(Modifier.width(spacingMedium))
            Text(
                text = stringResource(test.title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Surface(shape = RoundedCornerShape(50), color = chipBg) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Icon(chipIcon, contentDescription = null, tint = chipFg, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(
                            when (result) {
                                true -> Res.string.test_pass
                                false -> Res.string.test_fail
                                null -> Res.string.test_not_run
                            }
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = chipFg,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionCard(icon: ImageVector, title: StringResource, hint: StringResource, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(spacingMedium)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(spacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Results for the current app session only (not persisted). */
object TestSession {
    val results = mutableStateMapOf<HwTest, Boolean>()
}

/** Speaker, microphone and camera tests need platform APIs. */
@Composable internal expect fun DeviceTest(test: HwTest, onResult: (Boolean) -> Unit)

/** Full-screen report with share-as-image/PDF actions (platform rendering and sharing). */
@Composable internal expect fun ReportView(results: Map<HwTest, Boolean>, usedPhone: Boolean, onClose: () -> Unit)

@Composable
fun TestsScreen(results: Map<HwTest, Boolean> = TestSession.results) {
    var open by remember { mutableStateOf<HwTest?>(null) }
    var run by remember { mutableStateOf<FullTestRun?>(null) }
    var showReport by remember { mutableStateOf(false) }
    var showPerf by remember { mutableStateOf(false) }
    // Non-null during/after the «فحص جوال مستعمل» wizard: only this run's answers go into its report.
    var usedResults by remember { mutableStateOf<Map<HwTest, Boolean>?>(null) }
    // Called when a test finishes (null = skipped/closed); during a full run moves to the next test.
    val finish = { test: HwTest, pass: Boolean? ->
        if (pass != null) TestSession.results[test] = pass
        if (pass != null) usedResults = usedResults?.plus(test to pass)
        val r = run
        if (r != null) {
            r.advance(pass)
            open = r.current
            if (r.isFinished) {
                run = null
                showReport = true
            }
        } else {
            open = null
        }
    }
    Scaffold(
        topBar = { PrimaryTopAppBar(title = stringResource(Res.string.tests)) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(spacingMedium),
            verticalArrangement = Arrangement.spacedBy(spacingSmall),
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) {
            item {
                TestsSummary(
                    passed = results.values.count { it },
                    total = HwTest.entries.size,
                    done = results.size,
                    onFullTest = { usedResults = null; run = FullTestRun().also { open = it.current } },
                    onReport = { usedResults = null; showReport = true },
                )
            }
            item {
                ActionCard(Icons.AutoMirrored.Outlined.FactCheck, Res.string.used_check, Res.string.used_check_hint) {
                    usedResults = emptyMap()
                    run = FullTestRun().also { open = it.current }
                }
            }
            item { ActionCard(Icons.Outlined.Speed, Res.string.perf_title, Res.string.perf_hint) { showPerf = true } }
            items(HwTest.entries) { test ->
                TestCard(test = test, result = results[test], onClick = { open = test })
            }
        }
    }
    open?.let { test ->
        var exitRequested by remember(test) { mutableStateOf(false) }
        val close = { finish(test, null) }
        val onResult = { pass: Boolean -> finish(test, pass) }
        Dialog(
            // Back on an unfinished touch test first offers pass/fail (a dead area can't be covered).
            onDismissRequest = {
                if (test == HwTest.TOUCH && !exitRequested) exitRequested = true else close()
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                // Wizard bar sits above the test (not over it) so it never hides the test's own text.
                Column {
                    run?.let { r -> WizardBar(r.index + 1, r.total, close) }
                    Box(Modifier.weight(1f)) {
                        androidx.compose.runtime.key(test) {
                            when (test) {
                                HwTest.SCREEN -> ScreenTest(onResult)
                                HwTest.TOUCH -> TouchTest(onResult, exitRequested)
                                else -> DeviceTest(test, onResult)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showPerf) {
        Dialog(
            onDismissRequest = { showPerf = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                com.kgurgul.cpuinfo.features.perf.PerformanceView { showPerf = false }
            }
        }
    }
    if (showReport) {
        Dialog(
            onDismissRequest = { showReport = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                ReportView(usedResults ?: results, usedResults != null) { showReport = false }
            }
        }
    }
}

/** Step counter and skip action shown over each test during a guided run. */
@Composable
fun WizardBar(step: Int, total: Int, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().safeDrawingPadding().padding(horizontal = spacingSmall),
    ) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(
                "\u2068" + stringResource(Res.string.used_check_step, step, total) + "\u2069",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = onSkip,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        ) { Text(stringResource(Res.string.test_skip)) }
    }
}

@Composable
fun ResultBar(onResult: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacingMedium),
        modifier = modifier.fillMaxWidth().padding(spacingMedium),
    ) {
        Button(
            onClick = { onResult(true) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White),
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(Res.string.test_pass)) }
        Button(
            onClick = { onResult(false) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(Res.string.test_fail)) }
    }
}

private val screenColors = listOf(Color.Red, Color.Green, Color.Blue, Color.White, Color.Black)

@Composable
fun ScreenTest(onResult: (Boolean) -> Unit) {
    var taps by remember { mutableIntStateOf(0) }
    val color = screenColors[taps % screenColors.size]
    Box(modifier = Modifier.fillMaxSize().background(color).clickable { taps++ }) {
        if (taps == 0) {
            Text(
                text = stringResource(Res.string.test_screen_hint),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(spacingMedium),
            )
        }
        if (taps >= screenColors.size - 1) {
            ResultBar(onResult, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
        }
    }
}

@Composable
fun TouchTest(
    onResult: (Boolean) -> Unit,
    exitRequested: Boolean = false,
    grid: TouchGrid = remember { TouchGrid(cols = 8, rows = 14) },
) {
    var version by remember { mutableIntStateOf(0) }
    val fill = Color(0xFF2E7D32)
    val line = MaterialTheme.colorScheme.outline
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(grid) {
                awaitEachGesture {
                    var change = awaitFirstDown()
                    while (true) {
                        val p = change.position
                        if (grid.touch(p.x, p.y, size.width.toFloat(), size.height.toFloat())) version++
                        change.consume()
                        change = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: break
                    }
                }
            }
        ) {
            version // read to redraw on change
            val cw = size.width / grid.cols
            val ch = size.height / grid.rows
            for (r in 0 until grid.rows) for (c in 0 until grid.cols) {
                val topLeft = Offset(c * cw, r * ch)
                if (grid.isCovered(c, r)) drawRect(fill, topLeft, Size(cw, ch))
                drawRect(line, topLeft, Size(cw, ch), style = Stroke(1.dp.toPx()))
            }
        }
        if (grid.coveredCount == 0) {
            Text(
                text = stringResource(Res.string.test_touch_hint),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(spacingMedium),
            )
        }
        if (grid.isComplete || exitRequested) {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            ) {
                Column(modifier = Modifier.safeDrawingPadding().padding(top = spacingSmall)) {
                    Text(
                        text = "\u2066${grid.coveredCount} / ${grid.cols * grid.rows}\u2069",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    ResultBar(onResult)
                }
            }
        }
    }
}
