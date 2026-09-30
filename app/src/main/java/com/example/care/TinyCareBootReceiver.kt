package com.example.care

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.PreferencesManager

class TinyCareBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = PreferencesManager(context)
            if (prefs.tinyCareEnabled) {
                TinyCareScheduler.scheduleNext(context, forceReschedule = true)
            }
        }
    }
}
