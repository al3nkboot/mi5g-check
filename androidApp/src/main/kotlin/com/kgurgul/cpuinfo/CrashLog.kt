package com.kgurgul.cpuinfo

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/** Writes the last uncaught exception to filesDir/last_crash.txt (no network, local only). */
object CrashLog {
    const val FILE_NAME = "last_crash.txt"

    fun file(context: Context) = File(context.filesDir, FILE_NAME)

    fun install(context: Context) {
        val app = context.applicationContext ?: context
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(file(app), format(app, thread.name, error)) }
            if (previous != null) previous.uncaughtException(thread, error)
            else { android.os.Process.killProcess(android.os.Process.myPid()); System.exit(10) }
        }
    }

    fun format(context: Context, threadName: String, error: Throwable): String {
        val version = runCatching {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else @Suppress("DEPRECATION") pi.versionCode.toLong()
            "${pi.versionName} ($code)"
        }.getOrDefault("?")
        val sw = StringWriter()
        error.printStackTrace(PrintWriter(sw))
        return buildString {
            appendLine("App: ${context.packageName} $version")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK_INT ${Build.VERSION.SDK_INT})")
            appendLine("Time: ${java.util.Date()}")
            appendLine("Thread: $threadName")
            appendLine()
            append(sw.toString())
        }
    }

    /** Synchronous write (we are about to die). */
    fun write(file: File, text: String) {
        file.parentFile?.mkdirs()
        java.io.FileOutputStream(file).use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
    }

    fun read(context: Context): String? =
        file(context).takeIf { it.exists() }?.let { runCatching { it.readText() }.getOrNull() }

    fun delete(context: Context) {
        file(context).delete()
        markExitSeen(context)
    }

    private fun prefs(context: Context) = context.getSharedPreferences("crash_log", Context.MODE_PRIVATE)

    private fun markExitSeen(context: Context) {
        val ts = lastBadExit(context)?.timestamp ?: return
        prefs(context).edit().putLong("seen_exit", ts).commit()
    }

    /** API 30+: last process exit if it was a crash/native crash/ANR/init failure. */
    private fun lastBadExit(context: Context): android.app.ApplicationExitInfo? {
        if (Build.VERSION.SDK_INT < 30) return null
        return runCatching {
            val am = context.getSystemService(android.app.ActivityManager::class.java)
            am.getHistoricalProcessExitReasons(context.packageName, 0, 1).firstOrNull()?.takeIf {
                it.reason == android.app.ApplicationExitInfo.REASON_CRASH ||
                    it.reason == android.app.ApplicationExitInfo.REASON_CRASH_NATIVE ||
                    it.reason == android.app.ApplicationExitInfo.REASON_ANR ||
                    it.reason == android.app.ApplicationExitInfo.REASON_INITIALIZATION_FAILURE
            }
        }.getOrNull()
    }

    /** Text for a system-recorded bad exit not shown yet, or null. */
    fun pendingExitReport(context: Context): String? {
        if (Build.VERSION.SDK_INT < 30) return null
        return runCatching {
            val e = lastBadExit(context) ?: return null
            if (prefs(context).getLong("seen_exit", 0L) == e.timestamp) return null
            val reason = when (e.reason) {
                android.app.ApplicationExitInfo.REASON_CRASH -> "CRASH"
                android.app.ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
                android.app.ApplicationExitInfo.REASON_ANR -> "ANR"
                else -> "INITIALIZATION_FAILURE"
            }
            val trace = runCatching {
                e.traceInputStream?.use { String(it.readBytes().take(200_000).toByteArray(), Charsets.UTF_8) }
            }.getOrNull()
            buildString {
                appendLine("System exit record (ApplicationExitInfo)")
                appendLine("Reason: $reason  status=${e.status}  process=${e.processName}")
                appendLine("Time: ${java.util.Date(e.timestamp)}")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (SDK_INT ${Build.VERSION.SDK_INT})")
                appendLine("Description: ${e.description}")
                if (!trace.isNullOrBlank()) { appendLine(); append(trace) }
            }
        }.getOrNull()
    }

    /** Everything to show on the crash screen, or null if nothing pending. */
    fun pendingReport(context: Context): String? {
        val parts = listOfNotNull(read(context), pendingExitReport(context))
        return if (parts.isEmpty()) null else parts.joinToString("\n\n----------\n\n")
    }
}
