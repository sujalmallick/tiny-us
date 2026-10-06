package com.example.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.MyApplicationTheme
import com.example.widget.TinyUsWidgetProvider

/** Android's side of [PlatformActions]: the home-screen widget. */
class AndroidPlatformActions(private val context: Context) : PlatformActions {
    override fun refreshWidgets() = TinyUsWidgetProvider.updateAllWidgets(context)
}

/** The app's theme with Android's [PlatformActions] for every shared screen inside it. */
@Composable
fun AndroidAppRoot(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val actions = remember { AndroidPlatformActions(context.applicationContext) }
    MyApplicationTheme {
        CompositionLocalProvider(LocalPlatformActions provides actions, content = content)
    }
}
