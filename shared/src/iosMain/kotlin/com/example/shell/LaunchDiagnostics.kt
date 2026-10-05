package com.example.shell

import kotlin.experimental.ExperimentalNativeApi
import platform.Foundation.NSUserDefaults

/**
 * Remembers how far the shared world got while starting, and the last Kotlin error that closed the
 * app, so the next launch can show them instead of closing again. Read from Swift through
 * [lastProblem] and cleared with [clear].
 */
object LaunchDiagnostics {
    private const val STAGE = "tinyus.shared.launchStage"
    private const val CRASH = "tinyus.shared.lastCrash"
    private const val RUNNING = "running"
    private val defaults get() = NSUserDefaults.standardUserDefaults

    @OptIn(ExperimentalNativeApi::class)
    fun install() {
        var previous: ((Throwable) -> Unit)? = null
        previous = setUnhandledExceptionHook { error ->
            val stage = defaults.stringForKey(STAGE) ?: "unknown"
            defaults.setObject("While: $stage\n${error.stackTraceToString()}", CRASH)
            defaults.synchronize()
            previous?.invoke(error) // keep the system's own reporting too
        }
    }

    /** Records the step the shared world is on; [RUNNING] once the first frame is drawn. */
    fun stage(name: String) {
        defaults.setObject(name, STAGE)
        defaults.synchronize()
    }

    fun markRunning() = stage(RUNNING)

    /**
     * What went wrong last time, or null: the saved error, or the step it never finished (a freeze
     * or a crash outside Kotlin).
     */
    fun lastProblem(): String? {
        defaults.stringForKey(CRASH)?.let { return it }
        val stage = defaults.stringForKey(STAGE) ?: return null
        return if (stage == RUNNING) null else "The shared world stopped while: $stage (no Kotlin error was recorded)"
    }

    fun clear() {
        defaults.removeObjectForKey(CRASH)
        defaults.removeObjectForKey(STAGE)
        defaults.synchronize()
    }
}
