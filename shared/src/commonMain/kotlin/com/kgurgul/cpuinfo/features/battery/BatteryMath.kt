package com.kgurgul.cpuinfo.features.battery

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Unit/sign normalization and estimates for the battery screen. Capacity ideas (charge counter ÷
 * level, plausibility window 500–30000 mAh, µAh/mAh detection) adapted from aBattery by abanana84
 * (Apache-2.0, https://github.com/abanana84/abattery).
 */
object BatteryMath {

    private val PLAUSIBLE_MAH = 500..30_000

    /** Minimum level for a counter ÷ level estimate; below this rounding error dominates. */
    const val MIN_LEVEL_FOR_ESTIMATE = 20

    /**
     * CURRENT_NOW should be µA, but some OEMs report mA. Values of |x| >= 10000 are treated as µA.
     * Sign conventions also differ by OEM, so direction comes from the charging status: result is
     * positive while charging, negative while discharging, magnitude otherwise. 0/MIN_VALUE → null.
     */
    fun currentMa(raw: Int, isCharging: Boolean, isDischarging: Boolean): Int? {
        if (raw == 0 || raw == Int.MIN_VALUE) return null
        val magnitude = abs(raw).let { if (it >= 10_000) (it / 1000.0).roundToInt() else it }
        if (magnitude == 0) return null
        return when {
            isCharging -> magnitude
            isDischarging -> -magnitude
            else -> magnitude
        }
    }

    /** CHARGE_COUNTER should be µAh; some devices report mAh. Returns mAh or null. */
    fun chargeCounterMah(raw: Long): Int? {
        if (raw <= 0L || raw == Long.MIN_VALUE) return null
        return if (raw > 100_000L) (raw / 1000L).toInt() else raw.toInt()
    }

    /** Capacity value read from sysfs/profile: µAh if large, else mAh; null if implausible. */
    fun capacityMah(raw: Long): Int? {
        val mah = if (raw > 100_000L) (raw / 1000L).toInt() else raw.toInt()
        return mah.takeIf { it in PLAUSIBLE_MAH }
    }

    /** Estimated full-charge capacity = remaining charge ÷ level fraction. */
    fun estimateFullMah(chargeCounterMah: Int?, levelPercent: Int?): Int? {
        if (chargeCounterMah == null || levelPercent == null) return null
        if (levelPercent !in MIN_LEVEL_FOR_ESTIMATE..100) return null
        return (chargeCounterMah * 100.0 / levelPercent).roundToInt().takeIf { it in PLAUSIBLE_MAH }
    }

    /** Health % = estimated full ÷ design; null when either is missing or result is implausible. */
    fun healthPercent(fullMah: Int?, designMah: Int?): Int? {
        if (fullMah == null || designMah == null || designMah <= 0) return null
        return (fullMah * 100.0 / designMah).roundToInt().takeIf { it in 30..130 }
    }

    /** Power in watts from mA and volts (magnitude). */
    fun powerW(currentMa: Int?, voltageV: Float?): Double? {
        if (currentMa == null || voltageV == null || voltageV <= 0f) return null
        return abs(currentMa) / 1000.0 * voltageV
    }
}
