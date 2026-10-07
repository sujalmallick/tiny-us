package com.example.ui

import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.InMemoryKeyValueStorage
import com.example.data.PetKind
import com.example.data.PetStore
import com.example.engine.AmbientAudio
import com.example.engine.GameText
import com.example.engine.WorldCamera
import com.example.scene.CatState
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.WeatherType
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

/** Plan 10, E: the pets in every state, meeting them out in the world, and Our pets. PETS_PREVIEW_DIR=dir. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PetsPreviewTest {
    private val out: File? get() = System.getenv("PETS_PREVIEW_DIR")?.let { File(it).apply { mkdirs() } }
    private fun save(bmp: Bitmap, name: String) {
        val dir = out ?: return
        File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun everyPetInEveryState() {
        val kinds = PetKind.entries.filter { it != PetKind.CAT }
        val states = CatState.entries
        val cw = 150f
        val ch = 110f
        val image = Bitmap.createBitmap((cw * states.size).toInt(), (ch * kinds.size).toInt(), Bitmap.Config.ARGB_8888)
        image.eraseColor(android.graphics.Color.rgb(0x8C, 0xC0, 0x7A))
        val canvas = androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(image))
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(image.width.toFloat(), image.height.toFloat())) {
            kinds.forEachIndexed { row, kind ->
                states.forEachIndexed { col, state ->
                    PetSprites.drawPet(this, kind, col * cw + cw * 0.62f, row * ch + ch - 16f, 4f, 0.4f, state, facingLeft = col % 2 == 1)
                }
            }
        }
        assertTrue(image.width > 0)
        save(image, "pets_states.png")
    }

    @Test
    fun meetingThemOutInTheWorld() {
        val dir = out ?: return
        runBlocking { GameText.load() }
        val cw = 1080f
        val ch = 2400f
        fun shot(name: String, scene: SceneType, mode: String, weather: WeatherType?, chosen: PetKind? = null) {
            val camera = WorldCamera.forScreen(cw, ch, scene, pixelRenderer = true, topReservePx = 0.09f * ch, bottomReservePx = 0.10f * ch)
            val engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
                weatherDriftEnabled = false
                updateAtmosphereMode(mode)
                loadScene(scene)
                autonomyEnabled = false
                weather?.let { this.weather = it }
                petStore = PetStore(InMemoryKeyValueStorage()).apply { chosen?.let { meet(it); this.chosen = it } }
            }
            var t = 0f
            while (t < SceneEngine.PET_VISITOR_AFTER + 1.5f) { engine.update(1f / 30f, camera.worldW, camera.worldH); t += 1f / 30f }
            val bmp = Bitmap.createBitmap(cw.toInt(), ch.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp))
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(cw, ch)) {
                drawWorld(engine, LowResWorldBuffer(), camera)
            }
            save(Bitmap.createBitmap(bmp, 0, (ch * 0.30f).toInt(), cw.toInt(), (ch * 0.58f).toInt()), "$name.png")
            bmp.recycle()
        }
        shot("meet_bunny", SceneType.FLOWER, "DAY", WeatherType.SUNNY)
        shot("meet_hedgehog", SceneType.UNDER_TREE, "SUNSET", WeatherType.AUTUMN)
        shot("meet_duck", SceneType.WALK, "DAY", WeatherType.RAIN)
        shot("meet_owl", SceneType.LOOKING, "NIGHT", WeatherType.SUNNY)
        shot("lives_puppy", SceneType.SLEEP, "DAY", null, chosen = PetKind.PUPPY)
        shot("lives_duck", SceneType.FLOWER, "DAY", WeatherType.SUNNY, chosen = PetKind.DUCK)
        shot("lives_fox_loft", SceneType.COZY_LOFT, "NIGHT", null, chosen = PetKind.FOX)
        assertTrue(dir.exists())
    }

    @Test
    fun ourPetsPanel() {
        runBlocking { GameText.load() }
        val store = PetStore(InMemoryKeyValueStorage()).apply {
            meet(PetKind.PUPPY); meet(PetKind.BUNNY); meet(PetKind.DUCK)
            chosen = PetKind.PUPPY
        }
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val view = ComposeView(activity)
        activity.setContentView(view)
        view.setContent {
            TinySurface(modifier = Modifier.width(380.dp)) {
                Column(Modifier.padding(20.dp)) {
                    OurPetsPanel(store, PetKind.PUPPY, "Mochi", {}, {})
                }
            }
        }
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(2))
        val image = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        image.eraseColor(android.graphics.Color.rgb(0x2E, 0x21, 0x1D))
        view.draw(android.graphics.Canvas(image))
        assertTrue(image.width > 100 && image.height > 100)
        save(image, "our_pets.png")
    }
}
