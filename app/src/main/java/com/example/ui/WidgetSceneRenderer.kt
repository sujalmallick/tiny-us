package com.example.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.example.scene.SceneType
import com.example.scene.WeatherType

/**
 * The couple's scene as a small pixel picture for the home-screen widget (plan 06, H1): the shared
 * [WidgetScene] at the widget's width, as an Android bitmap. Safe off the main thread.
 */
object WidgetSceneRenderer {
    fun render(
        scene: SceneType,
        weather: WeatherType,
        atmosphereMode: String,
        boyName: String,
        girlName: String,
        widthPx: Int,
        heightPx: Int
    ): Bitmap = WidgetScene.render(scene, weather, atmosphereMode, boyName, girlName, widthPx, heightPx).asAndroidBitmap()
}
