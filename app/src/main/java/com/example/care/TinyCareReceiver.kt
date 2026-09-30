package com.example.care

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.data.PreferencesManager
import java.util.Calendar

class TinyCareReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = PreferencesManager(context)
        if (!prefs.tinyCareEnabled) {
            TinyCareScheduler.cancelAlarm(context)
            return
        }

        // Verify system-level notification permissions
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            prefs.tinyCareEnabled = false
            TinyCareScheduler.cancelAlarm(context)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                prefs.tinyCareEnabled = false
                TinyCareScheduler.cancelAlarm(context)
                return
            }
        }

        // Quiet hours check (supports force_deliver flag for Doze testing)
        val forceDeliver = intent?.getBooleanExtra("force_deliver", false) == true
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val inQuiet = if (forceDeliver) false else TinyCareScheduler.isTimeInQuietWindow(
            hour = currentHour,
            startHour = prefs.tinyCareQuietStartHour,
            endHour = prefs.tinyCareQuietEndHour
        )

        android.util.Log.d("TinyCare", "Receiver running. enabled=${prefs.tinyCareEnabled}, force=$forceDeliver, hour=$currentHour, quiet=${prefs.tinyCareQuietStartHour}..${prefs.tinyCareQuietEndHour}, inQuiet=$inQuiet")

        if (!inQuiet) {
            val recentIds = prefs.getTinyCareRecentMessageIds()
            val message = TinyCareMessagePool.pickMessage(
                enabledCategoryIds = prefs.tinyCareCategories,
                recentIds = recentIds
            )

            android.util.Log.d("TinyCare", "Selected message: ${message?.id} '${message?.title}', recentCount=${recentIds.size}")

            if (message != null) {
                TinyCareScheduler.createNotificationChannel(context)
                TinyCareScheduler.postNotification(
                    context = context,
                    notificationId = TinyCareScheduler.NOTIFICATION_ID_CARE,
                    title = message.title,
                    body = message.body
                )
                // Append and trim recent history to strictly 10 items
                prefs.addTinyCareRecentMessageId(message.id)
                android.util.Log.d("TinyCare", "Notification posted. Updated recent: ${prefs.getTinyCareRecentMessageIds()}")
            }
        } else {
            android.util.Log.d("TinyCare", "Delivery skipped due to quiet hours.")
        }

        // Schedule the next rolling reminder
        TinyCareScheduler.scheduleNext(context, forceReschedule = true)
    }
}
