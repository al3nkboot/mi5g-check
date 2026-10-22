package com.kgurgul.cpuinfo.features.battery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BatteryHistoryTest {
    private val day = BatteryHistory.DAY_MS

    @Test
    fun encodeDecodeRoundTrip() {
        val s = BatterySample(123L, 80, 92, 210, 31.5f)
        assertEquals(s, BatteryHistory.decode(BatteryHistory.encode(s)))
        val n = BatterySample(5L, null, null, null, null)
        assertEquals(n, BatteryHistory.decode(BatteryHistory.encode(n)))
        assertNull(BatteryHistory.decode("garbage"))
        assertNull(BatteryHistory.decode("x,1,2,3,4"))
    }

    @Test
    fun pruneKeepsAboutOneYearSorted() {
        val now = 1000 * day
        val out = BatteryHistory.prune(
            listOf(BatterySample(now - day, 1, null, null, null), BatterySample(now - 400 * day, 2, null, null, null),
                BatterySample(now - 300 * day, 3, null, null, null)), now)
        assertEquals(listOf(3, 1), out.map { it.level })
    }

    @Test
    fun ranges() {
        val now = 100 * day
        val all = listOf(2L, 10L, 40L).map { BatterySample(now - it * day, it.toInt(), null, null, null) }
        assertEquals(1, BatteryHistory.inRange(all, HistoryRange.WEEK, now).size)
        assertEquals(2, BatteryHistory.inRange(all, HistoryRange.MONTH, now).size)
        assertEquals(3, BatteryHistory.inRange(all, HistoryRange.ALL, now).size)
    }

    @Test
    fun recordGap() {
        assertTrue(BatteryHistory.shouldRecord(null, 0))
        assertFalse(BatteryHistory.shouldRecord(BatterySample(0, 1, null, null, null), 60_000))
        assertTrue(BatteryHistory.shouldRecord(BatterySample(0, 1, null, null, null), 31 * 60_000L))
    }

    @Test
    fun temperatureAlert() {
        val hour = 3_600_000L
        assertFalse(TemperatureAlert.shouldNotify(false, 50f, 42, 0, 10 * hour))
        assertFalse(TemperatureAlert.shouldNotify(true, 41.9f, 42, 0, 10 * hour))
        assertFalse(TemperatureAlert.shouldNotify(true, 42f, 42, 0, 10 * hour))
        assertFalse(TemperatureAlert.shouldNotify(true, null, 42, 0, 10 * hour))
        assertTrue(TemperatureAlert.shouldNotify(true, 42.1f, 42, 0, 10 * hour))
        assertFalse(TemperatureAlert.shouldNotify(true, 45f, 42, 10 * hour, 10 * hour + hour / 2))
        assertTrue(TemperatureAlert.shouldNotify(true, 45f, 42, 10 * hour, 11 * hour))
    }
}

class DumpstateParserTest {
    private fun parse(vararg lines: String) = DumpstateParser.parse(lines.asSequence())

    @Test
    fun savedAsoc() = assertEquals(92, parse("noise", "  mSavedBatteryAsoc: 92", "more noise"))

    @Test
    fun equalsAndBrackets() {
        assertEquals(88, parse("battery_asoc=88"))
        assertEquals(87, parse("[BSOH] : [87]"))
        assertEquals(86, parse("asoc: 86"))
    }

    @Test
    fun bsohWhenNoAsoc() = assertEquals(95, parse("x", "mSavedBatteryBsoh: 95", "y"))

    @Test
    fun asocPreferredOverBsoh() =
        assertEquals(91, parse("mSavedBatteryBsoh: 97", "BSOH: 96", "mSavedBatteryAsoc: 91", "mSavedBatteryAsoc: 50"))

    @Test
    fun ignoresNoiseAndLookalikes() {
        assertNull(parse("asocial: 5", "mSavedBatteryAsocX: 90", "basoc: 70", "asoc: abc", "asoc: 1234", "asoc: 0"))
        assertNull(parse())
        assertNull(parse("01-01 12:00:00.000 I/battery: level 80 temp 300", "BatteryInfoBackUp something"))
    }

    @Test
    fun caseInsensitiveAndInlineNoise() =
        assertEquals(93, parse("10-20 09:11:12.345  1000  1234 I BatteryService: MSAVEDBATTERYASOC:93, mSavedBatteryBsoh:99"))
}
