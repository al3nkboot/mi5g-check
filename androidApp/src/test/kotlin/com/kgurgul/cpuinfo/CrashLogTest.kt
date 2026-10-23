package com.kgurgul.cpuinfo

import androidx.test.core.app.ApplicationProvider
import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = SmokeTestApp::class, sdk = [34])
class CrashLogTest {
    @Test
    fun writesAndReadsCrashFile() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        CrashLog.delete(app)
        assertNull(CrashLog.read(app))
        val text = CrashLog.format(app, "main", IllegalStateException("boom"))
        CrashLog.write(CrashLog.file(app), text)
        val read = CrashLog.read(app)!!
        assertEquals(text, read)
        assertTrue(read.contains("IllegalStateException: boom"))
        assertTrue(read.contains("SDK_INT 34"))
        CrashLog.delete(app)
        assertNull(CrashLog.read(app))
        stopKoin()
    }
}
