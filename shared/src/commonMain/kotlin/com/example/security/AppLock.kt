package com.example.security

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.AppLockPolicy
import kotlin.time.TimeSource

/**
 * Process-wide lock state. [isLocked] is Compose state, so the app swaps its whole UI for the lock
 * screen (dialogs included, which would otherwise stay visible). The platform tells it when the app
 * starts, leaves and comes back.
 */
object AppLock {
    var isLocked by mutableStateOf(false)
        private set

    /** Monotonic time (not the wall clock) when the app was last left; null until the first time. */
    private var backgroundedAt: TimeSource.Monotonic.ValueTimeMark? = null

    private var coldStartHandled = false

    /** Set just before the app itself opens a system screen (file picker, share sheet). */
    private var ignoreNextBackground = false

    /** The first screen of a process locks; later recreations (rotation) keep the current state. */
    fun onLaunch(store: AppLockStore) {
        if (!coldStartHandled) {
            coldStartHandled = true
            if (store.isEnabled) isLocked = true
        }
    }

    fun expectExternalActivity() {
        ignoreNextBackground = true
    }

    fun onAppBackgrounded() {
        if (ignoreNextBackground) {
            ignoreNextBackground = false
            backgroundedAt = null // returning from our own picker must not trigger a lock
            return
        }
        backgroundedAt = TimeSource.Monotonic.markNow()
    }

    fun onAppForegrounded(store: AppLockStore) {
        val left = backgroundedAt ?: return
        val elapsedMs = left.elapsedNow().inWholeMilliseconds
        // The policy takes timestamps; "now" is the elapsed time after the leaving moment at 0.
        if (AppLockPolicy.shouldLockOnReturn(store.isEnabled, 0L, elapsedMs, store.graceSeconds)) {
            isLocked = true
        }
    }

    fun unlock() {
        isLocked = false
    }
}

/**
 * What the lock needs from the phone: biometrics, the device's own screen lock (for "Forgot PIN"),
 * hiding the app's preview, and the platform's wording. Android and iOS provide their own.
 */
interface LockDevice {
    /** True on iPhone and iPad, where the wording names Face ID and the passcode. */
    val isApple: Boolean get() = false

    fun biometricsAvailable(): Boolean = false

    /** Whether the device has its own screen lock, which "Forgot PIN" relies on. */
    fun hasDeviceCredential(): Boolean = false

    /**
     * Shows the system biometric sheet; with [allowDeviceCredential] the device's own PIN or
     * passcode is accepted too (used for "Forgot PIN").
     */
    fun promptBiometric(title: String, subtitle: String, negativeText: String, allowDeviceCredential: Boolean, onSuccess: () -> Unit) {}

    /** The lock or its preview setting changed: hide or show the app's preview accordingly. */
    fun privacyChanged(store: AppLockStore) {}

    /** A short note, such as "App lock turned off". */
    fun showMessage(text: String) {}

    /** The discreet launcher icon, where the platform has one (Android). */
    val discreetMode: DiscreetSwitch? get() = null
}

/** A setting the platform keeps itself, such as Android's discreet launcher icon. */
interface DiscreetSwitch {
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)
}

val LocalLockDevice = staticCompositionLocalOf<LockDevice> { object : LockDevice {} }
