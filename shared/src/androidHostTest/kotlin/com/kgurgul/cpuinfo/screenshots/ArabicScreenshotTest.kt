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
import com.kgurgul.cpuinfo.features.tests.HwTest
import com.kgurgul.cpuinfo.features.tests.TestsScreen
import com.kgurgul.cpuinfo.features.tests.TouchGrid
import com.kgurgul.cpuinfo.features.tests.TouchTest
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
import com.kgurgul.cpuinfo.shared.test_sensor_accelerometer
import com.kgurgul.cpuinfo.shared.test_sensor_gyroscope
import com.kgurgul.cpuinfo.shared.test_sensor_proximity
import com.kgurgul.cpuinfo.shared.test_sensor_light
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

    private val sampleSnapshot = BatterySnapshot(
        levelPercent = 80, health = Res.string.battery_good, state = Res.string.battery_discharging,
        technology = "Li-ion", voltageV = 4.12f, temperatureC = 31.5f, currentMa = -420,
        designMah = 5000, chargeCounterMah = 3680, cycleCount = 212,
    )

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
    fun about() = snap("about") {
        SettingsScreen(SettingsViewModel.UiState(), {}, {}, {})
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

    @Test
    @Config(qualifiers = "+h1700dp")
    fun batteryHistory() = snap("battery-history") {
        val day = com.kgurgul.cpuinfo.features.battery.BatteryHistory.DAY_MS
        val now = 400 * day
        val samples = (0 until 28).map { i ->
            com.kgurgul.cpuinfo.features.battery.BatterySample(
                now - (27 - i) * day, 80, 95 - i / 5 - (i % 3), 190 + i, 30f + i % 5,
            )
        }
        BatteryHealthScreen(
            BatteryHealthViewModel.UiState(isLoading = false, snapshot = sampleSnapshot),
            extras = com.kgurgul.cpuinfo.features.battery.BatteryExtras(
                history = samples, nowMs = now, systemHealth = 93, systemHealthDate = "22 أكتوبر 2026",
                importStatus = com.kgurgul.cpuinfo.features.battery.ImportStatus.Found(93), alertEnabled = true,
            ),
        )
    }

    @Test
    fun dumpstateHelp() = snap("dumpstate-help") {
        com.kgurgul.cpuinfo.features.battery.DumpstateHelpContent()
    }

    @Test
    fun tests() = snap("tests") {
        TestsScreen(mapOf(HwTest.SCREEN to true, HwTest.TOUCH to true, HwTest.SPEAKER to false, HwTest.SENSORS to true))
    }

    @Test
    fun sensors() = snap("sensors") {
        com.kgurgul.cpuinfo.features.tests.SensorsContent(
            listOf(
                com.kgurgul.cpuinfo.features.tests.SensorReading(Res.string.test_sensor_accelerometer, true, "0.12  9.79  0.31"),
                com.kgurgul.cpuinfo.features.tests.SensorReading(Res.string.test_sensor_gyroscope, true, "0.00  0.01  -0.02"),
                com.kgurgul.cpuinfo.features.tests.SensorReading(Res.string.test_sensor_proximity, true, "5.00"),
                com.kgurgul.cpuinfo.features.tests.SensorReading(Res.string.test_sensor_light, false, null),
            ),
            onResult = {},
        )
    }

    @Test
    fun report() = snap("report") {
        val report = com.kgurgul.cpuinfo.features.tests.TestReport.from(
            mapOf(HwTest.SCREEN to true, HwTest.TOUCH to true, HwTest.SPEAKER to false, HwTest.SENSORS to true, HwTest.CHARGING to true),
            model = "Xiaomi Redmi Note 13 5G", androidVersion = "14 (API 34)", batteryHealthPercent = 92, date = "23 أكتوبر 2026",
        )
        val text = com.kgurgul.cpuinfo.features.tests.rememberReportText(report, "\u206692%\u2069 (تقديري) · جيدة")
        com.kgurgul.cpuinfo.features.tests.ReportContent(text)
        // Also exercise the PNG renderer used for sharing.
        val bmp = com.kgurgul.cpuinfo.features.tests.renderReportBitmap(text, rtl = true, brand = 0xFF0B4F8A.toInt())
        val dir = File(System.getProperty("screenshots.dir") ?: "build/screens").apply { mkdirs() }
        File(dir, "report-share.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun touch() = snap("touch") {
        val grid = TouchGrid(cols = 8, rows = 14)
        for (r in 0 until 14) for (c in 0 until 8) if (r < 9 || c < 3) grid.touch(c + 0.5f, r + 0.5f, 8f, 14f)
        TouchTest(onResult = {}, exitRequested = true, grid = grid)
    }
}
