package com.kgurgul.cpuinfo

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.View
import android.widget.RemoteViews

/**
 * Classic home-screen widget (no extra dependency). Updates on the system's widget period
 * (30 min), on charger connect/disconnect and battery low/okay; no wakeups of its own.
 */
class BatteryWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        runCatching { update(context, manager, ids) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED,
                Intent.ACTION_BATTERY_LOW, Intent.ACTION_BATTERY_OKAY -> updateAll(context)
                else -> super.onReceive(context, intent)
            }
        }
    }

    companion object {
        fun updateAll(context: Context) {
            runCatching {
                val m = AppWidgetManager.getInstance(context)
                val ids = m.getAppWidgetIds(ComponentName(context, BatteryWidget::class.java))
                if (ids.isNotEmpty()) update(context, m, ids)
            }
        }

        private fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val views = runCatching { buildViews(context) }.getOrNull() ?: return
            ids.forEach { id -> runCatching { manager.updateAppWidget(id, views) } }
        }

        private fun buildViews(context: Context): RemoteViews {
            // Sticky broadcast read: no receiver is registered, nothing keeps running.
            val b = runCatching { context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }.getOrNull()
            val level = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = b?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) level * 100 / scale else null
            val temp = b?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)?.takeIf { it != Int.MIN_VALUE && it > -500 }
            val status = b?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            // Health % only when the user imported a system value (dumpstate); otherwise hidden.
            val health = runCatching {
                context.getSharedPreferences("battery", Context.MODE_PRIVATE).getInt("sys_health", -1).takeIf { it in 1..100 }
            }.getOrNull()
            val na = context.getString(R.string.widget_na)
            return RemoteViews(context.packageName, R.layout.widget_battery).apply {
                setTextViewText(R.id.widget_level, pct?.let { "\u200E$it%" } ?: na)
                setTextViewText(R.id.widget_temp, temp?.let { "\u200E${it / 10}.${kotlin.math.abs(it % 10)}°C" } ?: na)
                setTextViewText(
                    R.id.widget_state,
                    context.getString(if (b == null) R.string.widget_na else if (charging) R.string.widget_charging else R.string.widget_not_charging),
                )
                if (health != null) {
                    setViewVisibility(R.id.widget_health, View.VISIBLE)
                    setTextViewText(R.id.widget_health, context.getString(R.string.widget_health, "\u200E$health%"))
                } else {
                    setViewVisibility(R.id.widget_health, View.GONE)
                }
                val open = Intent(context, HostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setOnClickPendingIntent(
                    R.id.widget_root,
                    PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
                )
            }
        }
    }
}
