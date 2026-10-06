package com.example.security

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** The activity behind [this] context, for the biometric prompt and window flags. */
tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

/** Android's side of the shared lock: the biometric prompt, FLAG_SECURE and the discreet icon. */
class AndroidLockDevice(private val context: Context) : LockDevice {
    private val activity: FragmentActivity? get() = context.findFragmentActivity()

    override fun biometricsAvailable(): Boolean {
        val a = activity ?: return false
        return BiometricManager.from(a).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /** Whether the phone has its own screen lock, which "Forgot PIN" relies on. */
    override fun hasDeviceCredential(): Boolean {
        val a = activity ?: return false
        return BiometricManager.from(a).canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Shows the system fingerprint/face sheet. With [allowDeviceCredential] the phone's own PIN,
     * pattern or password is accepted too — used for "Forgot PIN".
     */
    override fun promptBiometric(title: String, subtitle: String, negativeText: String, allowDeviceCredential: Boolean, onSuccess: () -> Unit) {
        val a = activity ?: return
        val prompt = BiometricPrompt(a, ContextCompat.getMainExecutor(a), object : BiometricPrompt.AuthenticationCallback() {
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

    override fun privacyChanged(store: AppLockStore) {
        activity?.let { applyWindowPrivacy(it, store) }
    }

    override fun showMessage(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    override val discreetMode: DiscreetSwitch = object : DiscreetSwitch {
        override fun isEnabled() = DiscreetMode.isEnabled(context)
        override fun setEnabled(enabled: Boolean) = DiscreetMode.setEnabled(context, enabled)
    }

    companion object {
        /** FLAG_SECURE hides the Recents thumbnail and blocks screenshots while the lock is on. */
        fun applyWindowPrivacy(activity: Activity, store: AppLockStore) {
            if (store.isEnabled && store.hidePreview) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}
