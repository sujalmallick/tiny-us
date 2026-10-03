package com.example.security

import android.app.Activity
import android.os.SystemClock
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.AppLockPolicy

/**
 * Process-wide lock state. [isLocked] is Compose state, so the activity swaps the whole UI for the
 * lock screen (dialogs included — they live in their own windows and would otherwise stay visible).
 */
object AppLock {
    var isLocked by mutableStateOf(false)
        private set

    /** Uptime (not wall clock) when the app was last left; null until the first background. */
    private var backgroundedAtUptime: Long? = null

    private var coldStartHandled = false

    fun onActivityCreated(activity: Activity, store: AppLockStore) {
        // Lock on the first activity of a process; later recreations (rotation) keep the current state.
        if (!coldStartHandled) {
            coldStartHandled = true
            if (store.isEnabled) isLocked = true
        }
        applyWindowPrivacy(activity, store)
    }

    /** Set just before the app itself opens a system screen (file picker, share sheet). */
    private var ignoreNextBackground = false

    fun expectExternalActivity() {
        ignoreNextBackground = true
    }

    fun onAppBackgrounded() {
        if (ignoreNextBackground) {
            ignoreNextBackground = false
            backgroundedAtUptime = null // returning from our own picker must not trigger a lock
            return
        }
        backgroundedAtUptime = SystemClock.elapsedRealtime()
    }

    fun onAppForegrounded(store: AppLockStore) {
        val left = backgroundedAtUptime ?: return
        if (AppLockPolicy.shouldLockOnReturn(store.isEnabled, left, SystemClock.elapsedRealtime(), store.graceSeconds)) {
            isLocked = true
        }
    }

    fun unlock() {
        isLocked = false
    }

    /** FLAG_SECURE hides the Recents thumbnail and blocks screenshots while the lock is on. */
    fun applyWindowPrivacy(activity: Activity, store: AppLockStore) {
        if (store.isEnabled && store.hidePreview) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    fun canUseBiometrics(activity: Activity): Boolean =
        BiometricManager.from(activity).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    /** Whether the phone has its own screen lock, which "Forgot PIN" relies on. */
    fun hasDeviceCredential(activity: Activity): Boolean =
        BiometricManager.from(activity).canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Shows the system fingerprint/face sheet. With [allowDeviceCredential] the phone's own PIN,
     * pattern or password is accepted too — used for "Forgot PIN".
     */
    fun promptBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeText: String,
        allowDeviceCredential: Boolean,
        onSuccess: () -> Unit
    ) {
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .apply {
                if (allowDeviceCredential) {
                    setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                } else {
                    setAllowedAuthenticators(BIOMETRIC_WEAK)
                    setNegativeButtonText(negativeText)
                }
            }
            .build()
        prompt.authenticate(info)
    }
}
