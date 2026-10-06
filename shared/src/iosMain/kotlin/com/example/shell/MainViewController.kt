package com.example.shell

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.window.ComposeUIViewController
import com.example.engine.GameText
import com.example.engine.WorldViewport
import com.example.security.AppLock
import com.example.security.IosLock
import com.example.security.IosLockDevice
import com.example.security.LocalLockDevice
import com.example.ui.AppLockScreen
import com.example.ui.LocalPlatformActions
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
    IosLock.install()
    LaunchDiagnostics.stage("starting the app")
    return ComposeUIViewController {
        val platform = remember { IosMainPlatform() }
        val actions = remember { IosPlatformActions(platform.prefs) }
        val lockDevice = remember { IosLockDevice() }
        MyApplicationTheme {
            CompositionLocalProvider(LocalPlatformActions provides actions, LocalLockDevice provides lockDevice) {
                // While locked, the lock screen replaces the whole app (dialogs included).
                if (AppLock.isLocked) AppLockScreen(IosLock.store) else MainScreen(platform = platform)
            }
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
