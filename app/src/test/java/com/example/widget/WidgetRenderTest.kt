package com.example.widget

import android.graphics.Bitmap
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.data.PreferencesManager
import com.example.scene.SceneType
import com.example.scene.WeatherType
import com.example.ui.WidgetSceneRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The home-screen widget (plan 06, H4): its scene picture is drawn in the widget's shape, and the
 * whole widget lays out at small, default and large sizes. With WIDGET_PREVIEW_DIR set it also
 * saves each size as a PNG (the default one is the widget picker's preview).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetRenderTest {

    @Test
    fun pictureIsDrawnInTheWidgetsShapeWithNoGaps() {
        val picture = WidgetSceneRenderer.render(SceneType.FLOWER, WeatherType.SUNNY, "DAY", "Leo", "Mia", 750, 330)
        // Drawn at a whole-number zoom (6 x 144 = 864 >= 750), cropped to the widget's shape.
        assertEquals(864, picture.width)
        assertEquals(864 * 330 / 750, picture.height)
        var transparent = 0
        for (y in 0 until picture.height step 7) for (x in 0 until picture.width step 7) {
            if (picture.getPixel(x, y) ushr 24 == 0) transparent++
        }
        assertEquals("transparent pixels in the picture", 0, transparent)
    }

    @Test
    fun widgetLaysOutAtEverySize() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        PreferencesManager(context).apply {
            boyfriendName = "Leo"
            girlfriendName = "Mia"
        }
        WidgetState.save(context, SceneType.FLOWER, WeatherType.SUNNY)
        val dir = System.getenv("WIDGET_PREVIEW_DIR")?.let { File(it).apply { mkdirs() } }
        val density = context.resources.displayMetrics.density
        for ((name, size) in listOf("small" to (180 to 100), "default" to (250 to 110), "large" to (320 to 200))) {
            val (wDp, hDp) = size
            val views = TinyUsWidgetProvider.buildViews(context, wDp, hDp)
            val parent = FrameLayout(context)
            val widget = views.apply(context, parent)
            val w = (wDp * density).toInt()
            val h = (hDp * density).toInt()
            widget.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            widget.layout(0, 0, w, h)
            assertEquals("Leo & Mia", widget.findViewById<TextView>(R.id.widget_couple_names).text.toString())
            assertTrue(widget.findViewById<TextView>(R.id.widget_day_counter).text.isNotBlank())
            val image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            widget.draw(android.graphics.Canvas(image))
            dir?.let { d -> File(d, "widget_$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }
    }
}
