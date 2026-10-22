package com.kgurgul.cpuinfo.features.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.jetbrains.compose.resources.StringResource

/** Raw-ish battery readings; null means unavailable on this device. */
data class BatterySnapshot(
    val levelPercent: Int? = null,
    val health: StringResource? = null,
    val state: StringResource? = null,
    val chargers: List<StringResource> = emptyList(),
    val technology: String? = null,
    val voltageV: Float? = null,
    val temperatureC: Float? = null,
    val currentMa: Int? = null,
    val designMah: Int? = null,
    val chargeCounterMah: Int? = null,
    val cycleCount: Int? = null,
)

interface IBatteryHealthProvider {
    fun read(): BatterySnapshot
}

class BatteryHealthViewModel(provider: IBatteryHealthProvider) : ViewModel() {

    val uiStateFlow =
        flow {
                while (true) {
                    emit(UiState(isLoading = false, snapshot = provider.read()))
                    delay(1_000)
                }
            }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    data class UiState(val isLoading: Boolean = true, val snapshot: BatterySnapshot = BatterySnapshot())
}
