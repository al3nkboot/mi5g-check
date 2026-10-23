package com.kgurgul.cpuinfo.features.perf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class PerfMathTest {
    @Test fun megapixels() {
        assertEquals(12.5, PerfMath.megapixels(4080, 3060))
        assertEquals(50.1, PerfMath.megapixels(8160, 6144))
        assertEquals(199.8, PerfMath.megapixels(16320, 12240))
        assertEquals(8.3, PerfMath.megapixels(3840, 2160))
        assertEquals(0.0, PerfMath.megapixels(0, 100))
    }

    @Test fun refreshRates() {
        assertEquals("60 / 90 / 120 Hz", PerfMath.formatRefreshRates(listOf(120.00001f, 60f, 90f, 60.0f, 59.999f)))
        assertEquals("48 / 59.9 / 60 Hz", PerfMath.formatRefreshRates(listOf(60f, 59.94f, 48f)))
        assertNull(PerfMath.formatRefreshRates(emptyList()))
        assertNull(PerfMath.formatRefreshRates(listOf(0f)))
    }

    @Test fun diagonal() {
        assertEquals(6.7, PerfMath.diagonalInches(1080, 2400, 394f, 394f))
        assertNull(PerfMath.diagonalInches(1080, 2400, 0f, 0f))
    }

    @Test fun mbPerSec() {
        assertEquals(256.0, PerfMath.mbPerSec(256 * PerfMath.MB, 1_000_000_000))
        assertEquals(512.0, PerfMath.mbPerSec(256 * PerfMath.MB, 500_000_000))
        assertEquals(85.3, PerfMath.mbPerSec(256 * PerfMath.MB, 3_000_000_000))
        assertNull(PerfMath.mbPerSec(100, 0))
    }

    @Test fun storageTestSize() {
        assertEquals(256 * PerfMath.MB, PerfMath.storageTestBytes(10_000 * PerfMath.MB))
        assertEquals(100 * PerfMath.MB, PerfMath.storageTestBytes(400 * PerfMath.MB + 3))
        assertNull(PerfMath.storageTestBytes(40 * PerfMath.MB))
    }

    @Test fun videoLabel() {
        assertEquals("8K", PerfMath.videoLabel(7680, 4320))
        assertEquals("4K", PerfMath.videoLabel(2160, 3840))
        assertEquals("Full HD", PerfMath.videoLabel(1920, 1080))
    }

    @Test fun benchIsDeterministic() {
        assertEquals(CpuBench.unit(7), CpuBench.unit(7))
        assertEquals(CpuBench.unit(0), CpuBench.unit(0))
        assertNotEquals(CpuBench.unit(1), CpuBench.unit(2))
    }

    @Test fun benchScore() {
        assertEquals(1000, CpuBench.score(100, 1_000_000_000)) // 100 units/s x 10
        assertEquals(2000, CpuBench.score(100, 500_000_000))
        assertEquals(0, CpuBench.score(5, 0))
    }

    @Test
    fun oneDecimalHasNoFloatNoise() {
        kotlin.test.assertEquals("881.2", PerfMath.oneDecimal(881.2f.toDouble()))
        kotlin.test.assertEquals("2421.0", PerfMath.oneDecimal(2421.0))
        kotlin.test.assertEquals("0.1", PerfMath.oneDecimal(0.05))
    }
}
