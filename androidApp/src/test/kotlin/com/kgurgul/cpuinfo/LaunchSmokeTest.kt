package com.kgurgul.cpuinfo

import android.os.Looper
import androidx.test.core.app.ActivityScenario
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Starts CpuInfoApp + HostActivity (the launch path that crashed) on several API levels. */
@RunWith(RobolectricTestRunner::class)
@Config(application = SmokeTestApp::class, sdk = [24, 33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchSmokeTest {

    @After fun tearDown() = stopKoin()

    // Full Compose launch is slow/memory-hungry under Robolectric on this box: one SDK only.
    @Test
    @Config(sdk = [28])
    fun launches() {
        ActivityScenario.launch(HostActivity::class.java).use { s ->
            repeat(20) { shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200)) }
            s.onActivity { check(!it.isFinishing) }
        }
    }

    @Test
    fun infoProvidersDoNotThrow() = kotlinx.coroutines.runBlocking {
        com.kgurgul.cpuinfo.data.provider.ScreenDataProvider().getData()
        com.kgurgul.cpuinfo.data.provider.HardwareDataProvider().getData()
        com.kgurgul.cpuinfo.features.perf.detectStorageType()
        Unit
    }
}
