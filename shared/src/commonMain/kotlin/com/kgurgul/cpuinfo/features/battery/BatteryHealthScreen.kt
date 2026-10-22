package com.kgurgul.cpuinfo.features.battery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.kgurgul.cpuinfo.features.information.base.InformationRow
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
    val rows = buildList {
        add(stringResource(Res.string.level) to (s.levelPercent?.let { "$it%" } ?: na))
        add(stringResource(Res.string.battery_state) to (s.state?.let { stringResource(it) } ?: na))
        add(stringResource(Res.string.battery_health) to (s.health?.let { stringResource(it) } ?: na))
        add(stringResource(Res.string.battery_design_capacity) to (s.designMah?.let { ltr("$it mAh") } ?: na))
        add(stringResource(Res.string.battery_full_capacity) to (fullMah?.let { ltr("$it mAh") + " ($estimated)" } ?: na))
        add(stringResource(Res.string.battery_health_percent) to (healthPct?.let { ltr("$it%") + " ($estimated)" } ?: na))
        s.cycleCount?.let { add(stringResource(Res.string.battery_cycle_count) to it.toString()) }
        add(stringResource(Res.string.battery_live_current) to (s.currentMa?.let { ltr("$it mA") } ?: na))
        add(stringResource(Res.string.battery_power) to (power?.let { ltr("${fmt1(it)} W") } ?: na))
        add(stringResource(Res.string.voltage) to (s.voltageV?.let { ltr("${fmt2(it.toDouble())} V") } ?: na))
        add(stringResource(Res.string.temperature) to (s.temperatureC?.let { ltr("${fmt1(it.toDouble())} °C") } ?: na))
        add(
            stringResource(Res.string.battery_charger) to
                (if (s.chargers.isEmpty()) stringResource(Res.string.battery_unplugged)
                else s.chargers.map { stringResource(it) }.joinToString(" + "))
        )
        add(stringResource(Res.string.technology) to (s.technology ?: na))
    }
    Scaffold(
        topBar = { PrimaryTopAppBar(title = stringResource(Res.string.battery)) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(spacingSmall),
            verticalArrangement = Arrangement.spacedBy(spacingSmall),
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) {
            itemsIndexed(rows) { index, (title, value) ->
                InformationRow(title = title, value = value, isLastItem = index == rows.lastIndex)
            }
            item {
                Text(
                    text = stringResource(Res.string.battery_estimate_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(spacingMedium),
                )
            }
        }
    }
}
