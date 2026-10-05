package com.example.engine

import kotlin.math.floor

/**
 * Sprite animation timing for the pixel renderer (Plan 03, Phase 4): animations advance in whole
 * frames at [FPS], like a pixel game, while movement still updates every display frame.
 */
object SpriteClock {
    const val FPS = 12f

    /** [seconds] rounded down to the start of its sprite frame. */
    fun step(seconds: Float): Float = floor(seconds * FPS) / FPS
}

/**
 * Scene-change dissolve on a frame of ARGB pixels: cells of [CELL] x [CELL] pixels switch to the
 * wipe colour in a 4x4 ordered (Bayer) pattern as [amount] goes from 0 to 1.
 */
object PixelDissolve {
    const val CELL = 2
    private val BAYER = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)

    fun apply(px: IntArray, w: Int, h: Int, amount: Float, colour: Int) {
        if (amount <= 0f) return
        val level = (amount.coerceAtMost(1f) * 16f)
        for (y in 0 until h) {
            val by = (y / CELL) and 3
            val row = y * w
            for (x in 0 until w) {
                if (BAYER[by * 4 + ((x / CELL) and 3)] < level) px[row + x] = colour
            }
        }
    }
}
