package com.kgurgul.cpuinfo.features.battery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kgurgul.cpuinfo.shared.*
import com.kgurgul.cpuinfo.ui.theme.spacingMedium
import com.kgurgul.cpuinfo.ui.theme.spacingSmall
import org.jetbrains.compose.resources.stringResource

sealed interface ImportStatus {
    data object Scanning : ImportStatus
    data object NotFound : ImportStatus
    data object Error : ImportStatus
    data class Found(val percent: Int) : ImportStatus
}

data class BatteryExtras(
    val history: List<BatterySample> = emptyList(),
    val nowMs: Long = 0L,
    val initialRange: HistoryRange = HistoryRange.MONTH,
    val systemHealth: Int? = null,
    val systemHealthDate: String? = null,
    val importStatus: ImportStatus? = null,
    val alertEnabled: Boolean = false,
    val alertThresholdC: Int = TemperatureAlert.DEFAULT_THRESHOLD_C,
)

data class BatteryActions(
    val onImport: () -> Unit = {},
    val onHelp: () -> Unit = {},
    val onAlertEnabled: (Boolean) -> Unit = {},
    val onAlertThreshold: (Int) -> Unit = {},
)

private fun ltr(v: String): String = "\u2066$v\u2069"

@Composable
private fun CardTitle(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.secondary,
    fontWeight = FontWeight.Bold,
)

@Composable
internal fun HistoryCard(history: List<BatterySample>, nowMs: Long, initialRange: HistoryRange) {
    var range by remember { mutableStateOf(initialRange) }
    val shown = BatteryHistory.inRange(history, range, nowMs)
    val healthColor = MaterialTheme.colorScheme.secondary
    val cyclesColor = Color(0xFFF9A825)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(spacingMedium), verticalArrangement = Arrangement.spacedBy(spacingSmall)) {
            CardTitle(stringResource(Res.string.battery_history))
            Row(horizontalArrangement = Arrangement.spacedBy(spacingSmall)) {
                listOf(
                    HistoryRange.WEEK to Res.string.battery_range_7d,
                    HistoryRange.MONTH to Res.string.battery_range_30d,
                    HistoryRange.ALL to Res.string.battery_range_all,
                ).forEach { (r, label) ->
                    FilterChip(selected = range == r, onClick = { range = r }, label = { Text(stringResource(label)) })
                }
            }
            if (shown.size < 2) {
                Text(
                    stringResource(Res.string.battery_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val grid = MaterialTheme.colorScheme.outlineVariant
                Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    for (i in 0..4) {
                        val y = size.height * i / 4f
                        drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f)
                    }
                    val t0 = shown.first().timeMs
                    val span = (shown.last().timeMs - t0).coerceAtLeast(1L)
                    line(shown.map { it.timeMs to it.healthPercent?.toFloat() }, t0, span, healthColor)
                    line(shown.map { it.timeMs to it.cycles?.toFloat() }, t0, span, cyclesColor)
                }
                val h = shown.mapNotNull { it.healthPercent }
                val c = shown.mapNotNull { it.cycles }
                Row(horizontalArrangement = Arrangement.spacedBy(spacingMedium), verticalAlignment = Alignment.CenterVertically) {
                    Legend(healthColor, stringResource(Res.string.battery_history_health) +
                        if (h.isNotEmpty()) " " + ltr("${h.min()}–${h.max()}%") else "")
                    Legend(cyclesColor, stringResource(Res.string.battery_history_cycles) +
                        if (c.isNotEmpty()) " " + ltr("${c.min()}–${c.max()}") else "")
                }
                Text(
                    stringResource(Res.string.battery_history_count, shown.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Draws one series scaled to its own min/max (left-to-right in time regardless of layout). */
private fun DrawScope.line(points: List<Pair<Long, Float?>>, t0: Long, span: Long, color: Color) {
    val pts = points.filter { it.second != null }
    if (pts.isEmpty()) return
    val lo = pts.minOf { it.second!! }
    val hi = pts.maxOf { it.second!! }
    val pad = ((hi - lo) * 0.15f).coerceAtLeast(1f)
    val min = lo - pad
    val range = (hi + pad) - min
    fun pos(p: Pair<Long, Float?>) = Offset(
        size.width * ((p.first - t0).toFloat() / span),
        size.height * (1f - (p.second!! - min) / range),
    )
    val path = Path()
    pts.forEachIndexed { i, p -> pos(p).let { if (i == 0) path.moveTo(it.x, it.y) else path.lineTo(it.x, it.y) } }
    drawPath(path, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    pts.forEach { drawCircle(color, 3.5.dp.toPx(), pos(it)) }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = color, modifier = Modifier.size(10.dp)) {}
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun SystemHealthCard(extras: BatteryExtras, actions: BatteryActions) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(spacingMedium), verticalArrangement = Arrangement.spacedBy(spacingSmall)) {
            CardTitle(stringResource(Res.string.battery_system_health))
            Text(
                text = extras.systemHealth?.let {
                    stringResource(Res.string.battery_system_saved, ltr("$it%") + " (" + stringResource(Res.string.battery_from_system) + ")", extras.systemHealthDate ?: "")
                } ?: stringResource(Res.string.battery_system_none),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            extras.importStatus?.let { st ->
                Text(
                    text = when (st) {
                        ImportStatus.Scanning -> stringResource(Res.string.battery_import_scanning)
                        ImportStatus.NotFound -> stringResource(Res.string.battery_import_not_found)
                        ImportStatus.Error -> stringResource(Res.string.battery_import_error)
                        is ImportStatus.Found -> stringResource(Res.string.battery_import_found, ltr("${st.percent}%"))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (st is ImportStatus.NotFound || st is ImportStatus.Error) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = actions.onImport, enabled = extras.importStatus != ImportStatus.Scanning, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.FileOpen, contentDescription = null)
                Text(" " + stringResource(Res.string.battery_import_dumpstate))
            }
            TextButton(onClick = actions.onHelp) {
                Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null)
                Text(" " + stringResource(Res.string.battery_import_help))
            }
        }
    }
}

@Composable
internal fun TempAlertCard(extras: BatteryExtras, actions: BatteryActions) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(spacingMedium), verticalArrangement = Arrangement.spacedBy(spacingSmall)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { CardTitle(stringResource(Res.string.battery_temp_alert)) }
                Switch(checked = extras.alertEnabled, onCheckedChange = actions.onAlertEnabled)
            }
            Text(
                stringResource(Res.string.battery_temp_alert_desc, ltr("${extras.alertThresholdC} °C")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (extras.alertEnabled) {
                Slider(
                    value = extras.alertThresholdC.toFloat(),
                    onValueChange = { actions.onAlertThreshold(it.toInt()) },
                    valueRange = 35f..55f,
                    steps = 19,
                )
            }
        }
    }
}

/** Arabic/English steps to get a Samsung dumpstate via *#9900#. */
@Composable
fun DumpstateHelpContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(spacingMedium), verticalArrangement = Arrangement.spacedBy(spacingSmall)) {
        Text(stringResource(Res.string.dumpstate_help_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        listOf(
            Res.string.dumpstate_help_1, Res.string.dumpstate_help_2, Res.string.dumpstate_help_3,
            Res.string.dumpstate_help_4, Res.string.dumpstate_help_5, Res.string.dumpstate_help_6,
        ).forEachIndexed { i, step ->
            Row {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Spacer(Modifier.width(spacingMedium))
                Text(stringResource(step), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
        }
        Text(
            stringResource(Res.string.dumpstate_help_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
