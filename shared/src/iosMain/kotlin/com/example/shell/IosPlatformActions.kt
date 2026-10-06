package com.example.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.care.IosReminders
import com.example.data.PreferencesManager
import com.example.ui.PlatformActions
import com.example.ui.Reminders

/** iOS's side of [PlatformActions]: Tiny Care reminders (app lock and backup are still to come). */
class IosPlatformActions(private val prefs: PreferencesManager) : PlatformActions {
    @Composable
    override fun rememberReminders(): Reminders = remember { IosReminders(prefs) }
}
