package com.kgurgul.cpuinfo.features.tests

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.kgurgul.cpuinfo.features.battery.BatteryHealthProvider
import com.kgurgul.cpuinfo.features.battery.BatteryMath
import com.kgurgul.cpuinfo.shared.*
import com.kgurgul.cpuinfo.ui.theme.spacingMedium
import com.kgurgul.cpuinfo.ui.theme.spacingSmall
import java.io.File
import java.text.DateFormat
import java.util.Date
import org.jetbrains.compose.resources.stringResource

private val PassColor = Color(0xFF2E7D32)

/** Report fully resolved to display text in the current app language (used for screen, PNG and PDF). */
data class ReportText(
    val title: String,
    val info: List<Pair<String, String>>,
    val resultsTitle: String,
    val summary: String,
    val results: List<Triple<String, String, Boolean?>>,
    val footer: String,
)

@Composable
fun rememberReportText(report: TestReport, batteryHealth: String): ReportText {
    val pass = stringResource(Res.string.test_pass)
    val fail = stringResource(Res.string.test_fail)
    val notRun = stringResource(Res.string.test_not_run)
    return ReportText(
        title = stringResource(Res.string.report_title),
        info = listOf(
            stringResource(Res.string.report_device) to ltr(report.model),
            stringResource(Res.string.report_android) to ltr(report.androidVersion),
            stringResource(Res.string.report_battery_health) to batteryHealth,
            stringResource(Res.string.report_date) to report.date,
        ),
        resultsTitle = stringResource(Res.string.report_results),
        summary = stringResource(Res.string.report_summary, report.passed, report.failed, report.notRun),
        results = report.results.map { (t, r) ->
            Triple(stringResource(t.title), when (r) { true -> pass; false -> fail; null -> notRun }, r)
        },
        footer = stringResource(Res.string.report_footer),
    )
}

@Composable
internal actual fun ReportView(results: Map<HwTest, Boolean>, onClose: () -> Unit) {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val snapshot = remember { BatteryHealthProvider(context.applicationContext).read() }
    val systemHealth = remember { com.kgurgul.cpuinfo.features.battery.BatteryStore(context).systemHealth }
    val report = remember(results) {
        TestReport.from(
            results = results.toMap(),
            model = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            batteryHealthPercent = systemHealth ?: BatteryMath.healthPercent(
                BatteryMath.estimateFullMah(snapshot.chargeCounterMah, snapshot.levelPercent), snapshot.designMah,
            ),
            date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date()),
        )
    }
    val estimated = stringResource(Res.string.battery_estimated_value)
    val fromSystem = stringResource(Res.string.battery_from_system)
    val healthText = listOfNotNull(
        report.batteryHealthPercent?.let { ltr("$it%") + " (${if (systemHealth != null) fromSystem else estimated})" },
        snapshot.health?.let { stringResource(it) },
    ).joinToString(" · ").ifEmpty { stringResource(Res.string.battery_not_available) }
    val perf = remember { com.kgurgul.cpuinfo.features.perf.PerfStore(context) }
    val base = rememberReportText(report, healthText)
    val bench = perf.bench
    val speed = perf.storage
    val approx = stringResource(Res.string.perf_approx)
    val text = base.copy(
        info = base.info + listOfNotNull(
            bench?.let {
                stringResource(Res.string.report_cpu_score) to
                    "${stringResource(Res.string.perf_single)} ${ltr("${it.single}")} · ${stringResource(Res.string.perf_multi, "${it.threads}")} ${ltr("${it.multi}")}"
            },
            speed?.let {
                stringResource(Res.string.report_storage_speed) to
                    "${stringResource(Res.string.perf_write)} ${ltr("${it.writeMbS} MB/s")} · ${stringResource(Res.string.perf_read)} ${ltr("${it.readMbS} MB/s")} ($approx)"
            },
        ),
    )
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val brand = com.kgurgul.cpuinfo.ui.theme.md_theme_light_primary.toArgb()
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = spacingSmall)) {
            Text(text.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(spacingSmall))
            TextButton(onClick = onClose) { Text(stringResource(Res.string.report_close)) }
        }
        ReportContent(text, Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacingMedium),
            modifier = Modifier.fillMaxWidth().padding(spacingMedium),
        ) {
            Button(onClick = { shareReport(context, text, rtl, brand, pdf = false) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Image, contentDescription = null)
                Text(" " + stringResource(Res.string.report_share_image))
            }
            FilledTonalButton(onClick = { shareReport(context, text, rtl, brand, pdf = true) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
                Text(" " + stringResource(Res.string.report_share_pdf))
            }
        }
    }
}

@Composable
fun ReportContent(text: ReportText, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(spacingSmall),
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = spacingMedium),
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = spacingMedium, vertical = spacingSmall)) {
                text.info.forEach { (k, v) -> ValueRow(k, v) }
            }
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = spacingMedium, vertical = spacingSmall)) {
                Text(text.resultsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("\u2068${text.summary}\u2069", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                text.results.forEach { (name, value, ok) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(
                            value,
                            fontWeight = FontWeight.SemiBold,
                            color = when (ok) {
                                true -> PassColor
                                false -> MaterialTheme.colorScheme.error
                                null -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
        Text(
            text.footer,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = spacingSmall),
        )
    }
}

// ---------- Rendering to bitmap / PDF (same drawing code for both) ----------

private const val W = 1080
private const val PAD = 64f

private fun layout(text: String, size: Float, color: Int, bold: Boolean, width: Int, rtl: Boolean, align: Layout.Alignment) =
    StaticLayout.Builder.obtain(
        text, 0, text.length,
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        },
        width,
    ).setAlignment(align)
        .setTextDirection(if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
        .build()

/** Draws the report on [canvas] (white page) and returns the total height; pass null to measure. */
private fun drawReport(canvas: Canvas?, t: ReportText, rtl: Boolean, brand: Int): Int {
    val inner = (W - 2 * PAD).toInt()
    var y = 0f
    val dark = 0xFF1B1B1F.toInt()
    val grey = 0xFF5F6368.toInt()
    fun put(l: StaticLayout, gap: Float = 16f) {
        canvas?.let { c -> c.save(); c.translate(PAD, y); l.draw(c); c.restore() }
        y += l.height + gap
    }
    fun row(label: String, value: String, valueColor: Int) {
        val v = layout(value, 36f, valueColor, true, inner / 2, rtl, Layout.Alignment.ALIGN_OPPOSITE)
        val k = layout(label, 36f, dark, false, inner / 2, rtl, Layout.Alignment.ALIGN_NORMAL)
        canvas?.let { c ->
            c.save(); c.translate(if (rtl) PAD + inner / 2f else PAD, y); k.draw(c); c.restore()
            c.save(); c.translate(if (rtl) PAD else PAD + inner / 2f, y); v.draw(c); c.restore()
        }
        y += maxOf(k.height, v.height) + 18f
    }
    // Header band
    val header = layout(t.title, 56f, android.graphics.Color.WHITE, true, inner, rtl, Layout.Alignment.ALIGN_NORMAL)
    val headerH = header.height + 2 * PAD
    canvas?.drawRect(0f, 0f, W.toFloat(), headerH, Paint().apply { color = brand })
    y = PAD
    put(header, 0f)
    y = headerH + PAD
    t.info.forEach { (k, v) -> row(k, v, dark) }
    y += 24f
    put(layout(t.resultsTitle, 44f, dark, true, inner, rtl, Layout.Alignment.ALIGN_NORMAL), 8f)
    put(layout(t.summary, 32f, grey, false, inner, rtl, Layout.Alignment.ALIGN_NORMAL), 24f)
    t.results.forEach { (name, value, ok) ->
        row(name, value, when (ok) { true -> 0xFF2E7D32.toInt(); false -> 0xFFC62828.toInt(); null -> grey })
    }
    y += 32f
    canvas?.drawRect(PAD, y, W - PAD, y + 3f, Paint().apply { color = brand })
    y += 24f
    put(layout(t.footer, 30f, grey, false, inner, rtl, Layout.Alignment.ALIGN_CENTER), PAD)
    return y.toInt()
}

fun renderReportBitmap(t: ReportText, rtl: Boolean, brand: Int): Bitmap {
    val h = drawReport(null, t, rtl, brand)
    return Bitmap.createBitmap(W, h, Bitmap.Config.ARGB_8888).also {
        val c = Canvas(it)
        c.drawColor(android.graphics.Color.WHITE)
        drawReport(c, t, rtl, brand)
    }
}

private fun shareReport(context: Context, t: ReportText, rtl: Boolean, brand: Int, pdf: Boolean) {
    val dir = File(context.cacheDir, "reports").apply { mkdirs() }
    val file = File(dir, if (pdf) "mi5g-check-report.pdf" else "mi5g-check-report.png")
    runCatching {
        if (pdf) {
            val h = drawReport(null, t, rtl, brand)
            val scale = 595f / W // A4 width in points
            val doc = PdfDocument()
            val page = doc.startPage(PdfDocument.PageInfo.Builder(595, (h * scale).toInt() + 1, 1).create())
            page.canvas.scale(scale, scale)
            drawReport(page.canvas, t, rtl, brand)
            doc.finishPage(page)
            file.outputStream().use { doc.writeTo(it) }
            doc.close()
        } else {
            val bmp = renderReportBitmap(t, rtl, brand)
            file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.reports", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = if (pdf) "application/pdf" else "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, t.title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
