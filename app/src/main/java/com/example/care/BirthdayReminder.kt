package com.example.care

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.BirthdayStore
import com.example.data.Birthdays
import com.example.data.CoupleDates
import com.example.data.SharedPreferencesStorage
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import com.example.resources.*

/**
 * The opt-in morning reminder on a birthday (plan 09, A): "Something is waiting for you in Tiny
 * Us today." It never says whose birthday it is, so the surprise holds. One inexact alarm at
 * 9:00 on the next birthday, set again after it fires and after a reboot.
 */
object BirthdayReminder {
    const val ACTION = "com.tinyus.app.ACTION_BIRTHDAY_MORNING"
    private const val REQUEST_CODE = 7720
    private const val NOTIFICATION_ID = 7721
    private const val HOUR = 9

    private fun store(context: Context) =
        BirthdayStore(SharedPreferencesStorage(context.getSharedPreferences("tiny_us_prefs", Context.MODE_PRIVATE)))

    /** When the next reminder should go off, or null when it's off or no birthday is set. */
    @OptIn(kotlin.time.ExperimentalTime::class)
    fun nextTriggerMillis(store: BirthdayStore, nowMillis: Long = System.currentTimeMillis()): Long? {
        if (!store.morningReminderEnabled) return null
        val zone = TimeZone.currentSystemDefault()
        val today = CoupleDates.today()
        return store.birthdays().values.filterNotNull()
            .flatMap { b -> listOf(Birthdays.next(b, today), Birthdays.next(b, today.plusYear())) }
            .map { day -> LocalDateTime(day.year, day.month, day.day, HOUR, 0).toInstant(zone).toEpochMilliseconds() }
            .filter { it > nowMillis }
            .minOrNull()
    }

    private fun kotlinx.datetime.LocalDate.plusYear() = kotlinx.datetime.LocalDate(year + 1, month, if (month.ordinal == 1 && day == 29) 28 else day)

    /** Sets (or clears) the alarm for the next birthday morning. Safe to call often. */
    fun schedule(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = pendingIntent(context)
        val trigger = nextTriggerMillis(store(context))
        if (trigger == null) {
            alarms.cancel(pending)
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
            } else {
                alarms.set(AlarmManager.RTC_WAKEUP, trigger, pending)
            }
        } catch (_: SecurityException) {
        }
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, BirthdayReminderReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    internal fun fire(context: Context) {
        if (store(context).morningReminderEnabled) {
            TinyCareScheduler.createNotificationChannel(context)
            TinyCareScheduler.postNotification(
                context = context,
                notificationId = NOTIFICATION_ID,
                title = com.example.engine.GameText.get(Res.string.ui_tiny_us),
                body = com.example.engine.GameText.get(Res.string.bday_morning_reminder)
            )
        }
        schedule(context)
    }
}

/** Posts the birthday morning reminder, and sets the next one after a reboot. */
class BirthdayReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            BirthdayReminder.ACTION -> BirthdayReminder.fire(context)
            Intent.ACTION_BOOT_COMPLETED -> BirthdayReminder.schedule(context)
        }
    }
}
