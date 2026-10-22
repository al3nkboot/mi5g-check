package com.kgurgul.cpuinfo.features.perf

import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

/** Pure helpers for display/camera/storage/benchmark values (unit tested). */
object PerfMath {
    /** Megapixels with one decimal, e.g. 4080x3060 -> 12.5. */
    fun megapixels(width: Int, height: Int): Double =
        (width.toLong() * height / 100_000.0).roundToLong() / 10.0

    /** "60 / 90 / 120 Hz" from raw mode rates (deduplicated to 0.1 Hz, ascending). */
    fun formatRefreshRates(rates: List<Float>): String? {
        val r = rates.filter { it > 0f }.map { (it * 10).roundToInt() }.distinct().sorted()
        if (r.isEmpty()) return null
        return r.joinToString(" / ") { if (it % 10 == 0) "${it / 10}" else "${it / 10}.${it % 10}" } + " Hz"
    }

    /** Diagonal in inches with one decimal, or null when dpi values are not plausible. */
    fun diagonalInches(widthPx: Int, heightPx: Int, xdpi: Float, ydpi: Float): Double? {
        if (widthPx <= 0 || heightPx <= 0 || xdpi < 50f || ydpi < 50f) return null
        val w = widthPx / xdpi
        val h = heightPx / ydpi
        val d = sqrt((w * w + h * h).toDouble())
        return if (d in 1.0..40.0) (d * 10).roundToLong() / 10.0 else null
    }

    /** MB/s (1 MB = 1,048,576 bytes) with one decimal; null when time is not measurable. */
    fun mbPerSec(bytes: Long, nanos: Long): Double? {
        if (bytes <= 0 || nanos <= 0) return null
        return (bytes / 1_048_576.0 / (nanos / 1e9) * 10).roundToLong() / 10.0
    }

    const val MB = 1_048_576L

    /** Test file size: 256 MB, or a quarter of free space (whole MB, >= 16 MB); null if too little space. */
    fun storageTestBytes(freeBytes: Long): Long? {
        val size = minOf(256 * MB, freeBytes / 4 / MB * MB)
        return if (size >= 16 * MB) size else null
    }

    /** "4K" / "8K" / "" label for a video size. */
    fun videoLabel(width: Int, height: Int): String {
        val long = maxOf(width, height)
        return when {
            long >= 7680 -> "8K"
            long >= 3840 -> "4K"
            long >= 1920 -> "Full HD"
            else -> ""
        }
    }
}

data class BenchResult(val single: Int, val multi: Int, val threads: Int, val seconds: Double)

data class StorageSpeed(val writeMbS: Double, val readMbS: Double, val sizeMb: Long)

/**
 * The app's own light CPU benchmark. One "unit" is a fixed, deterministic mix of integer
 * math, float math, a sha-like hash and a sort. Score = units per second x [SCALE].
 */
object CpuBench {
    const val SCALE = 10.0

    /** Runs one unit of work and returns a checksum (identical for the same seed on every device). */
    fun unit(seed: Int): Long {
        // Integer: LCG + mixing
        var x = seed.toLong() * 2654435761L + 1
        var acc = 0L
        for (i in 0 until 20_000) {
            x = x * 6364136223846793005L + 1442695040888963407L
            acc += (x ushr 33) xor (acc shl 3)
        }
        // Float
        var f = 1.0 + (seed and 7)
        for (i in 1..20_000) f = f * 1.000001 + 1.0 / i - f / (i + 1)
        acc = acc xor (f * 1e6).toLong()
        // Sha-like compression rounds over a 64-word block
        val w = IntArray(64) { (x ushr (it % 32)).toInt() + it }
        var a = 0x6a09e667; var b = -0x4498517b; var c = 0x3c6ef372; var d = -0x5ab00ac6
        repeat(40) { r ->
            for (i in 0 until 64) {
                val t = (a.rotateLeft(5) + (b and c or (b.inv() and d)) + w[i] + r)
                d = c; c = b.rotateLeft(30); b = a; a = t
            }
            w[r % 64] = a xor d
        }
        acc = acc xor (a.toLong() shl 32) xor (c.toLong() and 0xffffffffL)
        // Sort
        val arr = IntArray(3_000) { i -> ((i * 1103515245 + seed * 12345) ushr 7) }
        arr.sort()
        acc += arr[arr.size / 2].toLong() + arr[0]
        return acc
    }

    /** Score for [units] completed in [nanos]. */
    fun score(units: Long, nanos: Long): Int =
        if (nanos <= 0) 0 else (units * 1e9 / nanos * SCALE).roundToInt()
}

private fun Int.rotateLeft(n: Int): Int = (this shl n) or (this ushr (32 - n))
