package com.example.shell

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import com.example.ui.theme.MyApplicationTheme as TinyUsTheme
import androidx.compose.ui.window.ComposeUIViewController
import com.example.engine.GameText
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import com.example.engine.WorldViewport
import kotlin.math.floor
import platform.UIKit.UIScreen
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
    limitWorldZoom()
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

/**
 * The whole shared app for SwiftUI: `MainViewControllerKt.SharedMainViewController()`, the same main
 * screen, menus and dialogs as Android (plan 08, S5).
 */
fun SharedMainViewController(): UIViewController {
    LaunchDiagnostics.install()
    loadSharedText()
    limitWorldZoom()
    LaunchDiagnostics.stage("starting the app")
    return ComposeUIViewController {
        val platform = remember { IosMainPlatform() }
        TinyUsTheme {
            com.example.ui.MainScreen(platform = platform)
        }
        LaunchedEffect(Unit) {
            withFrameNanos { }
            LaunchDiagnostics.markRunning()
        }
    }
}

/**
 * At most [MAX_GAME_PIXEL_POINTS] points per game pixel: about a phone's size, so the couple on a big
 * iPad is drawn as on an iPhone and the scene shows more around them (iPhones already fit under it).
 */
private fun limitWorldZoom() {
    WorldViewport.maxZoom = floor(MAX_GAME_PIXEL_POINTS * UIScreen.mainScreen.scale).toInt().coerceAtLeast(1)
}

private const val MAX_GAME_PIXEL_POINTS = 4.0

/** Shared text for the engine and other non-Compose code, loaded once (Android does this at app start). */
private fun loadSharedText() {
    if (GameText.isLoaded) return
    LaunchDiagnostics.stage("loading text")
    runBlocking { GameText.load() }
}
