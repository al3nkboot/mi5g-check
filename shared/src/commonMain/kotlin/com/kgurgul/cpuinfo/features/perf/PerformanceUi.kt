package com.kgurgul.cpuinfo.features.perf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kgurgul.cpuinfo.shared.*
import com.kgurgul.cpuinfo.ui.theme.spacingMedium
import com.kgurgul.cpuinfo.ui.theme.spacingSmall
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.time.TimeSource
import org.jetbrains.compose.resources.stringResource

/** Wraps a left-to-right value (numbers, units) so it renders correctly inside RTL text. */
internal fun ltrValue(s: String) = "\u200E$s\u200E"

fun formatSeconds(s: Double) = "${(s * 10).roundToLong() / 10.0} s"

/**
 * Runs the benchmark: [phaseSeconds] single-core, then the same on [threads] parallel workers
 * on Dispatchers.Default. Cancellable between units. [onProgress] gets 0..1.
 */
suspend fun runCpuBench(threads: Int, phaseSeconds: Double = 6.0, onProgress: (Float) -> Unit = {}): BenchResult =
    withContext(Dispatchers.Default) {
        val total = TimeSource.Monotonic.markNow()
        val phaseNs = (phaseSeconds * 1e9).toLong()
        var sink = 0L
        val start1 = TimeSource.Monotonic.markNow()
        var units = 0L
        while (start1.elapsedNow().inWholeNanoseconds < phaseNs) {
            ensureActive()
            sink += CpuBench.unit(units.toInt())
            units++
            onProgress(0.5f * start1.elapsedNow().inWholeNanoseconds / phaseNs)
        }
        val single = CpuBench.score(units, start1.elapsedNow().inWholeNanoseconds)
        val start2 = TimeSource.Monotonic.markNow()
        val perThread = coroutineScope {
            (0 until threads).map { t ->
                async {
                    var n = 0L
                    while (start2.elapsedNow().inWholeNanoseconds < phaseNs) {
                        ensureActive()
                        sink += CpuBench.unit(n.toInt() + t)
                        n++
                        if (t == 0) onProgress(0.5f + 0.5f * start2.elapsedNow().inWholeNanoseconds / phaseNs)
                    }
                    n
                }
            }.awaitAll()
        }
        val multi = CpuBench.score(perThread.sum(), start2.elapsedNow().inWholeNanoseconds)
        if (sink == 42L) onProgress(1f) // keep the work observable so it is not optimized away
        BenchResult(single, multi, threads, total.elapsedNow().inWholeMilliseconds / 1000.0)
    }

@Composable
fun PerformanceContent(
    bench: BenchResult?,
    benchProgress: Float?,
    onRunBench: () -> Unit,
    onCancelBench: () -> Unit,
    storageType: String?,
    storage: StorageSpeed?,
    storageRunning: Boolean,
    storageLowSpace: Boolean,
    onRunStorage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val na = stringResource(Res.string.battery_not_available)
    Column(
        verticalArrangement = Arrangement.spacedBy(spacingSmall),
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(spacingMedium),
    ) {
        Section(Icons.Outlined.Memory, stringResource(Res.string.perf_cpu)) {
            if (bench != null) {
                ScoreRow(stringResource(Res.string.perf_single), ltrValue("${bench.single}"))
                ScoreRow(stringResource(Res.string.perf_multi, "${bench.threads}"), ltrValue("${bench.multi}"))
                ValueLine(stringResource(Res.string.perf_time), ltrValue(formatSeconds(bench.seconds)))
            } else {
                ValueLine(stringResource(Res.string.perf_cpu), stringResource(Res.string.perf_not_run))
            }
            Note(stringResource(Res.string.perf_cpu_note))
            RunRow(benchProgress, onRunBench, onCancelBench)
        }
        Section(Icons.Outlined.SdStorage, stringResource(Res.string.perf_storage)) {
            ValueLine(stringResource(Res.string.perf_storage_type), storageType ?: na)
            val approx = stringResource(Res.string.perf_approx)
            when {
                storageLowSpace -> Note(stringResource(Res.string.perf_low_space))
                storage != null -> {
                    ValueLine(stringResource(Res.string.perf_write), ltrValue("${storage.writeMbS} MB/s") + " ($approx)")
                    ValueLine(stringResource(Res.string.perf_read), ltrValue("${storage.readMbS} MB/s") + " ($approx)")
                }
                else -> ValueLine(stringResource(Res.string.perf_storage), stringResource(Res.string.perf_not_run))
            }
            Note(stringResource(Res.string.perf_storage_note))
            RunRow(if (storageRunning) -1f else null, onRunStorage, null)
        }
    }
}

@Composable
private fun Section(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(spacingMedium), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(spacingSmall))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun ScoreRow(name: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun ValueLine(name: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** [progress]: null = idle, -1 = indeterminate, 0..1 = determinate. */
@Composable
private fun RunRow(progress: Float?, onRun: () -> Unit, onCancel: (() -> Unit)?) {
    if (progress == null) {
        Button(onClick = onRun, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.perf_run)) }
    } else {
        Text(stringResource(Res.string.perf_running), style = MaterialTheme.typography.bodyMedium)
        if (progress < 0f) {
            LinearProgressIndicator(strokeCap = StrokeCap.Round, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        } else {
            LinearProgressIndicator(progress = { progress }, strokeCap = StrokeCap.Round, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        }
        if (onCancel != null) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.perf_cancel)) }
        }
    }
}

/** Platform screen: runs the tests and keeps the last results. */
@Composable expect fun PerformanceView(onClose: () -> Unit)
