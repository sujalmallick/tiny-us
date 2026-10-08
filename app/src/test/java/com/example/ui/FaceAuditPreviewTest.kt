package com.example.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.engine.AmbientAudio
import com.example.engine.CharacterEmotion
import com.example.engine.CharacterPose
import com.example.engine.Direction
import com.example.engine.Expression
import com.example.engine.GameText
import com.example.engine.PixelArtRenderer
import com.example.engine.PixelCharacter
import com.example.engine.WorldCamera
import com.example.engine.WorldViewport
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Plan 12, B: every face the two of them can make, side by side at the size the game really shows
 * them (a 1080-wide screen with the pixel renderer: one sprite pixel is ten screen pixels), and
 * the faces drawn by the loft and the scooter ride, in the world. FACE_AUDIT_DIR=dir.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FaceAuditPreviewTest {
    private val out: File? get() = System.getenv("FACE_AUDIT_DIR")?.let { File(it).apply { mkdirs() } }

    private class Case(val label: String, val setup: PixelCharacter.() -> Unit)

    private fun save(bmp: Bitmap, name: String) {
        val dir = out ?: return
        File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun everyFaceAtNativeSize() {
        out ?: return
        val charP = WorldViewport.characterPixelScale(1080f, lowRes = true)
        val standing = listOf(
            Case("idle") {},
            Case("blink") { isBlinking = true },
            Case("loving") { emotion = CharacterEmotion.LOVING },
            Case("shy emotion") { emotion = CharacterEmotion.SHY },
            Case("surprised") { pose = CharacterPose.SURPRISED },
            Case("sleepy") { emotion = CharacterEmotion.SLEEPY },
            Case("eat sneak") { pose = CharacterPose.EAT_SNEAK },
            Case("joy jump") { pose = CharacterPose.JOY_JUMP },
            Case("head pat") { pose = CharacterPose.HEAD_PAT_RECEIVE }
        ) + Expression.entries.filter { it != Expression.NONE }.map { e -> Case(e.name.lowercase()) { express(e, 5f); expressionAge = 0.3f } }
        val sitting = listOf(
            Case("sit idle") { pose = CharacterPose.SIT },
            Case("sit blink") { pose = CharacterPose.SIT; isBlinking = true },
            Case("sit snuggle") { pose = CharacterPose.SIT_SNUGGLE },
            Case("sit loving") { pose = CharacterPose.SIT; emotion = CharacterEmotion.LOVING }
        ) + Expression.entries.filter { it != Expression.NONE }.map { e -> Case("sit " + e.name.lowercase()) { pose = CharacterPose.SIT; express(e, 5f); expressionAge = 0.3f } }
        val together = listOf(
            Case("sleep") { pose = CharacterPose.SLEEP },
            Case("hug") { pose = CharacterPose.HUG },
            Case("kiss") { pose = CharacterPose.KISS }
        )
        val rows = listOf(standing, sitting, together)
        val cellW = 200
        val cellH = 300
        val cols = rows.maxOf { it.size }
        val w = cellW * cols
        val h = cellH * rows.size * 2
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val labels = StringBuilder()
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp)), Size(w.toFloat(), h.toFloat())) {
            drawRect(Color(0xFF8CC97A), Offset.Zero, Size(w.toFloat(), h.toFloat()))
            for ((r, row) in rows.withIndex()) {
                for ((i, case) in row.withIndex()) {
                    for (girl in listOf(false, true)) {
                        val c = PixelCharacter(isGirl = girl, name = "", worldX = 0f, worldY = 0f).apply {
                            direction = if (girl) Direction.LEFT else Direction.RIGHT
                            case.setup(this)
                        }
                        val y = cellH * (r * 2 + if (girl) 1 else 0) + cellH - 16f
                        PixelArtRenderer.drawCharacter(this, c, cellW * i + cellW / 2f, y, pixelSize = charP, snapToPixel = true)
                    }
                    labels.append("row ${r + 1} col ${i + 1}: ${case.label}\n")
                }
            }
        }
        save(bmp, "faces_native.png")
        out?.let { File(it, "faces_native.txt").writeText(labels.toString()) }
        assertTrue(bmp.width > 0)
    }

    @Test
    fun everyOutfitAtNativeSize() {
        out ?: return
        val charP = WorldViewport.characterPixelScale(1080f, lowRes = true)
        val outfits = (0 until PixelArtRenderer.BoyOutfitPalettes.size).map { false to it } +
            (0 until PixelArtRenderer.GirlDressPalettes.size).map { true to it }
        val cellW = 200
        val cellH = 300
        val w = cellW * outfits.size
        val h = cellH * 2
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp)), Size(w.toFloat(), h.toFloat())) {
            drawRect(Color(0xFF8CC97A), Offset.Zero, Size(w.toFloat(), h.toFloat()))
            for ((i, o) in outfits.withIndex()) {
                val (girl, index) = o
                for (row in 0..1) {
                    val c = PixelCharacter(isGirl = girl, name = "", worldX = 0f, worldY = 0f).apply {
                        outfitIndex = index
                        pose = if (row == 1) CharacterPose.SIT else CharacterPose.IDLE
                    }
                    PixelArtRenderer.drawCharacter(this, c, cellW * i + cellW / 2f, cellH * (row + 1) - 16f, pixelSize = charP, snapToPixel = true)
                }
            }
        }
        save(bmp, "outfits_native.png")
        assertTrue(bmp.width > 0)
    }

    @Test
    fun facesInTheWorld() {
        out ?: return
        runBlocking { GameText.load() }
        val cw = 1080f
        val ch = 2400f
        for ((name, scene, mode) in listOf(
            Triple("loft", SceneType.COZY_LOFT, "NIGHT"),
            Triple("ride", SceneType.EVENING_RIDE, "SUNSET"),
            Triple("tree", SceneType.UNDER_TREE, "DAY"),
            Triple("pier", SceneType.SEASIDE_PIER, "SUNSET")
        )) {
            val camera = WorldCamera.forScreen(cw, ch, scene, pixelRenderer = true, topReservePx = 0.09f * ch, bottomReservePx = 0.10f * ch)
            val engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
                weatherDriftEnabled = false
                updateAtmosphereMode(mode)
                loadScene(scene)
                autonomyEnabled = false
            }
            var t = 0f
            while (t < 12f) { engine.update(1f / 20f, camera.worldW, camera.worldH); t += 1f / 20f }
            val bmp = Bitmap.createBitmap(cw.toInt(), ch.toInt(), Bitmap.Config.ARGB_8888)
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp)), Size(cw, ch)) {
                drawWorld(engine, LowResWorldBuffer(), camera)
            }
            save(Bitmap.createBitmap(bmp, 0, (ch * 0.30f).toInt(), cw.toInt(), (ch * 0.45f).toInt()), "world_$name.png")
            bmp.recycle()
        }
        assertTrue(true)
    }
}
