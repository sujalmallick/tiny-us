package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * What only the platform app can do, offered to the shared screens. Android provides its own at the
 * root of the app; elsewhere (iOS for now, tests, previews) these do nothing and the settings sheet
 * leaves out the parts that need them.
 */
interface PlatformActions {
    /** Redraws the home-screen widgets after the couple's moments, moods or signals change. */
    fun refreshWidgets() {}

    /** Gentle reminders ("Tiny Care") with their notification permission, or null where there are none. */
    @Composable
    fun rememberReminders(): Reminders? = null

    /** The app lock and discreet-mode settings, or null where the platform has none. */
    val privacySettings: (@Composable () -> Unit)? get() = null

    /** The backup and restore settings, or null where the platform has none. */
    val backupSettings: (@Composable () -> Unit)? get() = null
}

/** The platform's reminder scheduling and its notification permission. */
interface Reminders {
    /** True when the phone lets the app post notifications right now. */
    fun allowed(): Boolean

    /** True when the app must ask before it can post notifications. */
    fun needsPermission(): Boolean

    /** Asks for notification permission; [onResult] gets the answer. */
    fun requestPermission(onResult: (granted: Boolean) -> Unit)

    fun enable()
    fun disable()

    /** Sends one reminder now, to show what they look like. */
    fun sendPreview()
}

val LocalPlatformActions = staticCompositionLocalOf<PlatformActions> { object : PlatformActions {} }
