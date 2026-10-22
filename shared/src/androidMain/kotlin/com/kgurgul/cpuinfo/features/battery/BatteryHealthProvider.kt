package com.kgurgul.cpuinfo.features.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.kgurgul.cpuinfo.shared.Res
import com.kgurgul.cpuinfo.shared.battery_charging
import com.kgurgul.cpuinfo.shared.battery_cold
import com.kgurgul.cpuinfo.shared.battery_dead
import com.kgurgul.cpuinfo.shared.battery_discharging
import com.kgurgul.cpuinfo.shared.battery_dock
import com.kgurgul.cpuinfo.shared.battery_full
import com.kgurgul.cpuinfo.shared.battery_good
import com.kgurgul.cpuinfo.shared.battery_not_charging
import com.kgurgul.cpuinfo.shared.battery_overheat
import com.kgurgul.cpuinfo.shared.battery_overvoltage
import com.kgurgul.cpuinfo.shared.battery_unspecified_failure
import com.kgurgul.cpuinfo.shared.battery_wireless
import com.kgurgul.cpuinfo.shared.hardware_ac
import com.kgurgul.cpuinfo.shared.hardware_usb
import java.io.File

/** Reads battery data without root; sysfs/PowerProfile approach adapted from aBattery (Apache-2.0). */
class BatteryHealthProvider(private val context: Context) : IBatteryHealthProvider {

    private val designMah: Int? by lazy { readDesignMah() }

    override fun read(): BatterySnapshot {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val bm = context.getSystemService(BatteryManager::class.java)
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
        val isDischarging = status == BatteryManager.BATTERY_STATUS_DISCHARGING
        val cycles =
            if (Build.VERSION.SDK_INT >= 34 && intent?.hasExtra(BatteryManager.EXTRA_CYCLE_COUNT) == true) {
                intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1).takeIf { it >= 0 }
            } else null
        return BatterySnapshot(
            levelPercent = if (level >= 0 && scale > 0) level * 100 / scale else null,
            health = healthRes(intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1),
            state =
                when (status) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> Res.string.battery_charging
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> Res.string.battery_discharging
                    BatteryManager.BATTERY_STATUS_FULL -> Res.string.battery_full
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> Res.string.battery_not_charging
                    else -> null
                },
            chargers =
                buildList {
                    if (plugged and BatteryManager.BATTERY_PLUGGED_AC != 0) add(Res.string.hardware_ac)
                    if (plugged and BatteryManager.BATTERY_PLUGGED_USB != 0) add(Res.string.hardware_usb)
                    if (plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0) add(Res.string.battery_wireless)
                    if (Build.VERSION.SDK_INT >= 33 && plugged and BatteryManager.BATTERY_PLUGGED_DOCK != 0) {
                        add(Res.string.battery_dock)
                    }
                },
            technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf { it.isNotBlank() },
            voltageV = voltageMv.takeIf { it > 0 }?.let { it / 1000f },
            temperatureC = tempTenths?.takeIf { it != Int.MIN_VALUE }?.let { it / 10f },
            currentMa =
                bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)?.let {
                    BatteryMath.currentMa(it, isCharging, isDischarging)
                },
            designMah = designMah,
            chargeCounterMah =
                bm?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)?.let {
                    BatteryMath.chargeCounterMah(it)
                },
            cycleCount = cycles,
        )
    }

    private fun healthRes(health: Int) =
        when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> Res.string.battery_good
            BatteryManager.BATTERY_HEALTH_COLD -> Res.string.battery_cold
            BatteryManager.BATTERY_HEALTH_DEAD -> Res.string.battery_dead
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> Res.string.battery_overheat
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> Res.string.battery_overvoltage
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> Res.string.battery_unspecified_failure
            else -> null
        }

    private fun readDesignMah(): Int? {
        for (dir in listOf("battery", "Battery", "bms", "BMS")) {
            for (name in listOf("charge_full_design", "charge_full_design_uah")) {
                try {
                    val raw = File("/sys/class/power_supply/$dir/$name").readText().trim().toLongOrNull()
                    raw?.let { BatteryMath.capacityMah(it) }?.let { return it }
                } catch (_: Exception) {}
            }
        }
        // Nominal capacity from the OEM power profile (hidden API, may be unavailable).
        return try {
            val cls = Class.forName("com.android.internal.os.PowerProfile")
            val inst = cls.getConstructor(Context::class.java).newInstance(context)
            val cap = cls.getMethod("getBatteryCapacity").invoke(inst) as? Double
            cap?.let { BatteryMath.capacityMah(it.toLong()) }
        } catch (_: Throwable) {
            null
        }
    }
}
