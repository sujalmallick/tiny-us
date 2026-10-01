package com.example.data

import platform.Foundation.NSUserDefaults

/**
 * Apple iOS NSUserDefaults adapter implementing the shared KeyValueStorage contract.
 */
class IosUserDefaultsStorage(
    private val defaults: NSUserDefaults
) : KeyValueStorage {

    constructor() : this(NSUserDefaults.standardUserDefaults)

    companion object {
        fun defaultStorage(): IosUserDefaultsStorage = IosUserDefaultsStorage(NSUserDefaults.standardUserDefaults)
    }

    override fun getString(key: String, defaultValue: String?): String? =
        defaults.stringForKey(key) ?: defaultValue

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        if (defaults.objectForKey(key) != null) defaults.integerForKey(key).toInt() else defaultValue

    override fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), forKey = key)
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        if (defaults.objectForKey(key) != null) defaults.integerForKey(key) else defaultValue

    override fun putLong(key: String, value: Long) {
        defaults.setInteger(value, forKey = key)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defaultValue: Set<String>): Set<String> {
        val array = defaults.stringArrayForKey(key) ?: return defaultValue
        return (array as List<String>).toSet()
    }

    override fun putStringSet(key: String, values: Set<String>) {
        defaults.setObject(values.toList(), forKey = key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }

    override fun clear() {
        val dict = defaults.dictionaryRepresentation()
        for (key in dict.keys) {
            val k = key as? String ?: continue
            defaults.removeObjectForKey(k)
        }
    }
}
