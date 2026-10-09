package com.example.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.engine.AmbientAudio
import com.example.engine.CharacterPose
import com.example.engine.Direction
import com.example.engine.GameText
import com.example.engine.PixelArtRenderer
import com.example.engine.PixelCharacter
import com.example.engine.WorldCamera
import com.example.engine.WorldViewport
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.WeatherType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * In the rain the umbrella's pole runs through the boy's own raised fist, whichever way he
 * faces, whichever hand he holds it in, standing or sitting, mid-sway, mid-hop and mid-breath.
 * With UMBRELLA_PREVIEW_DIR set it also saves the couple under it in the world.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UmbrellaGripTest {

    @Test
    fun thePoleRunsThroughHisFist() {
        val p = WorldViewport.characterPixelScale(1080f, lowRes = true)
        val problems = mutableListOf<String>()
        for (facing in Direction.entries)
            for (backHand in listOf(false, true))
                for (pose in listOf(CharacterPose.IDLE, CharacterPose.WALK_2, CharacterPose.HUG, CharacterPose.SIT, CharacterPose.SIT_SNUGGLE))
                    for (snap in listOf(true, false))
                        for ((sway, hop, breath) in listOf(Triple(0f, 0f, 0f), Triple(3.3f, 17f, 1.7f), Triple(-6.1f, 0f, -2.2f))) {
                            val boy = PixelCharacter(isGirl = false, name = "Boy", worldX = 0.5f, worldY = 0.7f, pose = pose, direction = facing).apply {
                                idleSwayOffset = sway
                                bounceOffset = hop
                                breathingOffset = breath
                            }
                            val cx = 200.3f
                            val bottom = 420.6f
                            val bmp = Bitmap.createBitmap(400, 500, Bitmap.Config.ARGB_8888)
                            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp)), Size(400f, 500f)) {
                                PixelArtRenderer.drawCharacter(this, boy, cx, bottom, p, isHoldingUmbrella = true, snapToPixel = snap, umbrellaBackHand = backHand)
                            }
                            val g = PixelArtRenderer.umbrellaGrip(boy, cx, bottom, p, snap, backHand)
                            val fist = setOf(boy.look.skin.toArgb(), boy.look.skinShadow.toArgb())
                            // The middle of each of the fist's four blocks: the pole's column and the one inside it
                            for (dx in listOf(0, g.inward)) for (dy in 0..1) {
                                val x = (g.poleX + (dx + 0.5f) * g.block).toInt()
                                val y = (g.handTop + (dy + 0.5f) * g.block).toInt()
                                if (bmp.getPixel(x, y) !in fist) problems += "$facing ${if (backHand) "back" else "front"} $pose snap=$snap sway=$sway hop=$hop breath=$breath: no fist at block ($dx, $dy)"
                            }
                            // And just outside the fist, beyond the pole, there's nothing of him
                            val outX = (g.poleX + (-g.inward + 0.5f) * g.block).toInt()
                            val outY = (g.handTop + 0.5f * g.block).toInt()
                            if (bmp.getPixel(outX, outY) != 0) problems += "$facing ${if (backHand) "back" else "front"} $pose: something of him past the pole"
                            bmp.recycle()
                        }
        assertTrue("The pole misses his fist:\n" + problems.take(20).joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun underTheUmbrellaInTheWorld() {
        val out = System.getenv("UMBRELLA_PREVIEW_DIR")?.let { File(it).apply { mkdirs() } } ?: return
        runBlocking { GameText.load() }
        val cw = 1080f
        val ch = 2400f
        // Where she is from him, which way he faces, and whether they're sitting
        class Case(val name: String, val gap: Float, val boyFaces: Direction, val sit: Boolean = false)
        val cases = listOf(
            Case("close_right", 0.07f, Direction.RIGHT),
            Case("apart_right", 0.17f, Direction.RIGHT),
            Case("behind_him", -0.09f, Direction.RIGHT),
            Case("he_faces_left", -0.08f, Direction.LEFT),
            Case("far_apart", 0.34f, Direction.RIGHT),
            Case("sitting", 0.07f, Direction.RIGHT, sit = true)
        )
        for (c in cases) {
            val scene = SceneType.CAMPFIRE
            val camera = WorldCamera.forScreen(cw, ch, scene, pixelRenderer = true, topReservePx = 0.09f * ch, bottomReservePx = 0.10f * ch)
            val engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
                weatherDriftEnabled = false
                updateAtmosphereMode("NIGHT")
                loadScene(scene)
                autonomyEnabled = false
                weather = WeatherType.RAIN
            }
            var t = 0f
            while (t < 10f) { engine.update(1f / 20f, camera.worldW, camera.worldH); t += 1f / 20f }
            engine.cuddleProgress = 0f
            engine.boy.worldX = 0.42f
            engine.girl.worldX = 0.42f + c.gap
            engine.girl.worldY = engine.boy.worldY
            engine.boy.direction = c.boyFaces
            engine.girl.direction = if (c.gap > 0f) Direction.LEFT else Direction.RIGHT
            if (c.sit) {
                engine.boy.pose = CharacterPose.SIT
                engine.girl.pose = CharacterPose.SIT
            } else {
                engine.boy.pose = CharacterPose.IDLE
                engine.girl.pose = CharacterPose.IDLE
            }
            val bmp = Bitmap.createBitmap(cw.toInt(), ch.toInt(), Bitmap.Config.ARGB_8888)
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp)), Size(cw, ch)) {
                drawWorld(engine, LowResWorldBuffer(), camera)
            }
            val crop = Bitmap.createBitmap(bmp, 0, (ch * 0.30f).toInt(), cw.toInt(), (ch * 0.45f).toInt())
            File(out, "umbrella_${c.name}.png").outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
        }
        assertTrue(true)
    }
}
