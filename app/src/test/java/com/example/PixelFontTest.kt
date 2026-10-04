package com.example

import com.example.engine.PixelFont
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelFontTest {
    @Test
    fun everyGlyphHasTheFaceSize() {
        for (face in listOf(PixelFont.Large, PixelFont.Small)) {
            for ((ch, rows) in face.glyphs) {
                assertEquals("rows of '$ch'", face.height, rows.size)
                assertTrue("width of '$ch'", rows.all { it.length == face.width })
            }
        }
    }

    @Test
    fun bothFacesCoverLettersDigitsAndCommonPunctuation() {
        val wanted = ('A'..'Z') + ('0'..'9') + ".,!?'-&:+/".toList()
        for (face in listOf(PixelFont.Large, PixelFont.Small)) {
            val missing = wanted.filter { it !in face.glyphs }
            assertTrue("missing $missing", missing.isEmpty())
        }
    }

    @Test
    fun shortTextUsesTheLargeFaceInUpperCase() {
        val (face, text) = PixelFont.layout("Warm Bites", 59)
        assertSame(PixelFont.Large, face)
        assertEquals("WARM BITES", text)
        assertEquals(59, face.measure(text))
    }

    @Test
    fun longTextFallsBackToTheSmallFaceAndIsTrimmedToFit() {
        val (face, text) = PixelFont.layout("Momo and Dumpling House", 59)
        assertSame(PixelFont.Small, face)
        assertTrue(face.measure(text) <= 59)
        assertEquals("MOMO AND DUMPLI", text)
    }
}
