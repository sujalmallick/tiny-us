package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesManager
import com.example.ui.WidgetSceneRenderer
import com.example.resources.*
import com.example.engine.GameText

/**
 * AppWidgetProvider for Tiny Us: a small pixel-art window into the couple's world (plan 06, H).
 * Shows their current scene as a picture, with their names, today's status and the day count.
 */
class TinyUsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateInBackground(context, appWidgetManager, appWidgetIds)
    }

    /** Resized on the home screen: redraw the picture in the new shape. */
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        updateInBackground(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    /** Drawing the scene takes a moment, so it runs off the main thread while the broadcast is held open. */
    private fun updateInBackground(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        Thread {
            try {
                for (id in appWidgetIds) {
                    // Portrait width and landscape height: the picture covers both.
                    val options = appWidgetManager.getAppWidgetOptions(id)
                    val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: DEFAULT_WIDTH_DP
                    val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it > 0 } ?: DEFAULT_HEIGHT_DP
                    appWidgetManager.updateAppWidget(id, buildViews(context, widthDp, heightDp))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Widget update failed", e)
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        private const val TAG = "TinyUsWidget"

        /** Used when the launcher doesn't report the widget's size. */
        internal const val DEFAULT_WIDTH_DP = 250
        internal const val DEFAULT_HEIGHT_DP = 110

        /** The picture is drawn no larger than this, whatever the widget's size. */
        private const val MAX_PICTURE_PX = 1200

        /** The widget's views for a [widthDp] x [heightDp] widget: text, the scene picture, and a tap that opens the app. */
        internal fun buildViews(context: Context, widthDp: Int, heightDp: Int): RemoteViews {
            val prefs = PreferencesManager(context)
            val data = prefs.getWidgetData()
            val scene = WidgetState.scene(context)
            val weather = WidgetState.weather(context)
            val views = RemoteViews(context.packageName, R.layout.tiny_us_widget)

            views.setTextViewText(R.id.widget_couple_names, data.coupleNames)
            views.setTextViewText(R.id.widget_day_counter, GameText.get(Res.string.ui_day_number, data.daysTogether))
            val statusText = when {
                !data.latestSignalText.isNullOrBlank() -> data.latestSignalText
                !data.sharedMoodText.isNullOrBlank() -> context.getString(R.string.widget_mood, data.sharedMoodText)
                !data.dailyMomentPrompt.isNullOrBlank() -> data.dailyMomentPrompt
                else -> context.getString(R.string.widget_status_default)
            }
            views.setTextViewText(R.id.widget_status_text, statusText)

            // The scene picture in the widget's shape, at most MAX_PICTURE_PX wide.
            val density = context.resources.displayMetrics.density
            val widthPx = (widthDp * density).toInt().coerceIn(1, MAX_PICTURE_PX)
            val heightPx = (widthPx.toFloat() * heightDp / widthDp.coerceAtLeast(1)).toInt().coerceAtLeast(1)
            try {
                val picture = WidgetSceneRenderer.render(
                    scene, weather, prefs.atmosphereMode, prefs.boyfriendName, prefs.girlfriendName, widthPx, heightPx
                )
                views.setImageViewBitmap(R.id.widget_scene, picture)
                views.setContentDescription(
                    R.id.widget_scene,
                    context.getString(R.string.widget_scene_description, prefs.boyfriendName, prefs.girlfriendName, scene.title)
                )
            } catch (e: Exception) {
                // Text only, rather than no widget.
                Log.w(TAG, "Widget picture failed", e)
            }

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
            return views
        }

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
