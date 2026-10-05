package com.example.shell

import androidx.compose.ui.window.ComposeUIViewController
import com.example.engine.GameText
import kotlinx.coroutines.runBlocking
import platform.UIKit.UIViewController

/** Entry point for SwiftUI: `MainViewControllerKt.SharedComposeViewController()` hosts shared Compose UI. */
fun SharedComposeViewController(): UIViewController {
    // Shared text for the engine and other non-Compose code, loaded once (Android does this at app start).
    if (!GameText.isLoaded) runBlocking { GameText.load() }
    return ComposeUIViewController { SharedComposeProbe() }
}

/** The shared pixel world for SwiftUI: `MainViewControllerKt.SharedWorldViewController()`. */
fun SharedWorldViewController(): UIViewController {
    if (!GameText.isLoaded) runBlocking { GameText.load() }
    return ComposeUIViewController { SharedWorldScreen() }
}
