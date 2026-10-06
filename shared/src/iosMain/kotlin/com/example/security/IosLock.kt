@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.security

import com.example.care.IosNotifications
import com.example.data.IosUserDefaultsStorage
import com.example.data.PreferencesManager
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSUserDefaults
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.Foundation.NSError
// A star import: the alternate-icon calls are an Objective-C category on UIApplication.
import platform.UIKit.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * The app lock on iOS: its settings in their own defaults suite, locking when the app starts and
 * when it comes back after the grace period, and (for Swift, `IosLock.shared`) whether the app
 * switcher should see a plain cover.
 */
object IosLock {
    val store: AppLockStore by lazy { AppLockStore(IosUserDefaultsStorage(NSUserDefaults(suiteName = "tinyus.lock"))) }

    private var installed = false

    /** Locks a fresh launch and watches the app leaving and coming back. Safe to call more than once. */
    fun install() {
        AppLock.onLaunch(store)
        if (installed) return
        installed = true
        val center = NSNotificationCenter.defaultCenter
        val queue = NSOperationQueue.mainQueue
        center.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, queue) { _ -> AppLock.onAppBackgrounded() }
        center.addObserverForName(UIApplicationWillEnterForegroundNotification, null, queue) { _ -> AppLock.onAppForegrounded(store) }
    }

    /** True when the app switcher should show a cover instead of the app. */
    fun shouldHidePreview(): Boolean = store.isEnabled && store.hidePreview

    /** Reminders are written ahead of time on iOS: plan them again with or without their text. */
    fun replanReminders() = IosNotifications.planAll(PreferencesManager(IosUserDefaultsStorage.defaultStorage()))
}

/**
 * The discreet icon on iOS: the plain notebook ("AppIconDiscreet" in the asset catalog) as the
 * alternate app icon, which is also how it is remembered. Unlike Android, iOS shows its own short
 * note when the icon changes, and the name under the icon stays.
 */
object IosDiscreetIcon : DiscreetSwitch {
    private const val ICON = "AppIconDiscreet"

    fun supported(): Boolean = UIApplication.sharedApplication.supportsAlternateIcons

    override fun isEnabled(): Boolean = UIApplication.sharedApplication.alternateIconName == ICON

    override fun setEnabled(enabled: Boolean) {
        UIApplication.sharedApplication.setAlternateIconName(if (enabled) ICON else null) { _: NSError? ->
            dispatch_async(dispatch_get_main_queue()) { IosLock.replanReminders() }
        }
    }
}

/** iOS's side of the shared lock: Face ID or Touch ID, and the device passcode for "Forgot PIN". */
class IosLockDevice : LockDevice {
    override val isApple: Boolean get() = true

    override val discreetMode: DiscreetSwitch? get() = if (IosDiscreetIcon.supported()) IosDiscreetIcon else null

    override fun privacyChanged(store: AppLockStore) = IosLock.replanReminders()

    override fun biometricsAvailable(): Boolean =
        LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error = null)

    override fun hasDeviceCredential(): Boolean =
        LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error = null)

    override fun promptBiometric(title: String, subtitle: String, negativeText: String, allowDeviceCredential: Boolean, onSuccess: () -> Unit) {
        val context = LAContext()
        // The button Face ID shows after a failed try: "Use PIN" falls back to the app's own PIN pad.
        if (!allowDeviceCredential) context.setLocalizedFallbackTitle(negativeText)
        val policy = if (allowDeviceCredential) LAPolicyDeviceOwnerAuthentication else LAPolicyDeviceOwnerAuthenticationWithBiometrics
        context.evaluatePolicy(policy, localizedReason = subtitle.ifBlank { title }) { success, _ ->
            if (success) dispatch_async(dispatch_get_main_queue()) { onSuccess() }
        }
    }
}
