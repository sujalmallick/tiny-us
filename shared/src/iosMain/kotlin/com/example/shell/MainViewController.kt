package com.example.shell

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.IosAppShell
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
        // A restore bumps the generation: every screen is rebuilt with freshly read data.
        key(IosAppShell.generation) {
            val platform = remember { IosMainPlatform() }
            val actions = remember { IosPlatformActions(platform.prefs) }
            val lockDevice = remember { IosLockDevice() }
            MyApplicationTheme {
                CompositionLocalProvider(LocalPlatformActions provides actions, LocalLockDevice provides lockDevice) {
                    Box(Modifier.fillMaxSize()) {
                        // While locked, the lock screen replaces the whole app (dialogs included).
                        if (AppLock.isLocked) AppLockScreen(IosLock.store) else MainScreen(platform = platform)
                        ShellMessage(Modifier.align(Alignment.BottomCenter))
                    }
                }
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

/** A short note at the bottom of the screen for a few seconds (iOS has no toast). */
@androidx.compose.runtime.Composable
private fun ShellMessage(modifier: Modifier) {
    val text = IosAppShell.message ?: return
    LaunchedEffect(text) {
        kotlinx.coroutines.delay(3500)
        IosAppShell.message = null
    }
    androidx.compose.material3.Text(
        text,
        style = com.example.ui.theme.TinyType.Body.copy(color = androidx.compose.ui.graphics.Color.White),
        modifier = modifier
            .padding(bottom = 96.dp, start = 24.dp, end = 24.dp)
            .background(androidx.compose.ui.graphics.Color(0xE6332A36), com.example.ui.theme.TinyRadius.Medium)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}
