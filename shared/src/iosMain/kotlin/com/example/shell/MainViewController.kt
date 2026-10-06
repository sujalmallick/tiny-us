package com.example.shell

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.window.ComposeUIViewController
import com.example.engine.GameText
import com.example.engine.WorldViewport
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.floor
import kotlinx.coroutines.runBlocking
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController

/**
 * The whole app for SwiftUI: `MainViewControllerKt.SharedMainViewController()`, the same main screen,
 * menus and dialogs as Android (plan 08, S5).
 */
fun SharedMainViewController(): UIViewController {
    LaunchDiagnostics.install()
    loadSharedText()
    limitWorldZoom()
    LaunchDiagnostics.stage("starting the app")
    return ComposeUIViewController {
        val platform = remember { IosMainPlatform() }
        MyApplicationTheme {
            MainScreen(platform = platform)
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
