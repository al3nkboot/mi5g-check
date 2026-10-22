package com.kgurgul.cpuinfo.features.tests

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TouchGridTest {

    @Test
    fun touchMarksCellOnce() {
        val grid = TouchGrid(cols = 2, rows = 2)
        assertTrue(grid.touch(10f, 10f, 100f, 100f))
        assertFalse(grid.touch(20f, 20f, 100f, 100f))
        assertTrue(grid.isCovered(0, 0))
        assertFalse(grid.isCovered(1, 0))
        assertEquals(1, grid.coveredCount)
    }

    @Test
    fun outsideTouchesIgnored() {
        val grid = TouchGrid(cols = 2, rows = 2)
        assertFalse(grid.touch(-1f, 10f, 100f, 100f))
        assertFalse(grid.touch(10f, 100f, 100f, 100f))
        assertFalse(grid.touch(10f, 10f, 0f, 100f))
        assertEquals(0, grid.coveredCount)
    }

    @Test
    fun completeWhenAllCellsCovered() {
        val grid = TouchGrid(cols = 3, rows = 2)
        for (r in 0 until 2) for (c in 0 until 3) {
            assertFalse(grid.isComplete)
            grid.touch(c * 10f + 5f, r * 10f + 5f, 30f, 20f)
        }
        assertTrue(grid.isComplete)
        assertTrue(grid.isCovered(2, 1))
    }
}
