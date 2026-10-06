package com.example.data

/**
 * Platform-agnostic key-value storage abstraction.
 * Enables persistence across Android SharedPreferences, iOS NSUserDefaults, and in-memory test mocks.
 */
interface KeyValueStorage {
    fun getString(key: String, defaultValue: String? = null): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, defaultValue: Long = 0L): Long
    fun putLong(key: String, value: Long)
    fun getFloat(key: String, defaultValue: Float = 0f): Float
    fun putFloat(key: String, value: Float)
    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String>
    fun putStringSet(key: String, values: Set<String>)
    fun remove(key: String)
    fun clear()
}

/**
 * In-memory implementation of KeyValueStorage, ideal for unit testing and headless environments.
 */
class InMemoryKeyValueStorage : KeyValueStorage {
    private val data = mutableMapOf<String, Any>()

    override fun getString(key: String, defaultValue: String?): String? =
        (data[key] as? String) ?: defaultValue

    override fun putString(key: String, value: String) {
        data[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        (data[key] as? Boolean) ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        data[key] = value
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        (data[key] as? Int) ?: defaultValue

    override fun putInt(key: String, value: Int) {
        data[key] = value
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        (data[key] as? Long) ?: defaultValue

    override fun putLong(key: String, value: Long) {
        data[key] = value
    }

    override fun getFloat(key: String, defaultValue: Float): Float =
        (data[key] as? Float) ?: defaultValue

    override fun putFloat(key: String, value: Float) {
        data[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defaultValue: Set<String>): Set<String> =
        (data[key] as? Set<String>) ?: defaultValue

    override fun putStringSet(key: String, values: Set<String>) {
        data[key] = values.toSet()
    }

    override fun remove(key: String) {
        data.remove(key)
    }

    override fun clear() {
        data.clear()
    }
}
