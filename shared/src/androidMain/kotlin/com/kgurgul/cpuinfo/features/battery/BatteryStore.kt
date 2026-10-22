package com.kgurgul.cpuinfo.features.battery

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kgurgul.cpuinfo.shared.Res
import com.kgurgul.cpuinfo.shared.battery_temp_channel
import com.kgurgul.cpuinfo.shared.battery_temp_notif_text
import com.kgurgul.cpuinfo.shared.battery_temp_notif_title
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import org.jetbrains.compose.resources.getString

/** Local-only battery history (CSV in app files) and battery settings (SharedPreferences). */
class BatteryStore(context: Context) {
    private val app = context.applicationContext
    private val file = File(app.filesDir, "battery_history.csv")
    private val prefs = app.getSharedPreferences("battery", Context.MODE_PRIVATE)

    @Synchronized
    fun history(): List<BatterySample> =
        if (file.exists()) file.readLines().mapNotNull(BatteryHistory::decode).sortedBy { it.timeMs } else emptyList()

    /** Records a sample (skips if one was saved in the last 30 min) and trims to ~1 year. */
    @Synchronized
    fun record(s: BatterySnapshot, nowMs: Long = System.currentTimeMillis()) {
        val all = history()
        if (!BatteryHistory.shouldRecord(all.lastOrNull(), nowMs)) return
        val health = BatteryMath.healthPercent(BatteryMath.estimateFullMah(s.chargeCounterMah, s.levelPercent), s.designMah)
        val kept = BatteryHistory.prune(all + BatterySample(nowMs, s.levelPercent, health, s.cycleCount, s.temperatureC), nowMs)
        file.writeText(kept.joinToString("") { BatteryHistory.encode(it) + "\n" })
    }

    val systemHealth: Int? get() = prefs.getInt("sys_health", -1).takeIf { it > 0 }
    val systemHealthTime: Long get() = prefs.getLong("sys_health_time", 0L)

    fun saveSystemHealth(pct: Int) =
        prefs.edit().putInt("sys_health", pct).putLong("sys_health_time", System.currentTimeMillis()).apply()

    var alertEnabled: Boolean
        get() = prefs.getBoolean("alert_on", false)
        set(v) = prefs.edit().putBoolean("alert_on", v).apply()

    var alertThreshold: Int
        get() = prefs.getInt("alert_c", TemperatureAlert.DEFAULT_THRESHOLD_C)
        set(v) = prefs.edit().putInt("alert_c", v).apply()

    /** Posts a notification if the alert is on, the battery is too hot and none was sent in the last hour. */
    suspend fun checkTemperature(tempC: Float?, nowMs: Long = System.currentTimeMillis()) {
        val threshold = alertThreshold
        if (!TemperatureAlert.shouldNotify(alertEnabled, tempC, threshold, prefs.getLong("alert_last", 0L), nowMs)) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = app.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, getString(Res.string.battery_temp_channel), NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val n = NotificationCompat.Builder(app, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(getString(Res.string.battery_temp_notif_title))
            .setContentText(getString(Res.string.battery_temp_notif_text, "%.1f °C".format(tempC), "$threshold °C"))
            .setContentIntent(app.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                android.app.PendingIntent.getActivity(app, 0, it, android.app.PendingIntent.FLAG_IMMUTABLE)
            })
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(app).notify(NOTIFICATION_ID, n)
            prefs.edit().putLong("alert_last", nowMs).apply()
        } catch (_: SecurityException) {}
    }

    companion object {
        private const val CHANNEL = "battery_temp"
        private const val NOTIFICATION_ID = 4201
        private const val WORK = "battery_history"

        /** Schedules the ~6-hourly sample/alert job (kept if already scheduled). */
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<BatteryWorker>(6, TimeUnit.HOURS).build(),
            )
        }
    }
}

class BatteryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val snapshot = BatteryHealthProvider(applicationContext).read()
        val store = BatteryStore(applicationContext)
        store.record(snapshot)
        store.checkTemperature(snapshot.temperatureC)
        return Result.success()
    }
}

/** Streams a dumpstate .txt or .zip (nested zips too) and returns the battery health %, or null. */
object DumpstateScanner {
    fun scan(context: Context, uri: Uri): Int? =
        context.contentResolver.openInputStream(uri)?.use { scan(it) }

    fun scan(input: InputStream, depth: Int = 0): Int? {
        val bin = BufferedInputStream(input, 64 * 1024)
        bin.mark(4)
        val magic = ByteArray(2).also { bin.read(it) }
        bin.reset()
        if (magic[0] == 'P'.code.toByte() && magic[1] == 'K'.code.toByte() && depth < 3) {
            val zip = ZipInputStream(bin)
            var best: Int? = null
            while (true) {
                val e = zip.nextEntry ?: break
                if (e.isDirectory || e.name.lowercase().let { n -> listOf(".png", ".jpg", ".bin", ".gz", ".so").any { n.endsWith(it) } }) continue
                // Non-closing wrapper so the next entry stays readable.
                val found = scan(object : java.io.FilterInputStream(zip) { override fun close() {} }, depth + 1)
                if (found != null) { best = found; if (e.name.contains("dumpstate", true)) break }
            }
            return best
        }
        return bin.bufferedReader(Charsets.ISO_8859_1).let { DumpstateParser.parse(it.lineSequence()) }
    }
}
