package com.kgurgul.cpuinfo.features.tests

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kgurgul.cpuinfo.shared.Res
import com.kgurgul.cpuinfo.shared.test_camera_front
import com.kgurgul.cpuinfo.shared.test_camera_hint
import com.kgurgul.cpuinfo.shared.test_camera_rear
import com.kgurgul.cpuinfo.shared.test_camera_unavailable
import com.kgurgul.cpuinfo.shared.test_earpiece
import com.kgurgul.cpuinfo.shared.test_earpiece_unavailable
import com.kgurgul.cpuinfo.shared.test_error
import com.kgurgul.cpuinfo.shared.test_grant
import com.kgurgul.cpuinfo.shared.test_loudspeaker
import com.kgurgul.cpuinfo.shared.test_mic_hint
import com.kgurgul.cpuinfo.shared.test_permission_denied
import com.kgurgul.cpuinfo.shared.test_playing
import com.kgurgul.cpuinfo.shared.test_record
import com.kgurgul.cpuinfo.shared.test_recording
import com.kgurgul.cpuinfo.shared.test_speaker_hint
import com.kgurgul.cpuinfo.ui.theme.spacingMedium
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal actual fun DeviceTest(test: HwTest, onResult: (Boolean) -> Unit) {
    when (test) {
        HwTest.SPEAKER -> SpeakerTest(onResult)
        HwTest.MIC -> WithPermission(Manifest.permission.RECORD_AUDIO, onResult) { MicTest(onResult) }
        HwTest.CAMERA -> WithPermission(Manifest.permission.CAMERA, onResult) { CameraTest(onResult) }
        HwTest.SENSORS -> SensorsTest(onResult)
        HwTest.VIBRATION -> VibrationTest(onResult)
        HwTest.BUTTONS -> ButtonsTest(onResult)
        HwTest.FLASHLIGHT -> FlashlightTest(onResult)
        HwTest.CONNECTIVITY -> ConnectivityTest(onResult)
        HwTest.FINGERPRINT -> FingerprintTest(onResult)
        HwTest.CHARGING -> ChargingTest(onResult)
        else -> Unit
    }
}

@Composable
internal fun TestLayout(
    hint: StringResource,
    onResult: (Boolean) -> Unit,
    message: StringResource? = null,
    content: @Composable () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacingMedium),
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(spacingMedium),
    ) {
        Text(stringResource(hint), textAlign = TextAlign.Center)
        message?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { content() }
        ResultBar(onResult)
    }
}

/** Requests [permission] only when this test is opened; on denial explains and lets the user retry or mark fail. */
@Composable
internal fun WithPermission(permission: String, onResult: (Boolean) -> Unit, content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) true
            else null
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (granted == null) launcher.launch(permission) }
    when (granted) {
        true -> content()
        false -> TestLayout(hint = Res.string.test_permission_denied, onResult = onResult) {
            Button(onClick = { launcher.launch(permission) }) { Text(stringResource(Res.string.test_grant)) }
        }
        null -> Unit
    }
}

@Composable
private fun SpeakerTest(onResult: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var playing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<StringResource?>(null) }
    fun play(earpiece: Boolean) {
        playing = true
        message = null
        scope.launch {
            message = withContext(Dispatchers.IO) { playTone(context, earpiece) }
            playing = false
        }
    }
    TestLayout(hint = Res.string.test_speaker_hint, onResult = onResult, message = message) {
        Column(verticalArrangement = Arrangement.spacedBy(spacingMedium)) {
            Button(enabled = !playing, onClick = { play(earpiece = false) }) {
                Text(stringResource(Res.string.test_loudspeaker))
            }
            Button(enabled = !playing, onClick = { play(earpiece = true) }) {
                Text(stringResource(Res.string.test_earpiece))
            }
        }
    }
}

@Composable
private fun MicTest(onResult: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<StringResource?>(null) }
    var error by remember { mutableStateOf<StringResource?>(null) }
    TestLayout(hint = Res.string.test_mic_hint, onResult = onResult, message = error) {
        status?.let { Text(stringResource(it), style = MaterialTheme.typography.titleLarge) }
            ?: Button(onClick = {
                error = null
                scope.launch {
                    status = Res.string.test_recording
                    val pcm = withContext(Dispatchers.IO) { record(MIC_RATE * 3) }
                    if (pcm == null) {
                        error = Res.string.test_error
                    } else {
                        status = Res.string.test_playing
                        val ok = withContext(Dispatchers.IO) {
                            playPcm(pcm, MIC_RATE, AudioAttributes.USAGE_MEDIA, null)
                        }
                        if (!ok) error = Res.string.test_error
                    }
                    status = null
                }
            }) { Text(stringResource(Res.string.test_record)) }
    }
}

@Composable
private fun CameraTest(onResult: (Boolean) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var front by remember { mutableStateOf(false) }
    var available by remember { mutableStateOf(true) }
    val previewView = remember {
        PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
    }
    DisposableEffect(front) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            val p = runCatching { future.get() }.getOrNull() ?: run { available = false; return@addListener }
            provider = p
            val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            available = runCatching {
                p.unbindAll()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                p.bindToLifecycle(lifecycleOwner, selector, preview)
            }.isSuccess
        }, ContextCompat.getMainExecutor(context))
        onDispose { provider?.unbindAll() }
    }
    TestLayout(
        hint = Res.string.test_camera_hint,
        onResult = onResult,
        message = if (available) null else Res.string.test_camera_unavailable,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacingMedium)) {
                FilterChip(selected = !front, onClick = { front = false },
                    label = { Text(stringResource(Res.string.test_camera_rear)) })
                FilterChip(selected = front, onClick = { front = true },
                    label = { Text(stringResource(Res.string.test_camera_front)) })
            }
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }
    }
}

private const val TONE_RATE = 44100
private const val MIC_RATE = 16000

/** Plays a 1 kHz tone on the loudspeaker or earpiece. Returns an error message or null. */
private suspend fun playTone(context: Context, earpiece: Boolean): StringResource? {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val type = if (earpiece) AudioDeviceInfo.TYPE_BUILTIN_EARPIECE else AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
    val device = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.type == type }
    if (earpiece && device == null) return Res.string.test_earpiece_unavailable
    val pcm = ShortArray(TONE_RATE * 3 / 2) {
        (sin(2 * PI * 1000 * it / TONE_RATE) * 0.5 * Short.MAX_VALUE).toInt().toShort()
    }
    if (!earpiece) {
        return if (playPcm(pcm, TONE_RATE, AudioAttributes.USAGE_MEDIA, device)) null else Res.string.test_error
    }
    val oldMode = am.mode
    try {
        am.mode = AudioManager.MODE_IN_COMMUNICATION
        val routed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.availableCommunicationDevices.firstOrNull { it.type == type }
                ?.let { am.setCommunicationDevice(it) } == true
        } else {
            @Suppress("DEPRECATION")
            am.isSpeakerphoneOn = false
            true
        }
        if (!routed) return Res.string.test_earpiece_unavailable
        return if (playPcm(pcm, TONE_RATE, AudioAttributes.USAGE_VOICE_COMMUNICATION, device)) null
        else Res.string.test_error
    } finally {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.clearCommunicationDevice()
        am.mode = oldMode
    }
}

private suspend fun playPcm(pcm: ShortArray, rate: Int, usage: Int, device: AudioDeviceInfo?): Boolean {
    val track = runCatching {
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
    }.getOrNull() ?: return false
    return try {
        track.write(pcm, 0, pcm.size)
        device?.let { track.preferredDevice = it }
        track.play()
        delay(pcm.size * 1000L / rate + 200)
        true
    } catch (_: Exception) {
        false
    } finally {
        track.release()
    }
}

@SuppressLint("MissingPermission") // granted by WithPermission before MicTest is shown
private fun record(samples: Int): ShortArray? {
    val min = AudioRecord.getMinBufferSize(MIC_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    if (min <= 0) return null
    val recorder = runCatching {
        AudioRecord(MediaRecorder.AudioSource.MIC, MIC_RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, maxOf(min, 4096))
    }.getOrNull() ?: return null
    return try {
        if (recorder.state != AudioRecord.STATE_INITIALIZED) return null
        val pcm = ShortArray(samples)
        recorder.startRecording()
        var read = 0
        while (read < samples) {
            val n = recorder.read(pcm, read, minOf(2048, samples - read))
            if (n <= 0) return null
            read += n
        }
        pcm
    } catch (_: Exception) {
        null
    } finally {
        runCatching { recorder.stop() }
        recorder.release()
    }
}
