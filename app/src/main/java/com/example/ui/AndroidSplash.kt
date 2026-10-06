package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.data.PreferencesManager
import com.example.engine.AmbientAudio

/** The shared splash with Android's saved data and sound. */
@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val audio = remember { AmbientAudio(context.applicationContext).apply { isEnabled = prefs.soundEnabled } }
    SplashScreen(prefs = prefs, audio = audio, onSplashComplete = onSplashComplete)
}
