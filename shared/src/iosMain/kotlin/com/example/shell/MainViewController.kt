package com.example.shell

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.example.engine.GameText
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import platform.UIKit.UIViewController

/** Entry point for SwiftUI: `MainViewControllerKt.SharedComposeViewController()` hosts shared Compose UI. */
fun SharedComposeViewController(): UIViewController {
    loadSharedText()
    return ComposeUIViewController { SharedComposeProbe() }
}

/** The shared pixel world for SwiftUI: `MainViewControllerKt.SharedWorldViewController()`. */
fun SharedWorldViewController(): UIViewController {
    LaunchDiagnostics.install()
    loadSharedText()
    LaunchDiagnostics.stage("starting the world")
    return ComposeUIViewController {
        SharedWorldScreen(
            audio = SharedSound.audio,
            startWeather = remember { WeatherMemory.startWeather() },
            onWeather = WeatherMemory::save,
            onFirstFrame = LaunchDiagnostics::markRunning
        )
        // Remember the weather while the app is open, so a quick return finds the same sky.
        LaunchedEffect(Unit) {
            while (true) {
                delay(60_000)
                SharedWorldBridge.weather?.let(WeatherMemory::save)
            }
        }
    }
}

/** Shared text for the engine and other non-Compose code, loaded once (Android does this at app start). */
private fun loadSharedText() {
    if (GameText.isLoaded) return
    LaunchDiagnostics.stage("loading text")
    runBlocking { GameText.load() }
}
