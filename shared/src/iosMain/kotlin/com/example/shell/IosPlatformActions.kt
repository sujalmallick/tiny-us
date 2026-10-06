package com.example.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.care.IosReminders
import com.example.data.IosUserDefaultsStorage
import com.example.data.PreferencesManager
import com.example.security.IosLock
import com.example.ui.BackupRestoreSettings
import com.example.ui.IosBackupFiles
import com.example.ui.PlatformActions
import com.example.ui.PrivacyLockSettings
import com.example.ui.Reminders

/** iOS's side of [PlatformActions]: Tiny Care reminders, the app lock, and backup and restore. */
class IosPlatformActions(private val prefs: PreferencesManager) : PlatformActions {
    @Composable
    override fun rememberReminders(): Reminders = remember { IosReminders(prefs) }

    override val privacySettings: (@Composable () -> Unit) = { PrivacyLockSettings(IosLock.store) }

    override val backupSettings: (@Composable () -> Unit) = {
        BackupRestoreSettings(remember { IosBackupFiles(IosUserDefaultsStorage.defaultStorage()) })
    }
}
