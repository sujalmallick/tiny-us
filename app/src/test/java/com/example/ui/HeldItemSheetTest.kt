package com.example.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.engine.CharacterPose
import com.example.engine.Direction
import com.example.engine.HeldItem
import com.example.engine.PixelArtRenderer
import com.example.engine.PixelCharacter
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Plan 10, A: a contact sheet of every held item on both of them: carried and in use, standing and
 * sitting, facing right and left. Written to HELD_SHEET_DIR when it's set.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HeldItemSheetTest {
    private class Cell(val label: String, val girl: Boolean, val pose: CharacterPose, val dir: Direction, val use: Boolean)

    private val cells = listOf(
        Cell("stand R", true, CharacterPose.IDLE, Direction.RIGHT, false),
        Cell("stand L", true, CharacterPose.IDLE, Direction.LEFT, false),
        Cell("use R", true, CharacterPose.IDLE, Direction.RIGHT, true),
        Cell("use L", true, CharacterPose.IDLE, Direction.LEFT, true),
        Cell("walk R", false, CharacterPose.WALK_2, Direction.RIGHT, false),
        Cell("boy use R", false, CharacterPose.IDLE, Direction.RIGHT, true),
        Cell("sit R", true, CharacterPose.SIT, Direction.RIGHT, false),
        Cell("sit use L", false, CharacterPose.SIT, Direction.LEFT, true)
    )

    @Test
    fun everyHeldItemDrawsInEveryPose() {
        val items = HeldItem.entries.filter { it != HeldItem.NONE }
        // A row more for the poses that draw their own props (the spoon, the rose, the momo).
        val poseRow = listOf(CharacterPose.COOK, CharacterPose.GIVE_FLOWER, CharacterPose.EAT_MOMO, CharacterPose.FEED_MOMO, CharacterPose.WAVE, CharacterPose.HEAD_PAT, CharacterPose.SIT_SNUGGLE, CharacterPose.EAT_SNEAK)
        val cw = 120f
        val ch = 130f
        val w = (cw * cells.size).toInt()
        val h = (ch * (items.size + 2)).toInt()
        val image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        image.eraseColor(android.graphics.Color.rgb(0xF6, 0xEE, 0xE8))
        val canvas = androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(image))
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(w.toFloat(), h.toFloat())) {
            items.forEachIndexed { row, item ->
                if (row % 2 == 1) drawRect(Color(0xFFEFE3DA), androidx.compose.ui.geometry.Offset(0f, row * ch), Size(w.toFloat(), ch))
                cells.forEachIndexed { col, cell ->
                    val c = PixelCharacter(isGirl = cell.girl, name = if (cell.girl) "Sprout" else "Bean", worldX = 0f, worldY = 0f, pose = cell.pose, direction = cell.dir)
                    c.hold(item, 60f, if (cell.use) 30f else 0f)
                    c.heldItemAge = 0.3f
                    if (item == HeldItem.CAKE) c.heldItemState = 0b111
                    PixelArtRenderer.drawCharacter(this, c, col * cw + cw / 2f, row * ch + ch - 14f, pixelSize = 3.5f)
                }
            }
            val row = items.size
            poseRow.forEachIndexed { col, pose ->
                val c = PixelCharacter(isGirl = col % 2 == 0, name = "x", worldX = 0f, worldY = 0f, pose = pose, direction = Direction.RIGHT)
                PixelArtRenderer.drawCharacter(this, c, col * cw + cw / 2f, row * ch + ch - 14f, pixelSize = 3.5f)
            }
            // The pan at the stove: mid-toss and resting, both ways, both of them; then tossed in use.
            val panRow = items.size + 1
            listOf(0.4f, 2f, 0.4f, 2f, 0.4f, 2f, 0.2f, 0.6f).forEachIndexed { col, age ->
                val c = PixelCharacter(isGirl = col < 4 || col >= 6, name = "x", worldX = 0f, worldY = 0f, pose = CharacterPose.COOK,
                    direction = if (col % 4 < 2) Direction.RIGHT else Direction.LEFT)
                c.hold(HeldItem.PAN, 60f, if (col >= 6) 30f else 0f)
                c.heldItemAge = age
                PixelArtRenderer.drawCharacter(this, c, col * cw + cw / 2f, panRow * ch + ch - 14f, pixelSize = 3.5f)
            }
        }
        assertTrue(image.width > 0)
        val dir = System.getenv("HELD_SHEET_DIR") ?: return
        File(dir).mkdirs()
        File(dir, "held_items.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        File(dir, "held_items_rows.txt").writeText(
            (items.map { it.name } + "POSES: " + poseRow.joinToString { it.name }).joinToString("\n") +
                "\nCOLUMNS: " + cells.joinToString { it.label }
        )
    }
}
