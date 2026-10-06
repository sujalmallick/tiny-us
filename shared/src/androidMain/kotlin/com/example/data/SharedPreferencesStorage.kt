package com.example.data

import android.content.SharedPreferences

/**
 * Android SharedPreferences adapter implementing the shared KeyValueStorage contract.
 */
class SharedPreferencesStorage(private val prefs: SharedPreferences) : KeyValueStorage {

    override fun getString(key: String, defaultValue: String?): String? =
        prefs.getString(key, defaultValue)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        prefs.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        prefs.getInt(key, defaultValue)

    override fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        prefs.getLong(key, defaultValue)

    override fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun getFloat(key: String, defaultValue: Float): Float =
        prefs.getFloat(key, defaultValue)

    override fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    override fun getStringSet(key: String, defaultValue: Set<String>): Set<String> =
        prefs.getStringSet(key, defaultValue) ?: defaultValue

    override fun putStringSet(key: String, values: Set<String>) {
        prefs.edit().putStringSet(key, values).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}
