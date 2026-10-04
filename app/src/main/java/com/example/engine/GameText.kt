package com.example.engine

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes

/**
 * Translatable text for the scene engine, which has no Android context of its own: the app hands
 * over its resources at start-up ([com.example.TinyUsApp]), and the engine looks up its messages
 * by string resource, with names and other values passed as format arguments.
 */
object GameText {
    @Volatile
    private var resources: Resources? = null

    fun init(context: Context) {
        resources = context.applicationContext.resources
    }

    /** The string [id], formatted with [args] when there are any. Empty before [init]. */
    fun get(@StringRes id: Int, vararg args: Any?): String {
        val res = resources ?: return ""
        return if (args.isEmpty()) res.getString(id) else res.getString(id, *args)
    }
}
