package com.example.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.engine.CharacterPose
import com.example.engine.Direction
import com.example.engine.Expression
import com.example.engine.PixelArtRenderer
import com.example.engine.PixelCharacter
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Plan 12, B: every expression on both of them, standing and seated. EXPRESSIONS_PREVIEW_DIR=dir. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExpressionsPreviewTest {
    private val out: File? get() = System.getenv("EXPRESSIONS_PREVIEW_DIR")?.let { File(it).apply { mkdirs() } }

    @Test
    fun everyFace() {
        val dir = out ?: return
        val looks = Expression.entries
        val cell = 150f
        val p = 4.4f
        val w = (cell * looks.size).toInt()
        val h = (cell * 4).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp)), Size(w.toFloat(), h.toFloat())) {
            drawRect(Color(0xFFBFE3B4), Offset.Zero, Size(w.toFloat(), h.toFloat()))
            for ((i, e) in looks.withIndex()) {
                for (row in 0 until 4) {
                    val girl = row % 2 == 1
                    val sitting = row >= 2
                    val c = PixelCharacter(isGirl = girl, name = "", worldX = 0f, worldY = 0f).apply {
                        pose = if (sitting) CharacterPose.SIT else CharacterPose.IDLE
                        direction = if (i % 2 == 0) Direction.RIGHT else Direction.LEFT
                        express(e, 5f)
                        expressionAge = 0.3f
                    }
                    PixelArtRenderer.drawCharacter(this, c, cell * i + cell / 2f, cell * (row + 1) - 14f, pixelSize = p)
                }
            }
        }
        File(dir, "expressions.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(bmp.width > 0)
    }
}
