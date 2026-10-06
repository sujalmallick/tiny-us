package com.example

import com.example.ui.IosPolaroidCard
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Surface
import kotlin.test.Test
import kotlin.test.assertEquals

/** The printed Polaroid card draws on the simulator, at Android's size and in its paper colour. */
class IosPolaroidCardTest {
    @Test
    fun aScreenshotBecomesAPrintedCard() {
        val shot = Surface.makeRasterN32Premul(390, 844).apply {
            canvas.drawPaint(Paint().apply { color = 0xFF88AACC.toInt() })
        }.makeImageSnapshot()
        val card = IosPolaroidCard.render(shot, "Salt and Sunset", "Oct 7, 2026", "6:42 PM", "Seaside Pier")
        assertEquals(IosPolaroidCard.WIDTH, card.width)
        assertEquals(IosPolaroidCard.HEIGHT, card.height)
        val pixels = Bitmap.makeFromImage(card)
        assertEquals(0xFFFAF8F5.toInt(), pixels.getColor(30, 30), "paper")
        assertEquals(0xFF88AACC.toInt(), pixels.getColor(540, 500), "the photo")
    }
}
