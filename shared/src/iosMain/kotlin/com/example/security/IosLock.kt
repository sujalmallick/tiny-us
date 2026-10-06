package com.example.security

import com.example.data.IosUserDefaultsStorage
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSUserDefaults
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
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
}

/** iOS's side of the shared lock: Face ID or Touch ID, and the device passcode for "Forgot PIN". */
class IosLockDevice : LockDevice {
    override val isApple: Boolean get() = true

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
