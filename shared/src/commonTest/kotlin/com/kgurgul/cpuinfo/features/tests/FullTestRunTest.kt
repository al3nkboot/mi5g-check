package com.kgurgul.cpuinfo.features.tests

import com.kgurgul.cpuinfo.features.battery.BatteryMath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FullTestRunTest {
    @Test
    fun walksAllTestsInOrderWithSkips() {
        val run = FullTestRun()
        val recorded = mutableListOf<Pair<HwTest, Boolean?>>()
        HwTest.entries.forEachIndexed { i, test ->
            assertEquals(test, run.current)
            recorded += run.advance(if (i % 3 == 0) null else i % 2 == 0)!!
        }
        assertTrue(run.isFinished)
        assertNull(run.current)
        assertNull(run.advance(true))
        assertEquals(HwTest.entries, recorded.map { it.first })
        assertNull(recorded.first().second)
    }

    @Test
    fun reportListsEveryTestWithCounts() {
        val report = TestReport.from(
            mapOf(HwTest.SCREEN to true, HwTest.SENSORS to true, HwTest.CHARGING to false),
            model = "Xiaomi 2312DRA50G", androidVersion = "14", batteryHealthPercent = 91, date = "2026-10-23",
        )
        assertEquals(HwTest.entries.size, report.results.size)
        assertEquals(2, report.passed)
        assertEquals(1, report.failed)
        assertEquals(HwTest.entries.size - 3, report.notRun)
        assertNull(report.results.first { it.first == HwTest.MIC }.second)
    }

    @Test
    fun fastChargeThreshold() {
        assertFalse(BatteryMath.isFastCharge(null))
        assertFalse(BatteryMath.isFastCharge(10.0))
        assertFalse(BatteryMath.isFastCharge(15.0))
        assertTrue(BatteryMath.isFastCharge(18.2))
    }
}
