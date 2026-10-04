package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesManager

/**
 * AppWidgetProvider for Tiny Us.
 * Serves as a small, lightweight pixel-art window into the couple's world.
 */
class TinyUsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = PreferencesManager(context)
        val data = prefs.getWidgetData()

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.tiny_us_widget)

            // Populate data
            views.setTextViewText(R.id.widget_day_counter, context.getString(R.string.ui_day_number, data.daysTogether))
            views.setTextViewText(R.id.widget_couple_names, data.coupleNames)

            val statusText = when {
                !data.latestSignalText.isNullOrBlank() -> data.latestSignalText
                !data.sharedMoodText.isNullOrBlank() -> context.getString(R.string.widget_mood, data.sharedMoodText)
                !data.dailyMomentPrompt.isNullOrBlank() -> data.dailyMomentPrompt
                else -> context.getString(R.string.widget_status_default)
            }
            views.setTextViewText(R.id.widget_status_text, statusText)
            views.setTextViewText(R.id.widget_subtext, "${data.sceneName} • ${data.weatherName}")

            // Tap on widget opens main app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    companion object {
        /**
         * Broadcasts an immediate update to all active Tiny Us home screen widgets.
         */
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, TinyUsWidgetProvider::class.java)
                )
                if (ids.isNotEmpty()) {
                    val intent = Intent(context, TinyUsWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (_: Exception) {
                // Ignore if widget is not installed
            }
        }
    }
}
