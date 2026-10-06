package com.example.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * What only the platform app can do, offered to the shared screens. Android provides its own at the
 * root of the app; elsewhere (iOS for now, tests, previews) these do nothing.
 */
interface PlatformActions {
    /** Redraws the home-screen widgets after the couple's moments, moods or signals change. */
    fun refreshWidgets() {}
}

val LocalPlatformActions = staticCompositionLocalOf<PlatformActions> { object : PlatformActions {} }
