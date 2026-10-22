package com.kgurgul.cpuinfo.features.battery

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal actual fun BatteryHost(uiState: BatteryHealthViewModel.UiState) {
    val context = LocalContext.current
    val store = remember { BatteryStore(context) }
    val scope = rememberCoroutineScope()
    val locale = context.resources.configuration.locales[0]
    var history by remember { mutableStateOf(emptyList<BatterySample>()) }
    var sysHealth by remember { mutableStateOf(store.systemHealth) }
    var sysTime by remember { mutableStateOf(store.systemHealthTime) }
    var status by remember { mutableStateOf<ImportStatus?>(null) }
    var alertOn by remember { mutableStateOf(store.alertEnabled) }
    var threshold by remember { mutableStateOf(store.alertThreshold) }
    var showHelp by remember { mutableStateOf(false) }

    // Record a sample on open (once real data has loaded) and check the temperature alert.
    val loaded = !uiState.isLoading
    LaunchedEffect(loaded) {
        if (!loaded) return@LaunchedEffect
        withContext(Dispatchers.IO) { store.record(uiState.snapshot); history = store.history() }
    }
    val tempBucket = uiState.snapshot.temperatureC?.toInt()
    LaunchedEffect(tempBucket, alertOn, threshold) {
        withContext(Dispatchers.IO) { store.checkTemperature(uiState.snapshot.temperatureC) }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        status = ImportStatus.Scanning
        scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { DumpstateScanner.scan(context, uri) } }
            status = r.fold({ pct ->
                if (pct != null) {
                    store.saveSystemHealth(pct); sysHealth = pct; sysTime = store.systemHealthTime
                    ImportStatus.Found(pct)
                } else ImportStatus.NotFound
            }, { ImportStatus.Error })
        }
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        alertOn = granted; store.alertEnabled = granted
    }

    BatteryHealthScreen(
        uiState = uiState,
        extras = BatteryExtras(
            history = history,
            nowMs = System.currentTimeMillis(),
            systemHealth = sysHealth,
            systemHealthDate = sysTime.takeIf { it > 0 }?.let { DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(it)) },
            importStatus = status,
            alertEnabled = alertOn,
            alertThresholdC = threshold,
        ),
        actions = BatteryActions(
            onImport = { picker.launch(arrayOf("text/plain", "application/zip", "application/x-zip-compressed", "application/octet-stream", "text/*")) },
            onHelp = { showHelp = true },
            onAlertEnabled = { on ->
                if (on && Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else { alertOn = on; store.alertEnabled = on }
            },
            onAlertThreshold = { threshold = it; store.alertThreshold = it },
        ),
    )
    if (showHelp) {
        ModalBottomSheet(onDismissRequest = { showHelp = false }) {
            DumpstateHelpContent(Modifier.verticalScroll(rememberScrollState()))
        }
    }
}
