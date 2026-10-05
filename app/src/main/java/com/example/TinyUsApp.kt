package com.example

import android.app.Application
import com.example.engine.GameText
import kotlinx.coroutines.runBlocking

/** Sets up app-wide services before any screen opens. */
open class TinyUsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Shared text for the engine and other non-Compose code; one quick read of the resource file.
        runBlocking { GameText.load() }
    }
}
