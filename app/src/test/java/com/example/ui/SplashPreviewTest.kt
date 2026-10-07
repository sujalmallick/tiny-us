package com.example.ui

import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import com.example.data.PreferencesManager
import com.example.engine.AmbientAudio
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration

/**
 * Set SPLASH_SHOT_DIR (and run with --rerun) to render the splash screen, the couple on the pier
 * at sunset, on a tall phone and on a 7-inch tablet.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SplashPreviewTest {

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h800dp-port-xxhdpi")
    fun phone() = render("splash_phone.png")

    @Test
    @Config(sdk = [34], qualifiers = "w600dp-h960dp-port-xhdpi")
    fun tablet() = render("splash_tablet.png")

    private fun render(name: String) {
        val dir = System.getenv("SPLASH_SHOT_DIR")?.let { File(it) }
        assumeTrue("Set SPLASH_SHOT_DIR to render the splash", dir != null)
        dir!!.mkdirs()
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val prefs = PreferencesManager(activity).apply {
            boyfriendName = "Leo"
            girlfriendName = "Mia"
            soundEnabled = false
        }
        val view = ComposeView(activity)
        activity.setContentView(view)
        view.setContent { SplashScreen(prefs = prefs, audio = AmbientAudio().apply { isEnabled = false }, onSplashComplete = {}) }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))
        val image = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(image))
        File(dir, name).outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        activity.finish()
    }
}
