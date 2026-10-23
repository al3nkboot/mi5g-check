package com.kgurgul.cpuinfo

import android.app.Application
import com.kgurgul.cpuinfo.di.androidModule
import com.kgurgul.cpuinfo.di.sharedModule
import com.kgurgul.cpuinfo.features.battery.BatteryStore
import com.kgurgul.cpuinfo.utils.AppLanguage
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/** Same as [CpuInfoApp] minus the native library load (no .so on the JVM host). */
class SmokeTestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SmokeTestApp)
            modules(androidModule, sharedModule)
        }
        AppLanguage.init(this)
        BatteryStore.schedule(this)
    }
}
