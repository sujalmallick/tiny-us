package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.TinyColors
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plan 06, I3: the text colours meet WCAG AA contrast on the backgrounds they're used on: 4.5:1
 * for body text, 3:1 for large or bold labels (titles, chips, buttons, tags).
 */
class ContrastTest {

    private fun ratio(a: Color, b: Color): Float {
        val l1 = maxOf(a.luminance(), b.luminance())
        val l2 = minOf(a.luminance(), b.luminance())
        return (l1 + 0.05f) / (l2 + 0.05f)
    }

    private fun check(name: String, text: Color, background: Color, minimum: Float, failures: MutableList<String>) {
        val r = ratio(text, background)
        if (r < minimum) failures += "$name: ${"%.2f".format(r)}:1 (needs $minimum:1)"
    }

    @Test
    fun textColoursMeetWcagAa() {
        val failures = mutableListOf<String>()
        // Body text on the dialog surfaces.
        for ((bgName, bg) in listOf("Paper" to TinyColors.Paper, "Card" to TinyColors.Card, "Muted" to TinyColors.Muted)) {
            check("Ink on $bgName", TinyColors.Ink, bg, 4.5f, failures)
            check("InkMuted on $bgName", TinyColors.InkMuted, bg, 4.5f, failures)
            check("Rose on $bgName", TinyColors.Rose, bg, 4.5f, failures)
        }
        // Selected chips and tags: bold labels.
        check("White on Rose (selected chip, button)", Color.White, TinyColors.Rose, 4.5f, failures)
        check("Rose on RoseSoft (selected option)", TinyColors.Rose, TinyColors.RoseSoft, 3f, failures)
        check("Sage on SageSoft (done tag)", TinyColors.Sage, TinyColors.SageSoft, 3f, failures)
        check("Plum on PlumSoft", TinyColors.Plum, TinyColors.PlumSoft, 3f, failures)
        // The hint toast: white text on the dark scrim at 70%, over a white sky at worst.
        val toast = Color(
            red = TinyColors.Scrim.red * 0.7f + 0.3f, green = TinyColors.Scrim.green * 0.7f + 0.3f, blue = TinyColors.Scrim.blue * 0.7f + 0.3f
        )
        check("White on the hint toast over a white sky", Color.White, toast, 4.5f, failures)
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
