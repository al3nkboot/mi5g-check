/*
 * Copyright KG Soft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kgurgul.cpuinfo

import android.app.Application
import com.kgurgul.cpuinfo.appinitializers.AppInitializerComponent
import com.kgurgul.cpuinfo.di.androidModule
import com.kgurgul.cpuinfo.di.sharedModule
import com.kgurgul.cpuinfo.utils.AppLanguage
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CpuInfoApp : Application(), androidx.work.Configuration.Provider {

    // WorkManager auto-init is removed from the manifest; it initializes lazily on first use.
    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder().build()

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        // Earliest point in the process: catch crashes from providers/static init too.
        runCatching { CrashLog.install(base) }
    }

    override fun onCreate() {
        super.onCreate()
        // The crash viewer runs in its own process: skip all heavy init there.
        if (isCrashProcess()) return
        try {
            startKoin {
                androidContext(this@CpuInfoApp)
                modules(androidModule, sharedModule)
            }
            AppInitializerComponent().init(this)
            AppLanguage.init(this)
        } catch (t: Throwable) {
            // Keep the process alive so the crash screen can show the cause on launch.
            runCatching { CrashLog.write(CrashLog.file(this), CrashLog.format(this, "Application.onCreate", t)) }
        }
        runCatching { com.kgurgul.cpuinfo.features.battery.BatteryStore.schedule(this) }
    }

    private fun isCrashProcess(): Boolean = runCatching {
        val name = if (android.os.Build.VERSION.SDK_INT >= 28) getProcessName()
        else java.io.File("/proc/self/cmdline").readText().trim { it <= ' ' }
        name.endsWith(":crash")
    }.getOrDefault(false)
}
