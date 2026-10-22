package com.kgurgul.cpuinfo.features.battery

/** One stored battery reading. Nullable fields are unavailable on the device. */
data class BatterySample(
    val timeMs: Long,
    val level: Int?,
    val healthPercent: Int?,
    val cycles: Int?,
    val temperatureC: Float?,
)

enum class HistoryRange(val days: Int?) { WEEK(7), MONTH(30), ALL(null) }

/** Pure history logic; storage is a tiny CSV file (one sample per line) on Android. */
object BatteryHistory {

    const val DAY_MS = 24L * 60 * 60 * 1000
    const val MAX_AGE_MS = 366 * DAY_MS
    /** A new sample on app open is skipped if the last one is newer than this. */
    const val MIN_GAP_MS = 30L * 60 * 1000

    fun encode(s: BatterySample): String =
        listOf(s.timeMs, s.level, s.healthPercent, s.cycles, s.temperatureC).joinToString(",") { it?.toString() ?: "" }

    fun decode(line: String): BatterySample? {
        val p = line.trim().split(",")
        if (p.size < 5) return null
        val t = p[0].toLongOrNull() ?: return null
        return BatterySample(t, p[1].toIntOrNull(), p[2].toIntOrNull(), p[3].toIntOrNull(), p[4].toFloatOrNull())
    }

    /** Drops samples older than ~1 year and sorts by time. */
    fun prune(samples: List<BatterySample>, nowMs: Long): List<BatterySample> =
        samples.filter { nowMs - it.timeMs <= MAX_AGE_MS }.sortedBy { it.timeMs }

    fun inRange(samples: List<BatterySample>, range: HistoryRange, nowMs: Long): List<BatterySample> =
        range.days?.let { d -> samples.filter { nowMs - it.timeMs <= d * DAY_MS } } ?: samples

    fun shouldRecord(last: BatterySample?, nowMs: Long): Boolean = last == null || nowMs - last.timeMs >= MIN_GAP_MS
}

/** Temperature alert rule: above threshold and at most once per hour. */
object TemperatureAlert {
    const val DEFAULT_THRESHOLD_C = 42
    const val MIN_INTERVAL_MS = 60L * 60 * 1000

    fun shouldNotify(enabled: Boolean, tempC: Float?, thresholdC: Int, lastAlertMs: Long, nowMs: Long): Boolean =
        enabled && tempC != null && tempC > thresholdC && nowMs - lastAlertMs >= MIN_INTERVAL_MS
}

/**
 * Finds the battery health % that Samsung writes into dumpstate (*#9900#), e.g.
 * `mSavedBatteryAsoc: 92`. Works line by line so huge files can be streamed.
 */
object DumpstateParser {

    /** Keys in order of preference (ASOC = capacity state of health). */
    val KEYS = listOf("mSavedBatteryAsoc", "battery_asoc", "mSavedBatteryBsoh", "BSOH", "asoc")

    private val regex =
        Regex("""(?i)(?<![A-Za-z0-9_])(mSavedBatteryAsoc|battery_asoc|mSavedBatteryBsoh|bsoh|asoc)(?![A-Za-z0-9_])\]?\s*[:=]\s*\[?\s*(\d{1,3})(?!\d)""")

    /** Returns (key index, value) for the best match on this line, or null. */
    fun matchLine(line: String): Pair<Int, Int>? =
        regex.findAll(line).mapNotNull { m ->
            val idx = KEYS.indexOfFirst { it.equals(m.groupValues[1], ignoreCase = true) }
            val v = m.groupValues[2].toInt()
            if (idx >= 0 && v in 1..120) idx to v else null
        }.minByOrNull { it.first }

    /** Best value across all lines (preferred key wins; first occurrence of that key), or null. */
    fun parse(lines: Sequence<String>): Int? {
        var best: Pair<Int, Int>? = null
        for (line in lines) {
            val m = matchLine(line) ?: continue
            if (best == null || m.first < best.first) best = m
            if (best.first == 0) break
        }
        return best?.second
    }
}
