package com.example.ui

import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.Friend
import com.example.engine.AmbientAudio
import com.example.engine.GameText
import com.example.engine.WorldCamera
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Plan 09, I: the cafe open and after hours, Bao's tea, and the three secrets. FRIENDS_PREVIEW_DIR=dir. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FriendsPreviewTest {
    private val out: File? get() = System.getenv("FRIENDS_PREVIEW_DIR")?.let { File(it).apply { mkdirs() } }
    private fun save(bmp: Bitmap, name: String) {
        val dir = out ?: return
        File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun theirHours() {
        out ?: return
        val cw = 1080f
        val ch = 2400f
        fun shot(name: String, scene: SceneType, mode: String, hour: Int) {
            val camera = WorldCamera.forScreen(cw, ch, scene, pixelRenderer = true, topReservePx = 0.09f * ch, bottomReservePx = 0.10f * ch)
            val engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
                weatherDriftEnabled = false
                updateAtmosphereMode(mode)
                loadScene(scene)
                autonomyEnabled = false
                clockHourOverride = hour
            }
            var t = 0f
            while (t < 2f) { engine.update(1f / 30f, camera.worldW, camera.worldH); t += 1f / 30f }
            val bmp = Bitmap.createBitmap(cw.toInt(), ch.toInt(), Bitmap.Config.ARGB_8888)
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp)), Size(cw, ch)) {
                drawWorld(engine, LowResWorldBuffer(), camera)
            }
            save(Bitmap.createBitmap(bmp, 0, (ch * 0.18f).toInt(), cw.toInt(), (ch * 0.62f).toInt()), "$name.png")
            bmp.recycle()
        }
        shot("cafe_open", SceneType.RAINY_CAFE, "DAY", 10)
        shot("cafe_closed_wiping", SceneType.RAINY_CAFE, "NIGHT", 22)
        shot("cafe_closed_reading", SceneType.RAINY_CAFE, "NIGHT", 23)
        shot("pier_tea_at_four", SceneType.SEASIDE_PIER, "DAY", 16)
    }

    @Test
    fun theSecrets() {
        runBlocking { GameText.load() }
        for (friend in Friend.entries) {
            val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
            val view = ComposeView(activity)
            activity.setContentView(view)
            view.setContent {
                TinySurface(modifier = Modifier.width(380.dp)) {
                    Column(Modifier.padding(20.dp)) { FriendSecretPanel(friend, "Bean", "Sprout") {} }
                }
            }
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(2))
            val image = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            image.eraseColor(android.graphics.Color.rgb(0x2E, 0x21, 0x1D))
            view.draw(android.graphics.Canvas(image))
            assertTrue(image.width > 100 && image.height > 100)
            save(image, "secret_${friend.name.lowercase()}.png")
        }
    }
}
