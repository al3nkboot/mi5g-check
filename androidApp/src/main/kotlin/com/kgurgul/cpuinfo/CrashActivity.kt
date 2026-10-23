package com.kgurgul.cpuinfo

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Minimal crash report screen: plain framework views, no Compose/Koin, runs in its own
 * ":crash" process so it still works when the main UI or Application init crashes.
 */
class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = CrashLog.pendingReport(this)
        if (text == null) { openApp(); return }
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
            fitsSystemWindows = true
        }
        root.addView(TextView(this).apply {
            this.text = "آخر سبب لإغلاق التطبيق"
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
        })
        val body = TextView(this).apply {
            this.text = text
            textSize = 12f
            typeface = Typeface.MONOSPACE
            textDirection = View.TEXT_DIRECTION_LTR
            setTextIsSelectable(true)
        }
        root.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(-1, 0, 1f))
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun button(label: String, action: () -> Unit) =
            buttons.addView(Button(this).apply { this.text = label; setOnClickListener { action() } }, LinearLayout.LayoutParams(0, -2, 1f))
        button("نسخ") {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("crash", text))
            Toast.makeText(this, "تم النسخ", Toast.LENGTH_SHORT).show()
        }
        button("مشاركة") {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, "Mi5G Check crash")
                .putExtra(Intent.EXTRA_TEXT, text)
            startActivity(Intent.createChooser(send, null))
        }
        button("حذف") { CrashLog.delete(this); openApp() }
        root.addView(buttons)
        setContentView(root)
    }

    private fun openApp() {
        startActivity(Intent(this, HostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
