package com.kgurgul.cpuinfo.screenshots

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.kgurgul.cpuinfo.domain.model.ItemValue
import com.kgurgul.cpuinfo.features.battery.BatteryHealthScreen
import com.kgurgul.cpuinfo.features.battery.BatteryHealthViewModel
import com.kgurgul.cpuinfo.features.battery.BatterySnapshot
import com.kgurgul.cpuinfo.shared.battery_discharging
import com.kgurgul.cpuinfo.shared.hardware_usb
import com.kgurgul.cpuinfo.domain.model.RamData
import com.kgurgul.cpuinfo.features.information.hardware.HardwareInfoScreen
import com.kgurgul.cpuinfo.features.information.hardware.HardwareInfoViewModel
import com.kgurgul.cpuinfo.features.information.os.OsInfoScreen
import com.kgurgul.cpuinfo.features.information.os.OsInfoViewModel
import com.kgurgul.cpuinfo.features.information.ram.RamInfoScreen
import com.kgurgul.cpuinfo.features.information.ram.RamInfoViewModel
import com.kgurgul.cpuinfo.features.settings.SettingsScreen
import com.kgurgul.cpuinfo.features.settings.SettingsViewModel
import com.kgurgul.cpuinfo.shared.Res
import com.kgurgul.cpuinfo.shared.battery
import com.kgurgul.cpuinfo.shared.battery_health
import com.kgurgul.cpuinfo.shared.battery_good
import com.kgurgul.cpuinfo.shared.level
import com.kgurgul.cpuinfo.shared.temperature
import com.kgurgul.cpuinfo.shared.voltage
import com.kgurgul.cpuinfo.shared.brand
import com.kgurgul.cpuinfo.shared.model
import com.kgurgul.cpuinfo.shared.manufacturer
import com.kgurgul.cpuinfo.shared.os_security_patch
import com.kgurgul.cpuinfo.ui.theme.CpuInfoTheme
import java.io.File
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ar-rEG-ldrtl-w411dp-h891dp-night-xxhdpi")
class ArabicScreenshotTest {

    @get:Rule val composeRule = createComposeRule()

    private fun snap(name: String, content: @Composable () -> Unit) {
        composeRule.setContent { CpuInfoTheme(useDarkTheme = true) {
                // Host-test manifest has no supportsRtl, so force RTL as the app does on device.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(color = MaterialTheme.colorScheme.background) { content() }
                }
            } }
        composeRule.waitForIdle()
        val dir = File(System.getProperty("screenshots.dir") ?: "build/screens").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun settings() = snap("settings") {
        SettingsScreen(SettingsViewModel.UiState(), {}, {}, {})
    }

    @Test
    fun ram() = snap("ram") {
        RamInfoScreen(
            RamInfoViewModel.UiState(
                isInitializing = false,
                ramData = RamData(8_000_000_000, 3_200_000_000, 40, 500_000_000, emptyList()),
            )
        )
    }

    @Test
    fun hardware() = snap("hardware") {
        HardwareInfoScreen(
            HardwareInfoViewModel.UiState(
                isInitializing = false,
                hardwareItems = persistentListOf(
                    ItemValue.NameResource(Res.string.battery, ""),
                    ItemValue.NameResource(Res.string.level, "85%"),
                    ItemValue.NameValueResource(Res.string.battery_health, Res.string.battery_good),
                    ItemValue.NameResource(Res.string.temperature, "31.5°C"),
                    ItemValue.NameResource(Res.string.voltage, "4.2 V"),
                ),
            )
        )
    }

    @Test
    fun os() = snap("os") {
        OsInfoScreen(
            OsInfoViewModel.UiState(
                isInitializing = false,
                items = persistentListOf(
                    ItemValue.NameResource(Res.string.brand, "Xiaomi"),
                    ItemValue.NameResource(Res.string.model, "Redmi Note 13 5G"),
                    ItemValue.NameResource(Res.string.manufacturer, "Xiaomi"),
                    ItemValue.NameResource(Res.string.os_security_patch, "2026-09-01"),
                ),
            ),
            onExpandableItemClick = {},
        )
    }

    @Test
    fun battery() = snap("battery") {
        BatteryHealthScreen(
            BatteryHealthViewModel.UiState(
                isLoading = false,
                snapshot = BatterySnapshot(
                    levelPercent = 80,
                    health = Res.string.battery_good,
                    state = Res.string.battery_discharging,
                    chargers = emptyList(),
                    technology = "Li-ion",
                    voltageV = 4.12f,
                    temperatureC = 31.5f,
                    currentMa = -420,
                    designMah = 5000,
                    chargeCounterMah = 3680,
                    cycleCount = 212,
                ),
            )
        )
    }
}
