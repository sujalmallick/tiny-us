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
import com.example.data.InMemoryKeyValueStorage
import com.example.data.Painting
import com.example.data.VisitorStore
import com.example.data.Visitors
import com.example.engine.AmbientAudio
import com.example.engine.AvatarLook
import com.example.engine.CharacterPose
import com.example.engine.GameText
import com.example.engine.WorldCamera
import com.example.scene.OldCoupleVisit
import com.example.scene.PainterVisit
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.WeatherType
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Plan 11: the visitors in the world, the loft painting, and the dialogs. VISITORS_PREVIEW_DIR=dir. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VisitorsPreviewTest {
    private val out: File? get() = System.getenv("VISITORS_PREVIEW_DIR")?.let { File(it).apply { mkdirs() } }
    private fun save(bmp: Bitmap, name: String) {
        val dir = out ?: return
        File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val sample = Painting(20733, "FLOWER", "SAKURA", "AFTERNOON", 0, 0, "HOLD_HANDS", true)

    @Test
    fun inTheWorld() {
        out ?: return
        runBlocking { GameText.load() }
        val cw = 1080f
        val ch = 2400f
        fun shot(name: String, scene: SceneType, mode: String, weather: WeatherType?, together: String? = null, until: (SceneEngine) -> Boolean, setup: (SceneEngine, VisitorStore) -> Unit = { _, _ -> }) {
            Visitors.todayOverride = LocalDate(2026, 10, 7)
            val camera = WorldCamera.forScreen(cw, ch, scene, pixelRenderer = true, topReservePx = 0.09f * ch, bottomReservePx = 0.10f * ch)
            val store = VisitorStore(InMemoryKeyValueStorage())
            val engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
                weatherDriftEnabled = false
                updateAtmosphereMode(mode)
                loadScene(scene)
                autonomyEnabled = false
                weather?.let { this.weather = it }
                visitorStore = store
            }
            setup(engine, store)
            var t = 0f
            while (t < 120f && !until(engine)) {
                if (together == "HOLD_HANDS") { engine.boy.pose = CharacterPose.HOLD_HANDS; engine.girl.pose = CharacterPose.HOLD_HANDS }
                engine.update(1f / 20f, camera.worldW, camera.worldH); t += 1f / 20f
            }
            val bmp = Bitmap.createBitmap(cw.toInt(), ch.toInt(), Bitmap.Config.ARGB_8888)
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp)), Size(cw, ch)) {
                drawWorld(engine, LowResWorldBuffer(), camera)
            }
            save(Bitmap.createBitmap(bmp, 0, (ch * 0.25f).toInt(), cw.toInt(), (ch * 0.65f).toInt()), "$name.png")
            bmp.recycle()
            Visitors.todayOverride = null
        }
        shot("painter_painting", SceneType.FLOWER, "DAY", WeatherType.SAKURA, until = { it.painter.phase == PainterVisit.Phase.PAINTING && it.painter.progress > 0.5f })
        shot("painter_reveal", SceneType.FLOWER, "DAY", WeatherType.SAKURA, "HOLD_HANDS", until = { it.painter.showing })
        shot("old_couple", SceneType.SEASIDE_PIER, "SUNSET", null, "HOLD_HANDS", until = { it.oldCouple.phase == OldCoupleVisit.Phase.SITTING && it.oldCouple.mood == OldCoupleVisit.Mood.HOLD_HANDS })
        shot("old_couple_note", SceneType.SEASIDE_PIER, "DAY", null, until = { it.oldCouple.noteWaiting && it.oldCouple.x < 0.05f }) { e, _ -> e.togetherSince = "2023-10-07" }
        shot("loft_painting", SceneType.COZY_LOFT, "NIGHT", null, until = { it.sceneTime > 2f }) { _, store -> store.addPainting(sample) }
        assertTrue(true)
    }

    @Test
    fun theDialogs() {
        runBlocking { GameText.load() }
        val panels: List<Pair<String, @androidx.compose.runtime.Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit>> = listOf(
            "paintings" to {
                PaintingsPanel(
                    listOf(sample.copy(scene = "WALK", weather = "RAIN", phase = "SUNSET", together = "APART", mochi = false), sample),
                    AvatarLook.defaultFor(false), AvatarLook.defaultFor(true), "Bean", "Sprout"
                ) {}
            },
            "note" to { AnniversaryNotePanel {} }
        )
        for ((name, panel) in panels) {
            val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
            val view = ComposeView(activity)
            activity.setContentView(view)
            view.setContent {
                TinySurface(modifier = Modifier.width(380.dp)) { Column(Modifier.padding(20.dp)) { panel() } }
            }
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(2))
            val image = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            image.eraseColor(android.graphics.Color.rgb(0x2E, 0x21, 0x1D))
            view.draw(android.graphics.Canvas(image))
            assertTrue(image.width > 100 && image.height > 100)
            save(image, "dialog_$name.png")
        }
    }
}
