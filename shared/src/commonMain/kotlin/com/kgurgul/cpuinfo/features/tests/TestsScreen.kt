package com.kgurgul.cpuinfo.features.tests

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.ListItem
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

enum class HwTest(val title: StringResource) {
    SCREEN(Res.string.test_screen),
    TOUCH(Res.string.test_touch),
    SPEAKER(Res.string.test_speaker),
    MIC(Res.string.test_mic),
    CAMERA(Res.string.test_camera),
}

/** Results for the current app session only (not persisted). */
object TestSession {
    val results = mutableStateMapOf<HwTest, Boolean>()
}

/** Speaker, microphone and camera tests need platform APIs. */
@Composable internal expect fun DeviceTest(test: HwTest, onResult: (Boolean) -> Unit)

@Composable
fun TestsScreen(results: Map<HwTest, Boolean> = TestSession.results) {
    var open by remember { mutableStateOf<HwTest?>(null) }
    Scaffold(
        topBar = { PrimaryTopAppBar(title = stringResource(Res.string.tests)) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            items(HwTest.entries) { test ->
                val result = results[test]
                ListItem(
                    headlineContent = { Text(stringResource(test.title)) },
                    trailingContent = {
                        Text(
                            text = stringResource(
                                when (result) {
                                    true -> Res.string.test_pass
                                    false -> Res.string.test_fail
                                    null -> Res.string.test_not_run
                                }
                            ),
                            color = when (result) {
                                true -> Color(0xFF4CAF50)
                                false -> MaterialTheme.colorScheme.error
                                null -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                    modifier = Modifier.clickable { open = test },
                )
            }
        }
    }
    open?.let { test ->
        var exitRequested by remember(test) { mutableStateOf(false) }
        val close = { open = null }
        val onResult = { pass: Boolean ->
            TestSession.results[test] = pass
            close()
        }
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
                when (test) {
                    HwTest.SCREEN -> ScreenTest(onResult)
                    HwTest.TOUCH -> TouchTest(onResult, exitRequested)
                    else -> DeviceTest(test, onResult)
                }
            }
        }
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
