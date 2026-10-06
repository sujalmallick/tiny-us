@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.data

import kotlinx.cinterop.toKString
import platform.Foundation.NSArgumentDomain
import platform.Foundation.NSGlobalDomain
import platform.Foundation.NSNumber
import platform.Foundation.NSRegistrationDomain
import platform.Foundation.NSUserDefaults

/**
 * Apple iOS NSUserDefaults adapter implementing the shared KeyValueStorage contract.
 */
class IosUserDefaultsStorage(
    private val defaults: NSUserDefaults
) : KeyValueStorage {

    constructor() : this(NSUserDefaults.standardUserDefaults)

    companion object {
        /** Must match the App Group in iosApp.entitlements and TinyUsWidget.entitlements. */
        const val APP_GROUP_SUITE = "group.com.example.tinyus.shared"

        /** App Group defaults shared with the widget. Without the entitlement (unsigned sideloads) iOS keeps this suite app-local. */
        fun defaultStorage(): IosUserDefaultsStorage =
            IosUserDefaultsStorage(NSUserDefaults(suiteName = APP_GROUP_SUITE))
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

    override fun getFloat(key: String, defaultValue: Float): Float =
        if (defaults.objectForKey(key) != null) defaults.floatForKey(key) else defaultValue

    override fun putFloat(key: String, value: Float) {
        defaults.setFloat(value, forKey = key)
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

    /** The keys this app saved here (not the system's global or registered defaults). */
    private fun ownKeys(): List<String> {
        val system = NSUserDefaults.standardUserDefaults
        val notOurs = listOfNotNull(
            system.persistentDomainForName(NSGlobalDomain),
            system.volatileDomainForName(NSRegistrationDomain),
            system.volatileDomainForName(NSArgumentDomain)
        ).flatMap { it.keys }.mapNotNull { it as? String }.toSet()
        return defaults.dictionaryRepresentation().keys.mapNotNull { it as? String }.filter { it !in notOurs }
    }

    /**
     * Every saved value with its type, for a backup: String, Int, Long, Float, Boolean or
     * Set<String>. iOS keeps whole numbers without their size, so [longKeys] are read as Long.
     */
    fun snapshot(longKeys: Set<String> = emptySet()): Map<String, Any> {
        val out = LinkedHashMap<String, Any>()
        ownKeys().forEach { key ->
            val value: Any = when (val v = defaults.objectForKey(key)) {
                is String -> v
                is Boolean -> v
                is Int -> if (key in longKeys) v.toLong() else v
                is Long -> if (key in longKeys) v else if (v in Int.MIN_VALUE..Int.MAX_VALUE) v.toInt() else v
                is Double -> v.toFloat()
                is Float -> v
                is List<*> -> v.filterIsInstance<String>().toSet()
                is NSNumber -> when (v.objCType?.toKString()) {
                    "c", "B" -> v.boolValue
                    "f", "d" -> v.floatValue
                    else -> v.longLongValue.let { n -> if (key in longKeys || n !in Int.MIN_VALUE..Int.MAX_VALUE) n else n.toInt() }
                }
                else -> null
            } ?: return@forEach
            out[key] = value
        }
        return out
    }

    /** Replaces everything this app saved here with [values] (from a backup). */
    fun replaceAll(values: Map<String, Any>) {
        ownKeys().forEach { defaults.removeObjectForKey(it) }
        values.forEach { (key, value) ->
            @Suppress("UNCHECKED_CAST")
            when (value) {
                is String -> putString(key, value)
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is Set<*> -> putStringSet(key, value as Set<String>)
            }
        }
    }

    override fun clear() {
        val dict = defaults.dictionaryRepresentation()
        for (key in dict.keys) {
            val k = key as? String ?: continue
            defaults.removeObjectForKey(k)
        }
    }
}
