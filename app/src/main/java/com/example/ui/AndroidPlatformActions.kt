package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.care.TinyCareScheduler
import com.example.security.AndroidLockDevice
import com.example.security.AppLockStore
import com.example.security.LocalLockDevice
import com.example.ui.theme.MyApplicationTheme
import com.example.widget.TinyUsWidgetProvider

/** Android's side of [PlatformActions]: the home-screen widget, Tiny Care reminders, app lock and backup. */
class AndroidPlatformActions(private val context: Context) : PlatformActions {
    override fun refreshWidgets() = TinyUsWidgetProvider.updateAllWidgets(context)

    @Composable
    override fun rememberReminders(): Reminders {
        val reminders = remember { AndroidReminders(context) }
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            reminders.onPermissionResult(granted)
        }
        reminders.askForPermission = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        return reminders
    }

    override val privacySettings: (@Composable () -> Unit) = { PrivacyLockSettings(remember { AppLockStore(context) }) }

    override val backupSettings: (@Composable () -> Unit) = { BackupRestoreSettings(rememberAndroidBackupFiles()) }
}

/** Tiny Care reminders through [TinyCareScheduler], asking for POST_NOTIFICATIONS on Android 13+. */
private class AndroidReminders(private val context: Context) : Reminders {
    var askForPermission: () -> Unit = {}
    private var pending: ((Boolean) -> Unit)? = null

    override fun allowed(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun needsPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    override fun requestPermission(onResult: (granted: Boolean) -> Unit) {
        pending = onResult
        askForPermission()
    }

    fun onPermissionResult(granted: Boolean) {
        pending?.invoke(granted)
        pending = null
    }

    override fun enable() = TinyCareScheduler.enable(context)
    override fun disable() = TinyCareScheduler.disable(context)
    override fun sendPreview() = TinyCareScheduler.sendTestNotification(context)
}

/** The app's theme with Android's [PlatformActions] for every shared screen inside it. */
@Composable
fun AndroidAppRoot(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val actions = remember { AndroidPlatformActions(context.applicationContext) }
    val lockDevice = remember(context) { AndroidLockDevice(context) }
    MyApplicationTheme {
        CompositionLocalProvider(LocalPlatformActions provides actions, LocalLockDevice provides lockDevice, content = content)
    }
}
