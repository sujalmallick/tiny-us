package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * A tiny bitmap font for text painted inside the world (the momo stall sign), so lettering is made
 * of the same pixel blocks as everything around it. Letters are drawn upper case; characters the
 * font doesn't know become spaces.
 */
object PixelFont {
    class Face internal constructor(val width: Int, val height: Int, val glyphs: Map<Char, List<String>>) {
        /** Width of [text] in font pixels, with one pixel between letters. */
        fun measure(text: String): Int = if (text.isEmpty()) 0 else text.length * (width + 1) - 1
    }

    /** 5x7 letters for short text. */
    val Large = Face(
        5, 7,
        mapOf(
            'A' to listOf(" ### ", "#   #", "#   #", "#####", "#   #", "#   #", "#   #"),
            'B' to listOf("#### ", "#   #", "#   #", "#### ", "#   #", "#   #", "#### "),
            'C' to listOf(" ### ", "#   #", "#    ", "#    ", "#    ", "#   #", " ### "),
            'D' to listOf("#### ", "#   #", "#   #", "#   #", "#   #", "#   #", "#### "),
            'E' to listOf("#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#####"),
            'F' to listOf("#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#    "),
            'G' to listOf(" ### ", "#   #", "#    ", "# ###", "#   #", "#   #", " ####"),
            'H' to listOf("#   #", "#   #", "#   #", "#####", "#   #", "#   #", "#   #"),
            'I' to listOf(" ### ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", " ### "),
            'J' to listOf("  ###", "   # ", "   # ", "   # ", "   # ", "#  # ", " ##  "),
            'K' to listOf("#   #", "#  # ", "# #  ", "##   ", "# #  ", "#  # ", "#   #"),
            'L' to listOf("#    ", "#    ", "#    ", "#    ", "#    ", "#    ", "#####"),
            'M' to listOf("#   #", "## ##", "# # #", "# # #", "#   #", "#   #", "#   #"),
            'N' to listOf("#   #", "#   #", "##  #", "# # #", "#  ##", "#   #", "#   #"),
            'O' to listOf(" ### ", "#   #", "#   #", "#   #", "#   #", "#   #", " ### "),
            'P' to listOf("#### ", "#   #", "#   #", "#### ", "#    ", "#    ", "#    "),
            'Q' to listOf(" ### ", "#   #", "#   #", "#   #", "# # #", "#  # ", " ## #"),
            'R' to listOf("#### ", "#   #", "#   #", "#### ", "# #  ", "#  # ", "#   #"),
            'S' to listOf(" ####", "#    ", "#    ", " ### ", "    #", "    #", "#### "),
            'T' to listOf("#####", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  "),
            'U' to listOf("#   #", "#   #", "#   #", "#   #", "#   #", "#   #", " ### "),
            'V' to listOf("#   #", "#   #", "#   #", "#   #", "#   #", " # # ", "  #  "),
            'W' to listOf("#   #", "#   #", "#   #", "# # #", "# # #", "# # #", " # # "),
            'X' to listOf("#   #", "#   #", " # # ", "  #  ", " # # ", "#   #", "#   #"),
            'Y' to listOf("#   #", "#   #", " # # ", "  #  ", "  #  ", "  #  ", "  #  "),
            'Z' to listOf("#####", "    #", "   # ", "  #  ", " #   ", "#    ", "#####"),
            '0' to listOf(" ### ", "#   #", "#  ##", "# # #", "##  #", "#   #", " ### "),
            '1' to listOf("  #  ", " ##  ", "  #  ", "  #  ", "  #  ", "  #  ", " ### "),
            '2' to listOf(" ### ", "#   #", "    #", "   # ", "  #  ", " #   ", "#####"),
            '3' to listOf("#####", "   # ", "  #  ", "   # ", "    #", "#   #", " ### "),
            '4' to listOf("   # ", "  ## ", " # # ", "#  # ", "#####", "   # ", "   # "),
            '5' to listOf("#####", "#    ", "#### ", "    #", "    #", "#   #", " ### "),
            '6' to listOf("  ## ", " #   ", "#    ", "#### ", "#   #", "#   #", " ### "),
            '7' to listOf("#####", "    #", "   # ", "  #  ", " #   ", " #   ", " #   "),
            '8' to listOf(" ### ", "#   #", "#   #", " ### ", "#   #", "#   #", " ### "),
            '9' to listOf(" ### ", "#   #", "#   #", " ####", "    #", "   # ", " ##  "),
            '.' to listOf("     ", "     ", "     ", "     ", "     ", " ##  ", " ##  "),
            ',' to listOf("     ", "     ", "     ", "     ", " ##  ", "  #  ", " #   "),
            '!' to listOf("  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "     ", "  #  "),
            '?' to listOf(" ### ", "#   #", "    #", "   # ", "  #  ", "     ", "  #  "),
            '\'' to listOf("  #  ", "  #  ", " #   ", "     ", "     ", "     ", "     "),
            '-' to listOf("     ", "     ", "     ", "#####", "     ", "     ", "     "),
            '&' to listOf(" ##  ", "#  # ", "# #  ", " #   ", "# # #", "#  # ", " ## #"),
            ':' to listOf("     ", " ##  ", " ##  ", "     ", " ##  ", " ##  ", "     "),
            '+' to listOf("     ", "  #  ", "  #  ", "#####", "  #  ", "  #  ", "     "),
            '/' to listOf("     ", "    #", "   # ", "  #  ", " #   ", "#    ", "     ")
        )
    )

    /** 3x5 letters for text too long for [Large]. */
    val Small = Face(
        3, 5,
        mapOf(
            'A' to listOf("###", "# #", "###", "# #", "# #"),
            'B' to listOf("## ", "# #", "## ", "# #", "## "),
            'C' to listOf("###", "#  ", "#  ", "#  ", "###"),
            'D' to listOf("## ", "# #", "# #", "# #", "## "),
            'E' to listOf("###", "#  ", "## ", "#  ", "###"),
            'F' to listOf("###", "#  ", "## ", "#  ", "#  "),
            'G' to listOf("###", "#  ", "# #", "# #", "###"),
            'H' to listOf("# #", "# #", "###", "# #", "# #"),
            'I' to listOf("###", " # ", " # ", " # ", "###"),
            'J' to listOf("  #", "  #", "  #", "# #", "###"),
            'K' to listOf("# #", "# #", "## ", "# #", "# #"),
            'L' to listOf("#  ", "#  ", "#  ", "#  ", "###"),
            'M' to listOf("# #", "###", "###", "# #", "# #"),
            'N' to listOf("###", "# #", "# #", "# #", "# #"),
            'O' to listOf("###", "# #", "# #", "# #", "###"),
            'P' to listOf("###", "# #", "###", "#  ", "#  "),
            'Q' to listOf("###", "# #", "# #", "###", "  #"),
            'R' to listOf("###", "# #", "## ", "# #", "# #"),
            'S' to listOf("###", "#  ", "###", "  #", "###"),
            'T' to listOf("###", " # ", " # ", " # ", " # "),
            'U' to listOf("# #", "# #", "# #", "# #", "###"),
            'V' to listOf("# #", "# #", "# #", "# #", " # "),
            'W' to listOf("# #", "# #", "###", "###", "# #"),
            'X' to listOf("# #", "# #", " # ", "# #", "# #"),
            'Y' to listOf("# #", "# #", " # ", " # ", " # "),
            'Z' to listOf("###", "  #", " # ", "#  ", "###"),
            '0' to listOf("###", "# #", "# #", "# #", "###"),
            '1' to listOf(" # ", "## ", " # ", " # ", "###"),
            '2' to listOf("###", "  #", "###", "#  ", "###"),
            '3' to listOf("###", "  #", "###", "  #", "###"),
            '4' to listOf("# #", "# #", "###", "  #", "  #"),
            '5' to listOf("###", "#  ", "###", "  #", "###"),
            '6' to listOf("###", "#  ", "###", "# #", "###"),
            '7' to listOf("###", "  #", "  #", "  #", "  #"),
            '8' to listOf("###", "# #", "###", "# #", "###"),
            '9' to listOf("###", "# #", "###", "  #", "###"),
            '.' to listOf("   ", "   ", "   ", "   ", " # "),
            ',' to listOf("   ", "   ", "   ", " # ", "#  "),
            '!' to listOf(" # ", " # ", " # ", "   ", " # "),
            '?' to listOf("###", "  #", " ##", "   ", " # "),
            '\'' to listOf(" # ", " # ", "   ", "   ", "   "),
            '-' to listOf("   ", "   ", "###", "   ", "   "),
            '&' to listOf(" # ", "# #", " # ", "# #", " ##"),
            ':' to listOf("   ", " # ", "   ", " # ", "   "),
            '+' to listOf("   ", " # ", "###", " # ", "   "),
            '/' to listOf("  #", "  #", " # ", "#  ", "#  ")
        )
    )

    /**
     * Picks the face for [text] in a box [maxWidth] font pixels wide: [Large] when it fits,
     * otherwise [Small], trimming the text if even that is too wide.
     */
    fun layout(text: String, maxWidth: Int): Pair<Face, String> {
        val upper = text.uppercase().trim()
        if (Large.measure(upper) <= maxWidth) return Large to upper
        val fits = ((maxWidth + 1) / (Small.width + 1)).coerceAtLeast(0)
        return Small to upper.take(fits).trimEnd()
    }

    /**
     * Draws [text] centred in a box [maxWidth] x [boxHeight] font pixels whose top-left is
     * ([x], [y]), one font pixel being [p] screen pixels. [shadow], if given, is drawn one pixel
     * down and right.
     */
    fun drawCentered(
        scope: DrawScope,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Int,
        boxHeight: Int,
        p: Float,
        color: Color,
        shadow: Color? = null
    ) {
        val (face, shown) = layout(text, maxWidth)
        val left = x + ((maxWidth - face.measure(shown)) / 2) * p
        val top = y + ((boxHeight - face.height) / 2) * p
        if (shadow != null) drawRun(scope, face, shown, left + p, top + p, p, shadow)
        drawRun(scope, face, shown, left, top, p, color)
    }

    private fun drawRun(scope: DrawScope, face: Face, text: String, x: Float, y: Float, p: Float, color: Color) {
        var penX = x
        for (ch in text) {
            val rows = face.glyphs[ch]
            if (rows != null) {
                for ((r, row) in rows.withIndex()) {
                    var c = 0
                    while (c < row.length) {
                        if (row[c] == '#') {
                            // Merge horizontal runs into one rectangle.
                            var end = c
                            while (end + 1 < row.length && row[end + 1] == '#') end++
                            scope.drawRect(color, Offset(penX + c * p, y + r * p), Size((end - c + 1) * p, p))
                            c = end + 1
                        } else {
                            c++
                        }
                    }
                }
            }
            penX += (face.width + 1) * p
        }
    }
}
