package com.example.care

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.BirthdayStore
import com.example.data.SharedPreferencesStorage
import com.example.resources.*

/**
 * The opt-in morning reminders: on a birthday (plan 09, A) "Something is waiting for you in Tiny
 * Us today", never saying whose; on the anniversary and month-iversary (plan 09, C) a line of its
 * own. One inexact alarm at 9:00 on the soonest, set again after it fires and after a reboot.
 */
object BirthdayReminder {
    const val ACTION = "com.tinyus.app.ACTION_BIRTHDAY_MORNING"
    private const val REQUEST_CODE = 7720
    private const val NOTIFICATION_ID = 7721

    private fun storage(context: Context) =
        SharedPreferencesStorage(context.getSharedPreferences("tiny_us_prefs", Context.MODE_PRIVATE))

    private fun store(context: Context) = BirthdayStore(storage(context))

    /** The day they got together, as saved. */
    private fun start(context: Context): kotlinx.datetime.LocalDate? =
        runCatching { kotlinx.datetime.LocalDate.parse(com.example.data.PreferencesManager(storage(context)).anniversaryDate) }.getOrNull()

    /** When the next birthday reminder should go off, or null when it's off or no birthday is set. */
    fun nextTriggerMillis(store: BirthdayStore, nowMillis: Long = System.currentTimeMillis()): Long? =
        BirthdayMorning.nextMillis(store, nowMillis)

    private fun nextTrigger(context: Context): Long? =
        CoupleMornings.nextMillis(store(context), com.example.data.CoupleLifeStore(storage(context)), start(context), System.currentTimeMillis())

    /** Sets (or clears) the alarm for the next birthday morning. Safe to call often. */
    fun schedule(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = pendingIntent(context)
        val trigger = nextTrigger(context)
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
        val message = CoupleMornings.messageFor(
            com.example.data.CoupleDates.today(), store(context), com.example.data.CoupleLifeStore(storage(context)), start(context)
        )
        if (message != null) {
            TinyCareScheduler.createNotificationChannel(context)
            TinyCareScheduler.postNotification(
                context = context,
                notificationId = NOTIFICATION_ID,
                title = com.example.engine.GameText.get(Res.string.ui_tiny_us),
                body = com.example.engine.GameText.get(message)
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
