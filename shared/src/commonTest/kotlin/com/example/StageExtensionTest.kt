package com.example

import com.example.engine.StageExtension
import kotlin.test.Test
import kotlin.test.assertTrue

class StageExtensionTest {

    private fun channelsClose(a: Int, b: Int): Boolean =
        listOf(16, 8, 0).all { s -> kotlin.math.abs(((a shr s) and 0xFF) - ((b shr s) and 0xFF)) <= 48 }

    private fun luma(c: Int) = ((c shr 16) and 0xFF) * 299 + ((c shr 8) and 0xFF) * 587 + (c and 0xFF) * 114

    @Test
    fun groundBelowTheStageContinuesTheGroundsOwnTextureAndDarkensTowardTheEdge() {
        val w = 40
        val h = 120
        val stageBottom = 80
        val grass = 0xFF2F6B34.toInt()
        val blade = 0xFF3E7E43.toInt()
        val flower = 0xFFF38FB0.toInt() // a prop colour poking into the band
        val px = IntArray(w * h)
        for (y in 0 until stageBottom) for (x in 0 until w) {
            px[y * w + x] = when {
                y >= 64 && x == 5 -> flower
                y >= 64 && (x * 5 + y) % 7 == 0 -> blade
                else -> grass
            }
        }
        StageExtension.fill(px, w, h, stageTop = 0, stageBottom = stageBottom)
        val below = (stageBottom until h).flatMap { y -> (0 until w).map { x -> px[y * w + x] } }
        // Texture carries on (not one flat colour), and the prop's colour never does.
        assertTrue(below.toSet().size > 1, "the fill should keep the ground's texture")
        assertTrue(below.none { channelsClose(it, flower) }, "a prop colour leaked into the fill")
        // It darkens toward the screen edge, and the first row matches the ground exactly in tone.
        val firstRow = (0 until w).map { px[stageBottom * w + it] }
        val lastRow = (0 until w).map { px[(h - 1) * w + it] }
        assertTrue(firstRow.all { it == grass || it == blade })
        assertTrue(lastRow.sumOf { luma(it) } < firstRow.sumOf { luma(it) })
    }

    @Test
    fun groundBelowTheStageCarriesNoSpecklesFromAPropInTheBottomBand() {
        val w = 40
        val h = 100
        val stageBottom = 80
        val water = 0xFF3F74BD.toInt()
        val glint = 0xFF5A86C8.toInt()
        val grass = 0xFF0E1E17.toInt()
        val grassDot = 0xFF1A2E24.toInt()
        val px = IntArray(w * h)
        for (y in 0 until stageBottom) {
            for (x in 0 until w) {
                px[y * w + x] = when {
                    y in 64..69 -> if ((x + y) % 4 == 0) glint else water // a creek across the bottom band
                    y >= 70 -> if ((x * 7 + y * 3) % 11 == 0) grassDot else grass
                    else -> grass
                }
            }
        }
        StageExtension.fill(px, w, h, stageTop = 0, stageBottom = stageBottom)
        for (y in stageBottom until h) for (x in 0 until w) {
            val c = px[y * w + x]
            assertTrue(!channelsClose(c, water) && !channelsClose(c, glint), "creek colour leaked below the stage at ($x, $y)")
        }
    }
}
