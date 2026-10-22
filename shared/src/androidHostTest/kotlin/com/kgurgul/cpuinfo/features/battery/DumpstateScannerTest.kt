package com.kgurgul.cpuinfo.features.battery

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DumpstateScannerTest {
    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> entries.forEach { (n, b) -> z.putNextEntry(ZipEntry(n)); z.write(b); z.closeEntry() } }
        return out.toByteArray()
    }

    private val noise = buildString { repeat(20_000) { append("01-01 00:00:00 I/x: noise line $it\n") } }

    @Test
    fun plainText() = assertEquals(90, DumpstateScanner.scan((noise + "mSavedBatteryAsoc: 90\n").byteInputStream()))

    @Test
    fun zipWithNestedZip() {
        val inner = zip("dumpstate_SM-S918B.txt" to (noise + "  mSavedBatteryBsoh: 94\n").toByteArray())
        val outer = zip("readme.txt" to "hello".toByteArray(), "dumpState_2026.zip" to inner)
        assertEquals(94, DumpstateScanner.scan(outer.inputStream()))
    }

    @Test
    fun missing() {
        assertNull(DumpstateScanner.scan(noise.byteInputStream()))
        assertNull(DumpstateScanner.scan(zip("a.txt" to noise.toByteArray()).inputStream()))
    }
}
