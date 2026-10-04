package com.example

import android.app.Application
import com.example.engine.GameText

/** Sets up app-wide services before any screen opens. */
class TinyUsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GameText.init(this)
    }
}
