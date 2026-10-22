package com.kgurgul.cpuinfo.features.battery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kgurgul.cpuinfo.shared.battery_section_capacity
import com.kgurgul.cpuinfo.shared.battery_section_info
import com.kgurgul.cpuinfo.shared.battery_section_live
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.kgurgul.cpuinfo.shared.Res
import com.kgurgul.cpuinfo.shared.battery
import com.kgurgul.cpuinfo.shared.battery_charger
import com.kgurgul.cpuinfo.shared.battery_cycle_count
import com.kgurgul.cpuinfo.shared.battery_design_capacity
import com.kgurgul.cpuinfo.shared.battery_estimate_note
import com.kgurgul.cpuinfo.shared.battery_estimated_value
import com.kgurgul.cpuinfo.shared.battery_full_capacity
import com.kgurgul.cpuinfo.shared.battery_health
import com.kgurgul.cpuinfo.shared.battery_health_percent
import com.kgurgul.cpuinfo.shared.battery_live_current
import com.kgurgul.cpuinfo.shared.battery_not_available
import com.kgurgul.cpuinfo.shared.battery_power
import com.kgurgul.cpuinfo.shared.battery_state
import com.kgurgul.cpuinfo.shared.battery_unplugged
import com.kgurgul.cpuinfo.shared.level
import com.kgurgul.cpuinfo.shared.technology
import com.kgurgul.cpuinfo.shared.temperature
import com.kgurgul.cpuinfo.shared.voltage
import com.kgurgul.cpuinfo.ui.components.PrimaryTopAppBar
import com.kgurgul.cpuinfo.ui.theme.spacingMedium
import com.kgurgul.cpuinfo.ui.theme.spacingSmall
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object BatteryBaseRoute {
    @SerialName("battery") @Serializable data object BatteryRoute
}

fun NavGraphBuilder.batteryScreen() {
    navigation<BatteryBaseRoute>(startDestination = BatteryBaseRoute.BatteryRoute) {
        composable<BatteryBaseRoute.BatteryRoute> { BatteryHealthScreen() }
    }
}

@Composable
fun BatteryHealthScreen(viewModel: BatteryHealthViewModel = koinViewModel()) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    BatteryHealthScreen(uiState = uiState)
}

private fun fmt1(v: Double): String = (kotlin.math.round(v * 10) / 10.0).toString()

private fun fmt2(v: Double): String = (kotlin.math.round(v * 100) / 100.0).toString()

/** Keeps "number unit" left-to-right inside Arabic text (Unicode LTR isolate). */
private fun ltr(v: String): String = "\u2066$v\u2069"

@Composable
fun BatteryHealthScreen(uiState: BatteryHealthViewModel.UiState) {
    val s = uiState.snapshot
    val na = stringResource(Res.string.battery_not_available)
    val estimated = stringResource(Res.string.battery_estimated_value)
    val fullMah = BatteryMath.estimateFullMah(s.chargeCounterMah, s.levelPercent)
    val healthPct = BatteryMath.healthPercent(fullMah, s.designMah)
    val power = BatteryMath.powerW(s.currentMa, s.voltageV)
    val capacity = buildList {
        add(Triple(Icons.Outlined.BatteryStd, stringResource(Res.string.battery_design_capacity), s.designMah?.let { ltr("$it mAh") } ?: na))
        add(Triple(Icons.Outlined.BatteryFull, stringResource(Res.string.battery_full_capacity), fullMah?.let { ltr("$it mAh") + " ($estimated)" } ?: na))
        add(Triple(Icons.Outlined.Favorite, stringResource(Res.string.battery_health), s.health?.let { stringResource(it) } ?: na))
        s.cycleCount?.let { add(Triple(Icons.Outlined.Loop, stringResource(Res.string.battery_cycle_count), it.toString())) }
    }
    val live = listOf(
        Triple(Icons.Outlined.ElectricBolt, stringResource(Res.string.battery_live_current), s.currentMa?.let { ltr("$it mA") } ?: na),
        Triple(Icons.Outlined.Power, stringResource(Res.string.battery_power), power?.let { ltr("${fmt1(it)} W") } ?: na),
        Triple(Icons.Outlined.Speed, stringResource(Res.string.voltage), s.voltageV?.let { ltr("${fmt2(it.toDouble())} V") } ?: na),
        Triple(Icons.Outlined.Thermostat, stringResource(Res.string.temperature), s.temperatureC?.let { ltr("${fmt1(it.toDouble())} °C") } ?: na),
    )
    val info = listOf(
        Triple(
            Icons.Outlined.Cable,
            stringResource(Res.string.battery_charger),
            if (s.chargers.isEmpty()) stringResource(Res.string.battery_unplugged)
            else s.chargers.map { stringResource(it) }.joinToString(" + "),
        ),
        Triple(Icons.Outlined.Memory, stringResource(Res.string.technology), s.technology ?: na),
    )
    Scaffold(
        topBar = { PrimaryTopAppBar(title = stringResource(Res.string.battery)) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(spacingMedium),
            verticalArrangement = Arrangement.spacedBy(spacingMedium),
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) {
            item {
                HeaderCard(
                    healthPct = healthPct,
                    estimated = estimated,
                    na = na,
                    level = s.levelPercent,
                    state = s.state?.let { stringResource(it) } ?: na,
                )
            }
            item { GroupCard(stringResource(Res.string.battery_section_capacity), capacity) }
            item { GroupCard(stringResource(Res.string.battery_section_live), live) }
            item { GroupCard(stringResource(Res.string.battery_section_info), info) }
            item {
                Text(
                    text = stringResource(Res.string.battery_estimate_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = spacingSmall),
                )
            }
        }
    }
}

private fun gaugeColor(pct: Int?, primary: Color): Color = when {
    pct == null -> primary
    pct >= 80 -> Color(0xFF43A047)
    pct >= 60 -> Color(0xFFF9A825)
    else -> Color(0xFFC62828)
}

@Composable
private fun HeaderCard(healthPct: Int?, estimated: String, na: String, level: Int?, state: String) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val color = gaugeColor(healthPct, MaterialTheme.colorScheme.secondary)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(spacingMedium),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 14.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(track, 135f, 270f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    val sweep = 270f * ((healthPct ?: 0).coerceIn(0, 100) / 100f)
                    if (sweep > 0f) drawArc(color, 135f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = healthPct?.let { ltr("$it%") } ?: na,
                        style = if (healthPct != null) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(Res.string.battery_health_percent),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (healthPct != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Text(
                                text = estimated,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(spacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.level),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = level?.let { ltr("$it%") } ?: na,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                LinearProgressIndicator(
                    progress = { (level ?: 0) / 100f },
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth().padding(vertical = spacingSmall).height(8.dp),
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    text = state,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun GroupCard(title: String, rows: List<Triple<ImageVector, String, String>>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = spacingSmall)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = spacingMedium, vertical = spacingSmall),
            )
            rows.forEachIndexed { i, (icon, label, value) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = spacingMedium, vertical = 10.dp),
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(spacingMedium))
                    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
                }
                if (i != rows.lastIndex) HorizontalDivider(modifier = Modifier.padding(horizontal = spacingMedium), color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
