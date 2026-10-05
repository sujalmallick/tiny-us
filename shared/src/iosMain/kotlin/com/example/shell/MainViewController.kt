package com.example.shell

import androidx.compose.ui.window.ComposeUIViewController
import com.example.engine.GameText
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
    return ComposeUIViewController { SharedWorldScreen(onFirstFrame = LaunchDiagnostics::markRunning) }
}

/** Shared text for the engine and other non-Compose code, loaded once (Android does this at app start). */
private fun loadSharedText() {
    if (GameText.isLoaded) return
    LaunchDiagnostics.stage("loading text")
    runBlocking { GameText.load() }
}
