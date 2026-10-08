package com.example.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.PetKind
import com.example.engine.WorldViewport
import com.example.scene.PetMoves
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Plan 12, A: every pet doing each of its moves, five frames across each move, at the size the
 * game draws them (a 1080-wide screen). One row per move. PET_MOVES_PREVIEW_DIR=dir.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PetMovesPreviewTest {
    @Test
    fun everyPetsMoves() {
        val dir = System.getenv("PET_MOVES_PREVIEW_DIR")?.let { File(it).apply { mkdirs() } } ?: return
        val p = WorldViewport.pixelScale(1080f)
        val frames = floatArrayOf(0.1f, 0.3f, 0.5f, 0.7f, 0.9f)
        val cellW = 160
        val cellH = 130
        for (kind in PetKind.entries) {
            val moves = PetMoves.movesFor(kind)
            val w = cellW * frames.size
            val h = cellH * moves.size
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp)), Size(w.toFloat(), h.toFloat())) {
                drawRect(Color(0xFF8CC97A), Offset.Zero, Size(w.toFloat(), h.toFloat()))
                for ((r, move) in moves.withIndex()) {
                    if (r % 2 == 1) drawRect(Color(0xFF82BF70), Offset(0f, r * cellH.toFloat()), Size(w.toFloat(), cellH.toFloat()))
                    for ((i, f) in frames.withIndex()) {
                        val cx = cellW * i + cellW / 2f
                        val ground = cellH * (r + 1) - 18f
                        val t = move.seconds * f
                        if (PetSprites.drawsKind(kind)) {
                            PetSprites.drawMove(this, kind, move, t, cx, ground, p, facingLeft = false)
                        } else {
                            drawCatMove(this, move, t, cx, ground, p, t, facingLeft = false, isSnow = false, collarStyle = 0)
                        }
                    }
                }
            }
            File(dir, "moves_${kind.name.lowercase()}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            assertTrue(bmp.width > 0)
        }
    }
}
