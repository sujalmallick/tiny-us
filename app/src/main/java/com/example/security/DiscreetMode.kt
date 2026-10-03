package com.example.security

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Discreet mode swaps the launcher entry for a neutral "Journal" icon and makes reminder
 * notifications generic. The enabled launcher alias is the single source of truth.
 */
object DiscreetMode {
    // Declared in AndroidManifest.xml as activity-aliases of MainActivity.
    private const val DEFAULT_ALIAS = "com.example.LauncherDefault"
    private const val DISCREET_ALIAS = "com.example.LauncherDiscreet"

    fun isEnabled(context: Context): Boolean {
        val state = context.packageManager.getComponentEnabledSetting(ComponentName(context.packageName, DISCREET_ALIAS))
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    /**
     * Some launchers take a few seconds to refresh and may drop a home-screen shortcut; the app
     * drawer entry always updates.
     */
    fun setEnabled(context: Context, enabled: Boolean) {
        val pm = context.packageManager
        // Enable the new entry before disabling the old one so there is never zero launchers.
        val (on, off) = if (enabled) DISCREET_ALIAS to DEFAULT_ALIAS else DEFAULT_ALIAS to DISCREET_ALIAS
        pm.setComponentEnabledSetting(ComponentName(context.packageName, on), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(ComponentName(context.packageName, off), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
    }
}
