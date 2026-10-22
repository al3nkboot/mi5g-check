package com.kgurgul.cpuinfo.features.battery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BatteryMathTest {
    @Test
    fun currentMicroAmpsConvertedAndSignFromStatus() {
        assertEquals(1500, BatteryMath.currentMa(1_500_000, isCharging = true, isDischarging = false))
        assertEquals(1500, BatteryMath.currentMa(-1_500_000, isCharging = true, isDischarging = false))
        assertEquals(-420, BatteryMath.currentMa(420_000, isCharging = false, isDischarging = true))
    }

    @Test
    fun currentMilliAmpsKept() {
        assertEquals(-850, BatteryMath.currentMa(850, isCharging = false, isDischarging = true))
        assertEquals(2300, BatteryMath.currentMa(-2300, isCharging = true, isDischarging = false))
    }

    @Test
    fun currentUnavailable() {
        assertNull(BatteryMath.currentMa(0, true, false))
        assertNull(BatteryMath.currentMa(Int.MIN_VALUE, true, false))
    }

    @Test
    fun chargeCounterUnits() {
        assertEquals(4000, BatteryMath.chargeCounterMah(4_000_000L))
        assertEquals(4000, BatteryMath.chargeCounterMah(4000L))
        assertNull(BatteryMath.chargeCounterMah(0L))
        assertNull(BatteryMath.chargeCounterMah(Long.MIN_VALUE))
    }

    @Test
    fun capacityUnitsAndPlausibility() {
        assertEquals(5000, BatteryMath.capacityMah(5_000_000L))
        assertEquals(5000, BatteryMath.capacityMah(5000L))
        assertNull(BatteryMath.capacityMah(100L))
    }

    @Test
    fun fullCapacityEstimate() {
        assertEquals(4600, BatteryMath.estimateFullMah(3680, 80))
        assertNull(BatteryMath.estimateFullMah(500, 10))
        assertNull(BatteryMath.estimateFullMah(null, 80))
        assertNull(BatteryMath.estimateFullMah(3680, null))
    }

    @Test
    fun healthPercent() {
        assertEquals(92, BatteryMath.healthPercent(4600, 5000))
        assertNull(BatteryMath.healthPercent(4600, null))
        assertNull(BatteryMath.healthPercent(1000, 5000))
    }

    @Test
    fun power() {
        assertEquals(6.0, BatteryMath.powerW(-1500, 4.0f)!!, 1e-6)
        assertNull(BatteryMath.powerW(null, 4.0f))
        assertNull(BatteryMath.powerW(1500, null))
    }
}
