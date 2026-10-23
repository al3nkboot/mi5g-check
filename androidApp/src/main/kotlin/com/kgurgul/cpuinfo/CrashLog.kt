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

    fun delete(context: Context) { file(context).delete() }
}
