package com.example.care

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesManager
import java.util.Calendar
import kotlin.random.Random

object TinyCareScheduler {

    const val CHANNEL_ID = "tiny_care_channel"
    const val ACTION_ALARM = "com.tinyus.app.ACTION_TINY_CARE_ALARM"

    private const val REQUEST_CODE_ALARM = 7701
    const val NOTIFICATION_ID_CARE = 7710
    const val NOTIFICATION_ID_TEST = 7711

    // Notification channel setup
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Tiny Care"
            val descriptionText = "Gentle, wholesome offline reminders"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    // Enable Tiny Care, schedule first reminder, and activate BootReceiver
    fun enable(context: Context) {
        createNotificationChannel(context)
        val prefs = PreferencesManager(context)
        prefs.tinyCareEnabled = true

        setBootReceiverEnabled(context, true)
        scheduleNext(context, forceReschedule = true, isFirstReminder = true)
    }

    // Disable Tiny Care, cancel pending alarms, and deactivate BootReceiver
    fun disable(context: Context) {
        val prefs = PreferencesManager(context)
        prefs.tinyCareEnabled = false
        prefs.tinyCareNextTriggerMillis = 0L

        cancelAlarm(context)
        setBootReceiverEnabled(context, false)
    }

    // Schedule the next rolling notification
    fun scheduleNext(
        context: Context,
        forceReschedule: Boolean = false,
        isFirstReminder: Boolean = false
    ) {
        val prefs = PreferencesManager(context)
        if (!prefs.tinyCareEnabled) {
            cancelAlarm(context)
            return
        }

        val now = System.currentTimeMillis()
        // If an alarm is already scheduled in the future and we are not forcing a reschedule, preserve it!
        if (!forceReschedule && prefs.tinyCareNextTriggerMillis > now) {
            val minsRemaining = (prefs.tinyCareNextTriggerMillis - now) / 60000
            android.util.Log.d("TinyCare", "Existing alarm preserved at ${prefs.tinyCareNextTriggerMillis} (in $minsRemaining mins)")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerMillis = computeNextTriggerTime(
            quietStartHour = prefs.tinyCareQuietStartHour,
            quietEndHour = prefs.tinyCareQuietEndHour,
            currentTimeMillis = now,
            isFirstReminder = isFirstReminder
        )

        prefs.tinyCareNextTriggerMillis = triggerMillis
        val pendingIntent = getAlarmPendingIntent(context)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
            val delayMins = (triggerMillis - now) / 60000
            android.util.Log.d("TinyCare", "Alarm scheduled for timestamp $triggerMillis (in $delayMins mins)")
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    // Cancel pending alarm
    fun cancelAlarm(context: Context) {
        val prefs = PreferencesManager(context)
        prefs.tinyCareNextTriggerMillis = 0L
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = getAlarmPendingIntent(context)
        alarmManager.cancel(pendingIntent)
    }

    private fun getAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, TinyCareReceiver::class.java).apply {
            action = ACTION_ALARM
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Dynamic boot receiver toggle via PackageManager
    private fun setBootReceiverEnabled(context: Context, enabled: Boolean) {
        val receiver = ComponentName(context, TinyCareBootReceiver::class.java)
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        try {
            context.packageManager.setComponentEnabledSetting(
                receiver,
                state,
                PackageManager.DONT_KILL_APP
            )
        } catch (_: Exception) {
        }
    }

    /**
     * Computes the next delivery timestamp.
     * Spaced 90 to 150 minutes (1.5 to 2.5 hours) apart for a wholesome daily cadence.
     * When enabling for the first time, schedules a prompt first check-in within 15 to 30 minutes.
     * If the target lands inside quiet hours (default 11 PM - 7 AM), it gracefully pushes to tomorrow morning.
     */
    fun computeNextTriggerTime(
        quietStartHour: Int = 23,
        quietEndHour: Int = 7,
        currentTimeMillis: Long = System.currentTimeMillis(),
        isFirstReminder: Boolean = false
    ): Long {
        val now = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }

        // Interval: 15 to 30 mins for first check-in; 90 to 150 mins (1.5 to 2.5 hrs) for ongoing care
        val intervalMinutes = if (isFirstReminder) {
            Random.nextInt(15, 31)
        } else {
            Random.nextInt(90, 151)
        }
        val target = (now.clone() as Calendar).apply {
            add(Calendar.MINUTE, intervalMinutes)
        }

        val targetHour = target.get(Calendar.HOUR_OF_DAY)
        val inQuietHours = isTimeInQuietWindow(targetHour, quietStartHour, quietEndHour)

        return if (inQuietHours) {
            // Jump to morning at quietEndHour + random morning jitter (15 to 45 mins)
            val morning = (now.clone() as Calendar).apply {
                if (get(Calendar.HOUR_OF_DAY) >= quietEndHour) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                set(Calendar.HOUR_OF_DAY, quietEndHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, Random.nextInt(15, 46))
            }
            morning.timeInMillis
        } else {
            target.timeInMillis
        }
    }

    fun isTimeInQuietWindow(hour: Int, startHour: Int, endHour: Int): Boolean {
        return if (startHour > endHour) {
            // e.g. 23 (11 PM) to 7 (7 AM)
            hour >= startHour || hour < endHour
        } else {
            // e.g. 1 AM to 8 AM
            hour in startHour until endHour
        }
    }

    // Sends an immediate test notification without altering recent-history or rolling alarm schedule
    fun sendTestNotification(context: Context) {
        createNotificationChannel(context)
        val prefs = PreferencesManager(context)

        // Select a sample message from enabled categories or fallback to dedicated test message
        val sample = TinyCareMessagePool.pickMessage(prefs.tinyCareCategories, emptyList())
            ?: TinyCareMessagePool.testMessage

        postNotification(
            context = context,
            notificationId = NOTIFICATION_ID_TEST,
            title = sample.title,
            body = sample.body
        )
    }

    // Builds and displays the notification
    fun postNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, notification)
        } catch (_: SecurityException) {
        }
    }
}
