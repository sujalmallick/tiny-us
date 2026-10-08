package com.example

import com.example.engine.StageExtension
import kotlin.test.Test
import kotlin.test.assertTrue

class StageExtensionTest {

    private fun channelsClose(a: Int, b: Int): Boolean =
        listOf(16, 8, 0).all { s -> kotlin.math.abs(((a shr s) and 0xFF) - ((b shr s) and 0xFF)) <= 48 }

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
