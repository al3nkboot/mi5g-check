package com.kgurgul.cpuinfo.features.tests

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.GnssStatus
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.kgurgul.cpuinfo.features.battery.BatteryHealthProvider
import com.kgurgul.cpuinfo.features.battery.BatteryMath
import com.kgurgul.cpuinfo.shared.*
import com.kgurgul.cpuinfo.ui.theme.spacingMedium
import com.kgurgul.cpuinfo.ui.theme.spacingSmall
import java.util.Locale
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val Ok = Color(0xFF43A047)

/** Keeps numbers/units left-to-right inside Arabic text. */
internal fun ltr(v: String) = "\u2066$v\u2069"

/** A label/value row inside a card; [ok] colours the value (green/red/neutral). */
@Composable
internal fun ValueRow(label: String, value: String, ok: Boolean? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Content),
            fontWeight = FontWeight.SemiBold,
            color = when (ok) {
                true -> Ok
                false -> MaterialTheme.colorScheme.error
                null -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun RowsCard(content: @Composable () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = spacingMedium, vertical = spacingSmall)) { content() }
    }
}

// ---------- Sensors ----------

data class SensorReading(val name: StringResource, val present: Boolean, val values: String?)

private val sensorTypes = listOf(
    Sensor.TYPE_ACCELEROMETER to Res.string.test_sensor_accelerometer,
    Sensor.TYPE_GYROSCOPE to Res.string.test_sensor_gyroscope,
    Sensor.TYPE_PROXIMITY to Res.string.test_sensor_proximity,
    Sensor.TYPE_LIGHT to Res.string.test_sensor_light,
)

@Composable
internal fun SensorsTest(onResult: (Boolean) -> Unit) {
    val context = LocalContext.current
    val sm = remember { context.getSystemService(SensorManager::class.java) }
    var values by remember { mutableStateOf(mapOf<Int, String>()) }
    val present = remember { sensorTypes.associate { (t, _) -> t to (sm?.getDefaultSensor(t) != null) } }
    DisposableEffect(sm) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val n = if (e.sensor.type == Sensor.TYPE_PROXIMITY || e.sensor.type == Sensor.TYPE_LIGHT) 1 else 3
                values = values + (e.sensor.type to e.values.take(n).joinToString("  ") { String.format(Locale.US, "%.2f", it) })
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        sensorTypes.forEach { (t, _) ->
            sm?.getDefaultSensor(t)?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        }
        onDispose { sm?.unregisterListener(listener) }
    }
    SensorsContent(sensorTypes.map { (t, name) -> SensorReading(name, present[t] == true, values[t]) }, onResult)
}

@Composable
fun SensorsContent(readings: List<SensorReading>, onResult: (Boolean) -> Unit) {
    TestLayout(hint = Res.string.test_sensors_hint, onResult = onResult) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacingSmall),
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            readings.forEach { r ->
                RowsCard {
                    ValueRow(
                        stringResource(r.name),
                        stringResource(if (r.present) Res.string.test_sensor_present else Res.string.test_sensor_missing),
                        ok = r.present,
                    )
                    if (r.present) {
                        Text(
                            ltr(r.values ?: "…"),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

// ---------- Vibration ----------

@Composable
internal fun VibrationTest(onResult: (Boolean) -> Unit) {
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
    }
    val has = vibrator?.hasVibrator() == true
    DisposableEffect(Unit) { onDispose { vibrator?.cancel() } }
    TestLayout(
        hint = Res.string.test_vibration_hint,
        onResult = onResult,
        message = if (has) null else Res.string.test_vibration_unavailable,
    ) {
        Button(enabled = has, onClick = {
            val pattern = longArrayOf(0, 300, 150, 300, 150, 600)
            if (Build.VERSION.SDK_INT >= 26) vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            else @Suppress("DEPRECATION") vibrator?.vibrate(pattern, -1)
        }) { Text(stringResource(Res.string.test_vibrate)) }
    }
}

// ---------- Buttons ----------

@Composable
internal fun ButtonsTest(onResult: (Boolean) -> Unit) {
    var up by remember { mutableStateOf(false) }
    var down by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(
        Modifier.focusRequester(focus).focusable().onPreviewKeyEvent {
            when (it.nativeKeyEvent.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> { up = true; true }
                KeyEvent.KEYCODE_VOLUME_DOWN -> { down = true; true }
                else -> false
            }
        }
    ) {
        TestLayout(hint = Res.string.test_buttons_hint, onResult = onResult) {
            Column(verticalArrangement = Arrangement.spacedBy(spacingSmall)) {
                RowsCard {
                    val pressed = stringResource(Res.string.test_pressed)
                    val waiting = stringResource(Res.string.test_waiting)
                    ValueRow(stringResource(Res.string.test_volume_up), if (up) pressed else waiting, ok = up.takeIf { it })
                    ValueRow(stringResource(Res.string.test_volume_down), if (down) pressed else waiting, ok = down.takeIf { it })
                }
                Text(
                    stringResource(Res.string.test_power_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------- Flashlight ----------

@Composable
internal fun FlashlightTest(onResult: (Boolean) -> Unit) {
    val context = LocalContext.current
    val cm = remember { context.getSystemService(CameraManager::class.java) }
    val torchId = remember {
        runCatching {
            cm?.cameraIdList?.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }
    var on by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(torchId == null) }
    DisposableEffect(torchId) { onDispose { if (torchId != null) runCatching { cm?.setTorchMode(torchId, false) } } }
    TestLayout(
        hint = Res.string.test_flashlight_hint,
        onResult = onResult,
        message = when {
            torchId == null -> Res.string.test_flashlight_unavailable
            error -> Res.string.test_error
            else -> null
        },
    ) {
        Button(enabled = torchId != null, onClick = {
            val target = !on
            error = runCatching { cm?.setTorchMode(torchId!!, target) }.isFailure
            if (!error) on = target
        }) { Text(stringResource(if (on) Res.string.test_torch_off else Res.string.test_torch_on)) }
    }
}

// ---------- Connectivity ----------

@SuppressLint("MissingPermission")
@Composable
internal fun ConnectivityTest(onResult: (Boolean) -> Unit) {
    val context = LocalContext.current
    val wifi = remember { context.applicationContext.getSystemService(WifiManager::class.java) }
    val cm = remember { context.getSystemService(ConnectivityManager::class.java) }
    val bt = remember { context.getSystemService(BluetoothManager::class.java)?.adapter }
    val lm = remember { context.getSystemService(LocationManager::class.java) }
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); tick++ } }
    tick // re-read radio states every second
    val wifiOn = runCatching { wifi?.isWifiEnabled == true }.getOrDefault(false)
    val wifiConnected = runCatching {
        cm?.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }.getOrDefault(false)
    val btOn: Boolean? = if (bt == null) null else runCatching { bt.isEnabled }.getOrNull()

    val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    fun fineGranted() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    var gpsRequested by remember { mutableStateOf(false) }
    var granted by remember { mutableStateOf(fineGranted()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = fineGranted()
        gpsRequested = true
    }
    var seen by remember { mutableStateOf(0) }
    var used by remember { mutableStateOf(0) }
    var fix by remember { mutableStateOf(false) }
    val gpsActive = gpsRequested && granted && lm != null
    DisposableEffect(gpsActive) {
        if (!gpsActive) return@DisposableEffect onDispose {}
        val handler = Handler(Looper.getMainLooper())
        val gnss = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                seen = status.satelliteCount
                used = (0 until status.satelliteCount).count { status.usedInFix(it) }
            }
            override fun onFirstFix(ttffMillis: Int) { fix = true }
        }
        val listener = LocationListener { fix = true }
        runCatching {
            lm!!.registerGnssStatusCallback(gnss, handler)
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
        }
        onDispose {
            runCatching { lm?.unregisterGnssStatusCallback(gnss) }
            runCatching { lm?.removeUpdates(listener) }
        }
    }
    val on = stringResource(Res.string.test_on)
    val off = stringResource(Res.string.test_off)
    TestLayout(hint = Res.string.test_connectivity_hint, onResult = onResult) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacingSmall),
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            RowsCard {
                ValueRow(
                    stringResource(Res.string.test_wifi),
                    (if (wifiOn) on else off) + " · " +
                        stringResource(if (wifiConnected) Res.string.test_connected else Res.string.test_not_connected),
                    wifiOn,
                )
            }
            RowsCard {
                ValueRow(
                    stringResource(Res.string.test_bluetooth),
                    when (btOn) {
                        null -> stringResource(Res.string.test_sensor_missing)
                        true -> on
                        false -> off
                    },
                    btOn,
                )
            }
            RowsCard {
                val gpsEnabled = runCatching { lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true }.getOrDefault(false)
                val hasGps = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
                ValueRow(
                    stringResource(Res.string.test_gps),
                    when {
                        !hasGps -> stringResource(Res.string.test_sensor_missing)
                        !gpsActive -> ""
                        fix -> stringResource(Res.string.test_gps_fix, used)
                        else -> stringResource(Res.string.test_gps_searching, seen, used)
                    },
                    if (fix) true else if (!hasGps) false else null,
                )
                when {
                    !hasGps -> Unit
                    gpsRequested && !granted -> {
                        Text(stringResource(Res.string.test_permission_denied), color = MaterialTheme.colorScheme.error)
                        Button(onClick = { launcher.launch(perms) }) { Text(stringResource(Res.string.test_grant)) }
                    }
                    gpsActive && !gpsEnabled ->
                        Text(stringResource(Res.string.test_gps_disabled), color = MaterialTheme.colorScheme.error)
                    !gpsActive -> Button(onClick = { launcher.launch(perms) }) { Text(stringResource(Res.string.test_gps_start)) }
                    else -> Unit
                }
            }
        }
    }
}

// ---------- Fingerprint ----------

private tailrec fun Context.findActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
internal fun FingerprintTest(onResult: (Boolean) -> Unit) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK
    val availability = remember { BiometricManager.from(context).canAuthenticate(authenticators) }
    var message by remember {
        mutableStateOf(
            when (availability) {
                BiometricManager.BIOMETRIC_SUCCESS -> null
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Res.string.test_bio_no_hardware
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Res.string.test_bio_not_enrolled
                else -> Res.string.test_bio_unavailable
            }
        )
    }
    var success by remember { mutableStateOf(false) }
    val title = stringResource(Res.string.test_fingerprint)
    val cancel = stringResource(Res.string.report_close)
    TestLayout(hint = Res.string.test_fingerprint_hint, onResult = onResult, message = message) {
        if (success) {
            Text(stringResource(Res.string.test_auth_success), style = MaterialTheme.typography.titleLarge, color = Ok)
        } else {
            Button(enabled = availability == BiometricManager.BIOMETRIC_SUCCESS && activity != null, onClick = {
                val prompt = BiometricPrompt(
                    activity!!,
                    ContextCompat.getMainExecutor(context),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            success = true
                            message = null
                        }
                        override fun onAuthenticationFailed() { message = Res.string.test_auth_failed }
                        override fun onAuthenticationError(code: Int, err: CharSequence) {
                            if (code != BiometricPrompt.ERROR_USER_CANCELED && code != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                message = Res.string.test_bio_unavailable
                            }
                        }
                    },
                )
                prompt.authenticate(
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle(title)
                        .setNegativeButtonText(cancel)
                        .setAllowedAuthenticators(authenticators)
                        .build()
                )
            }) { Text(stringResource(Res.string.test_authenticate)) }
        }
    }
}

// ---------- Charging ----------

@Composable
internal fun ChargingTest(onResult: (Boolean) -> Unit) {
    val context = LocalContext.current
    val provider = remember { BatteryHealthProvider(context.applicationContext) }
    var snapshot by remember { mutableStateOf(provider.read()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); snapshot = provider.read() } }
    val plugged = snapshot.chargers.isNotEmpty()
    val watts = BatteryMath.powerW(snapshot.currentMa, snapshot.voltageV)
    val estimated = stringResource(Res.string.battery_estimated_value)
    val na = stringResource(Res.string.battery_not_available)
    TestLayout(hint = Res.string.test_charging_hint, onResult = onResult) {
        if (!plugged) {
            Text(stringResource(Res.string.test_plug_in), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacingSmall), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    watts?.let { ltr(String.format(Locale.US, "%.1f W", it)) } ?: na,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )
                RowsCard {
                    ValueRow(stringResource(Res.string.test_charger_type), snapshot.chargers.map { stringResource(it) }.joinToString(" + "))
                    snapshot.currentMa?.let { ValueRow(stringResource(Res.string.battery_live_current), ltr("$it mA")) }
                    snapshot.voltageV?.let { ValueRow(stringResource(Res.string.voltage), ltr(String.format(Locale.US, "%.2f V", it))) }
                    if (watts != null) {
                        val fast = BatteryMath.isFastCharge(watts)
                        ValueRow(
                            stringResource(Res.string.test_charging_power),
                            stringResource(if (fast) Res.string.test_fast_charging else Res.string.test_normal_charging) + " ($estimated)",
                            fast.takeIf { it },
                        )
                    }
                }
            }
        }
    }
}
