package com.kgurgul.cpuinfo.features.perf

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.kgurgul.cpuinfo.shared.Res
import com.kgurgul.cpuinfo.shared.perf_title
import com.kgurgul.cpuinfo.shared.report_close
import com.kgurgul.cpuinfo.ui.theme.spacingSmall
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/** Last benchmark / storage results, kept across app restarts. */
class PerfStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("perf", Context.MODE_PRIVATE)

    var bench: BenchResult?
        get() = if (!prefs.contains("single")) null else BenchResult(
            prefs.getInt("single", 0), prefs.getInt("multi", 0), prefs.getInt("threads", 0),
            prefs.getFloat("seconds", 0f).toDouble(),
        )
        set(v) { v?.let { prefs.edit().putInt("single", it.single).putInt("multi", it.multi).putInt("threads", it.threads).putFloat("seconds", it.seconds.toFloat()).apply() } }

    var storage: StorageSpeed?
        get() = if (!prefs.contains("write")) null else StorageSpeed(
            prefs.getFloat("write", 0f).toDouble(), prefs.getFloat("read", 0f).toDouble(), prefs.getLong("sizeMb", 0),
        )
        set(v) { v?.let { prefs.edit().putFloat("write", it.writeMbS.toFloat()).putFloat("read", it.readMbS.toFloat()).putLong("sizeMb", it.sizeMb).apply() } }
}

/** Best effort: UFS shows up as a SCSI disk (sda), eMMC as mmcblk0 with type MMC. Null if unreadable. */
fun detectStorageType(): String? = runCatching {
    val mmcType = runCatching { File("/sys/block/mmcblk0/device/type").readText().trim() }.getOrNull()
    val ufsHost = runCatching {
        File("/sys/class/scsi_host").listFiles()?.any { h ->
            runCatching { File(h, "proc_name").readText().contains("ufs", true) }.getOrDefault(false)
        } == true
    }.getOrDefault(false)
    when {
        ufsHost || File("/sys/block/sda").exists() -> "UFS"
        mmcType == "MMC" -> "eMMC"
        else -> null
    }
}.getOrNull()

/** Sequential write (with fsync) then read of a temp file in the cache dir; always deleted. Null if low space. */
suspend fun runStorageTest(dir: File): StorageSpeed? = withContext(Dispatchers.IO) {
    val size = PerfMath.storageTestBytes(dir.usableSpace) ?: return@withContext null
    val file = File(dir, "speedtest.tmp")
    val buf = ByteArray(4 * 1024 * 1024) { (it * 31).toByte() }
    try {
        val w0 = System.nanoTime()
        FileOutputStream(file).use { out ->
            var left = size
            while (left > 0) {
                ensureActive()
                val n = minOf(left, buf.size.toLong()).toInt()
                out.write(buf, 0, n)
                left -= n
            }
            out.fd.sync()
        }
        val wNs = System.nanoTime() - w0
        val r0 = System.nanoTime()
        var read = 0L
        FileInputStream(file).use { inp ->
            while (true) {
                ensureActive()
                val n = inp.read(buf)
                if (n < 0) break
                read += n
            }
        }
        val rNs = System.nanoTime() - r0
        StorageSpeed(PerfMath.mbPerSec(size, wNs) ?: 0.0, PerfMath.mbPerSec(read, rNs) ?: 0.0, size / PerfMath.MB)
    } finally {
        file.delete()
    }
}

@Composable
actual fun PerformanceView(onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember { PerfStore(context) }
    val scope = rememberCoroutineScope()
    var bench by remember { mutableStateOf(store.bench) }
    var storage by remember { mutableStateOf(store.storage) }
    var progress by remember { mutableStateOf<Float?>(null) }
    var benchJob by remember { mutableStateOf<Job?>(null) }
    var storageRunning by remember { mutableStateOf(false) }
    var lowSpace by remember { mutableStateOf(false) }
    val storageType = remember { detectStorageType() }
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = spacingSmall)) {
            Text(stringResource(Res.string.perf_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(spacingSmall))
            TextButton(onClick = { benchJob?.cancel(); onClose() }) { Text(stringResource(Res.string.report_close)) }
        }
        PerformanceContent(
            bench = bench,
            benchProgress = progress,
            onRunBench = {
                if (benchJob?.isActive != true && !storageRunning) {
                    progress = 0f
                    benchJob = scope.launch {
                        try {
                            val r = runCpuBench(Runtime.getRuntime().availableProcessors()) { p -> progress = p }
                            bench = r
                            store.bench = r
                        } finally {
                            progress = null
                        }
                    }
                }
            },
            onCancelBench = { benchJob?.cancel() },
            storageType = storageType,
            storage = storage,
            storageRunning = storageRunning,
            storageLowSpace = lowSpace,
            onRunStorage = {
                if (!storageRunning && benchJob?.isActive != true) {
                    storageRunning = true
                    scope.launch {
                        try {
                            val r = runStorageTest(context.cacheDir)
                            lowSpace = r == null
                            if (r != null) { storage = r; store.storage = r }
                        } finally {
                            storageRunning = false
                        }
                    }
                }
            },
            modifier = Modifier.weight(1f),
        )
    }
}
