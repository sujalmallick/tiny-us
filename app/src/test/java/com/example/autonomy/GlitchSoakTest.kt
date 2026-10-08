package com.example.autonomy

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.InMemoryKeyValueStorage
import com.example.data.PetKind
import com.example.data.PetStore
import com.example.engine.AmbientAudio
import com.example.engine.Expression
import com.example.engine.GameText
import com.example.engine.WorldCamera
import com.example.scene.PetMoves
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.WeatherType
import com.example.scene.autonomy.SceneSpots
import com.example.ui.LowResWorldBuffer
import com.example.ui.drawWorld
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.random.Random

/**
 * Plan 12: a long soak in every scene with the couple's own routine on, the pet doing its moves,
 * and a stream of random taps (the pet, things to use, the ground, the two of them), checking
 * every frame that nobody is stuck, lost off the stage or left in a half-finished state.
 * With SOAK_PREVIEW_DIR set it also saves a frame from each scene now and then.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlitchSoakTest {
    private val cw = 1080f
    private val ch = 2400f
    private val dt = 0.05f

    private fun engineFor(kind: PetKind, mode: String): SceneEngine {
        runBlocking { GameText.load() }
        return SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
            weatherDriftEnabled = false
            updateAtmosphereMode(mode)
            autonomyEnabled = true
            val store = PetStore(InMemoryKeyValueStorage())
            PetKind.entries.forEach { store.meet(it) }
            petStore = store
            choosePet(kind)
        }
    }

    @Test
    fun everySceneHoldsTogetherUnderRandomPlay() {
        val out = System.getenv("SOAK_PREVIEW_DIR")?.let { File(it).apply { mkdirs() } }
        val pets = listOf(PetKind.CAT, PetKind.PUPPY, PetKind.OWL, PetKind.DUCK, PetKind.BUNNY, PetKind.FOX, PetKind.HEDGEHOG)
        val modes = listOf("DAY", "SUNSET", "NIGHT")
        val weathers = WeatherType.entries
        val problems = mutableListOf<String>()
        for ((si, scene) in SceneType.entries.withIndex()) {
            val kind = pets[si % pets.size]
            val mode = modes[si % modes.size]
            val e = engineFor(kind, mode)
            e.loadScene(scene)
            e.weather = weathers[si % weathers.size]
            val rnd = Random(100 + si)
            val spots = SceneSpots.forScene(scene)
            var t = 0f
            var goUseFor = 0f
            var nextTap = 9f
            var nextShot = 20f
            var used = 0
            try {
                while (t < 150f) {
                    e.update(dt, cw, ch)
                    t += dt
                    if (t >= nextTap) {
                        nextTap = t + 0.4f + rnd.nextFloat() * 1.6f
                        when (rnd.nextInt(8)) {
                            0 -> e.onTouchCat(cw, ch)
                            1, 2 -> if (spots.isNotEmpty()) e.sendToUse(spots[rnd.nextInt(spots.size)].action, cw, ch) { used++ }
                            3 -> e.commandCatWalkTo(0.15f + rnd.nextFloat() * 0.7f, 0.70f, cw, ch)
                            4 -> e.onTouchWalkableGround(cw * (0.15f + rnd.nextFloat() * 0.7f), ch * (0.70f + rnd.nextFloat() * 0.06f), cw, ch)
                            5 -> if (rnd.nextBoolean()) e.onTouchBoy(cw, ch) else e.onTouchGirl(cw, ch)
                            6 -> e.startPetMove(PetMoves.movesFor(kind).random(rnd), cw, ch)
                            else -> e.notifyUserInteraction()
                        }
                    }
                    // Nobody lost off the stage
                    if (scene != SceneType.EVENING_RIDE) {
                        for (c in listOf(e.boy, e.girl)) {
                            if (c.worldX !in -0.02f..1.02f || c.worldY !in 0.3f..1.0f) problems += "$scene t=$t ${c.name} at ${c.worldX}, ${c.worldY}"
                            if (c.bounceOffset > 40f) problems += "$scene t=$t ${c.name} bounce ${c.bounceOffset}"
                            if (c.expressionTimer <= 0f && c.expression != Expression.NONE) problems += "$scene t=$t ${c.name} expression ${c.expression} left on"
                        }
                        if (e.catWorldX !in -0.02f..1.02f || e.catWorldY !in 0.3f..1.0f) problems += "$scene t=$t pet at ${e.catWorldX}, ${e.catWorldY}"
                    }
                    // Nothing half-finished
                    val move = e.petMove
                    if (move != null && e.petMoveTime > move.seconds + 0.2f) problems += "$scene t=$t $kind stuck in $move"
                    goUseFor = if (e.isGoingToUseSomething) goUseFor + dt else 0f
                    if (goUseFor > 25f) { problems += "$scene t=$t stuck going to use something"; goUseFor = 0f }
                    if (out != null && t >= nextShot) {
                        nextShot = t + 40f
                        val camera = WorldCamera.forScreen(cw, ch, scene, pixelRenderer = true, topReservePx = 0.09f * ch, bottomReservePx = 0.10f * ch)
                        val bmp = Bitmap.createBitmap(cw.toInt(), ch.toInt(), Bitmap.Config.ARGB_8888)
                        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp)), Size(cw, ch)) {
                            drawWorld(e, LowResWorldBuffer(), camera)
                        }
                        val crop = Bitmap.createBitmap(bmp, 0, (ch * 0.25f).toInt(), cw.toInt(), (ch * 0.6f).toInt())
                        val small = Bitmap.createScaledBitmap(crop, crop.width / 2, crop.height / 2, false)
                        File(out, "soak_${scene.name.lowercase()}_${t.toInt()}.png").outputStream().use { small.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        bmp.recycle()
                    }
                }
            } catch (ex: Throwable) {
                fail("$scene ($kind, $mode) crashed at t=$t: $ex")
            }
            println("SOAK $scene: $kind $mode, used $used things")
        }
        val distinct = problems.distinctBy { it.substringBefore(" t=") + it.substringAfter(" ", "").substringAfter(" ").take(30) }
        if (distinct.isNotEmpty()) println("SOAK PROBLEMS:\n" + distinct.take(60).joinToString("\n"))
        assertTrue("Glitches:\n" + distinct.take(25).joinToString("\n"), distinct.isEmpty())
    }
}
