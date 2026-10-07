@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.example.shell

import com.example.data.IosUserDefaultsStorage
import com.example.scene.SceneType
import com.example.scene.WeatherType
import com.example.ui.WidgetScene
import com.example.ui.encodePng
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSUserDefaults
import platform.Foundation.create

/** What the home-screen widget shows, handed to the Swift app to write for WidgetKit. */
data class IosWidgetSnapshot(
    val coupleNames: String,
    val daysTogether: Long,
    /** The anniversary as yyyy-MM-dd. */
    val anniversary: String,
    val sceneName: String,
    val weatherName: String,
    val timePhase: String,
    val dailyMomentPrompt: String,
    val latestSignalText: String
)

/**
 * The bridge to the widget: WidgetKit has no Objective-C API, so the Swift app sets [publish] at
 * launch (`IosWidget.shared.publish = { snapshot in ... }`) and writes the App Group payload itself.
 */
object IosWidget {
    var publish: ((IosWidgetSnapshot) -> Unit)? = null
}

/**
 * The widget's scene pictures: the shared [WidgetScene] at one game pixel per pixel (the widget
 * scales it up without smoothing), as PNGs in the App Group defaults where the widget reads them.
 * One picture per widget shape: small is square, medium about 2.14 : 1.
 */
object IosWidgetPictures {
    const val SMALL_KEY = "tiny-us.widget.scene.small"
    const val MEDIUM_KEY = "tiny-us.widget.scene.medium"

    private const val MEDIUM_ASPECT = 2.14f

    /** Draws both pictures and saves them; slow enough to keep off the main thread. */
    fun save(scene: SceneType, weather: WeatherType, atmosphereMode: String, boyName: String, girlName: String) {
        val defaults = NSUserDefaults(suiteName = IosUserDefaultsStorage.APP_GROUP_SUITE)
        val w = WidgetScene.STAGE_W
        listOf(SMALL_KEY to w, MEDIUM_KEY to (w / MEDIUM_ASPECT).toInt()).forEach { (key, h) ->
            val png = encodePng(WidgetScene.render(scene, weather, atmosphereMode, boyName, girlName, w, h)) ?: return@forEach
            defaults.setObject(png.toNSData(), forKey = key)
        }
    }

    private fun ByteArray.toNSData(): NSData = usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
}
