package com.example.games

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Set COZY_SPRITES_DIR (and run with --rerun) to write a sheet of every cooking, fishing and garden sprite. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CozySpritesPreviewTest {

    @Test
    fun writesTheSpriteSheetWhenAsked() {
        val out = File(System.getenv("COZY_SPRITES_DIR") ?: return).apply { mkdirs() }
        val p = 10f
        val cell = 14 * p
        val rows = listOf(
            CozySprites.INGREDIENTS.values.toList(),
            CozySprites.DISHES.values.toList() + CozySprites.CATCHES.values.toList(),
            CozySprites.PLOT.values.toList() + listOf(CozySprites.BOUQUET, CozySprites.BOBBER, CozySprites.WATERING_CAN)
        )
        val w = (rows.maxOf { it.size } * cell).toInt()
        val h = (rows.size * cell).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp))
        val tulip = CozySprites.flowerColors("tulip")
        val bunch = listOf("tulip", "daisy", "lavender").map { CozySprites.flowerColors(it).first }
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(w.toFloat(), h.toFloat())) {
            drawRect(Color(0xFFC8A27A), Offset.Zero, Size(w.toFloat(), h.toFloat()))
            for ((r, sprites) in rows.withIndex()) for ((i, s) in sprites.withIndex()) {
                val left = i * cell + (cell - s.width * p) / 2f
                val top = r * cell + (cell - s.height * p) / 2f
                val petals = if (s === CozySprites.BOUQUET) bunch else listOf(tulip.first)
                CozySprites.draw(this, s, left, top, p, petals, tulip.second)
            }
        }
        File(out, "cozy_sprites.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** With COZY_SPRITES_DIR set, also a sheet of the five request bubbles (plan 07 D1), fresh and fading. */
    @Test
    fun writesTheRequestBubblesWhenAsked() {
        val out = File(System.getenv("COZY_SPRITES_DIR") ?: return).apply { mkdirs() }
        val kinds = com.example.engine.PixelArtRenderer.requestIconNames.toList()
        val p = 16f
        val cell = 10 * p
        val w = (kinds.size * cell).toInt()
        val h = (2 * cell).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp))
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(w.toFloat(), h.toFloat())) {
            drawRect(Color(0xFF9AD1F5), Offset.Zero, Size(w.toFloat(), h.toFloat()))
            kinds.forEachIndexed { i, k ->
                com.example.engine.PixelArtRenderer.drawRequestBubble(this, k, i * cell + cell / 2f, cell * 0.85f, p, 1f, 0f)
                com.example.engine.PixelArtRenderer.drawRequestBubble(this, k, i * cell + cell / 2f, cell * 1.85f, p, 0.4f, 0f)
            }
        }
        File(out, "request_bubbles.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
