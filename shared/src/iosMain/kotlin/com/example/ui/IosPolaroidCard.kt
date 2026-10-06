package com.example.ui

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Typeface

/**
 * The printed Polaroid card on iOS, drawn like Android's PolaroidManager.renderPolaroidCard: a warm
 * 1080 x 1440 instant-film card, the scene cropped into the photo, the title in bold serif, the
 * scene and date, the time, and the "TINY US" mark with a small pixel heart.
 */
object IosPolaroidCard {
    const val WIDTH = 1080
    const val HEIGHT = 1440

    /** 7x6 pixel heart for the watermark ('X' = filled cell). */
    private val PIXEL_HEART = listOf(".XX.XX.", "XXXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X...")

    /** The named font, or null for Skia's default (a Font takes null as "the default typeface"). */
    private fun typeface(family: String, style: FontStyle): Typeface? =
        FontMgr.default.matchFamilyStyle(family, style)

    private fun fill(argb: Long) = Paint().apply { color = argb.toInt(); isAntiAlias = true; mode = PaintMode.FILL }

    private fun stroke(argb: Long, width: Float) = Paint().apply { color = argb.toInt(); isAntiAlias = true; mode = PaintMode.STROKE; strokeWidth = width }

    private fun Canvas.centered(text: String, y: Float, font: Font, paint: Paint) {
        drawString(text, WIDTH / 2f - font.measureTextWidth(text, paint) / 2f, y, font, paint)
    }

    fun render(scene: Image, title: String, date: String, time: String, sceneName: String): Image {
        val surface = Surface.makeRasterN32Premul(WIDTH, HEIGHT)
        val canvas = surface.canvas
        val corner = 24f

        // 1. Warm instant-film paper
        canvas.drawRRect(RRect.makeXYWH(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), corner), fill(0xFFFAF8F5))

        // 2. The photo: 960 x 980 at (60, 60), the scene cropped around the couple
        val photo = Rect.makeLTRB(60f, 60f, 1020f, 1040f)
        canvas.drawRect(photo, fill(0xFF1A1A1A))
        val sw = scene.width
        val sh = scene.height
        val targetRatio = photo.width / photo.height
        val src = if (sw.toFloat() / sh > targetRatio) {
            val cropW = (sh * targetRatio).toInt().coerceAtMost(sw)
            val x = (sw - cropW) / 2
            Rect.makeXYWH(x.toFloat(), 0f, cropW.toFloat(), sh.toFloat())
        } else {
            val cropH = (sw / targetRatio).toInt().coerceAtMost(sh)
            val y = ((sh - cropH) * 0.48f).toInt().coerceIn(0, (sh - cropH).coerceAtLeast(0))
            Rect.makeXYWH(0f, y.toFloat(), sw.toFloat(), cropH.toFloat())
        }
        canvas.drawImageRect(scene, src, photo, SamplingMode.LINEAR, Paint().apply { isAntiAlias = true }, true)
        canvas.drawRect(photo, stroke(0xFFDDD7CE, 2f))

        // 3. The caption
        val titleFont = Font(typeface("Georgia", FontStyle.BOLD), 48f)
        val titlePaint = fill(0xFF1E293B)
        while (titleFont.measureTextWidth(title, titlePaint) > WIDTH - 140f && titleFont.size > 30f) {
            titleFont.size -= 2f
        }
        canvas.centered(title, 1145f, titleFont, titlePaint)

        val dateText = if (sceneName.isNotBlank() && date.isNotBlank()) "$sceneName  •  $date" else sceneName.ifBlank { date }
        canvas.centered(dateText, 1215f, Font(typeface("Georgia", FontStyle.NORMAL), 28f), fill(0xFF64748B))

        if (time.isNotBlank()) {
            canvas.centered(time, 1270f, Font(typeface("Menlo", FontStyle.NORMAL), 22f), fill(0xFF94A3B8))
        }

        // The "TINY US" mark, a pixel heart and the letters centred together
        val brandFont = Font(typeface("Helvetica Neue", FontStyle.NORMAL), 20f)
        val brandPaint = fill(0xFFE5989B)
        val brandText = "T I N Y   U S"
        val brandWidth = brandFont.measureTextWidth(brandText, brandPaint)
        val cell = 3f
        val heartWidth = 7 * cell
        val gap = 14f
        val startX = WIDTH / 2f - (heartWidth + gap + brandWidth) / 2f
        val heartTop = 1370f - 17f
        val heartPaint = Paint().apply { color = brandPaint.color; mode = PaintMode.FILL }
        PIXEL_HEART.forEachIndexed { row, line ->
            line.forEachIndexed { col, c ->
                if (c == 'X') {
                    val x = startX + col * cell
                    val y = heartTop + row * cell
                    canvas.drawRect(Rect.makeLTRB(x, y, x + cell, y + cell), heartPaint)
                }
            }
        }
        canvas.drawString(brandText, startX + heartWidth + gap, 1370f, brandFont, brandPaint)

        // The card's edge
        canvas.drawRRect(RRect.makeLTRB(1f, 1f, WIDTH - 1f, HEIGHT - 1f, corner), stroke(0xFFE8E2D8, 2f))

        return surface.makeImageSnapshot()
    }
}
