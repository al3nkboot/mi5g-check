package com.kgurgul.cpuinfo.features.tests

/** Tracks which cells of a [cols] x [rows] grid were touched. */
class TouchGrid(val cols: Int, val rows: Int) {
    private val cells = BooleanArray(cols * rows)

    var coveredCount = 0
        private set

    val isComplete: Boolean
        get() = coveredCount == cells.size

    fun isCovered(col: Int, row: Int): Boolean = cells[row * cols + col]

    /** Marks the cell under ([x], [y]) in a [width] x [height] area. Returns true if newly covered. */
    fun touch(x: Float, y: Float, width: Float, height: Float): Boolean {
        if (width <= 0f || height <= 0f || x < 0f || y < 0f || x >= width || y >= height) return false
        val col = (x / width * cols).toInt().coerceIn(0, cols - 1)
        val row = (y / height * rows).toInt().coerceIn(0, rows - 1)
        val i = row * cols + col
        if (cells[i]) return false
        cells[i] = true
        coveredCount++
        return true
    }
}
