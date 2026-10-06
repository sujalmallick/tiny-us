"""Builds shared/src/androidMain/res/font/tiny_pixel.ttf, the Tiny Us pixel font (Plan 03, Phase 5).

Every glyph is drawn on a 5-wide grid: 7 rows from the cap line to the baseline, plus 2 rows of
descender. Each lit cell becomes a square in the outline, so the font is pixel art at any size.
The glyphs are original to Tiny Us (the capitals and digits match the in-world PixelFont).

Run:  python tools/pixelfont/build_tiny_pixel_font.py
"""
import os

from fontTools.fontBuilder import FontBuilder
from fontTools.pens.ttGlyphPen import TTGlyphPen

PX = 100            # font units per pixel
CAP = 7             # rows from cap line to baseline
EM = 1000
ADVANCE = 6 * PX    # 5 pixels + 1 pixel of spacing
SPACE = 4 * PX

# 9 rows per glyph: rows 0-6 sit on the baseline, rows 7-8 hang below it.
G = {}


def glyph(ch, *rows):
    rows = list(rows) + [""] * (9 - len(rows))
    G[ch] = [r.ljust(5) for r in rows]


# Capitals and digits (same shapes as the in-world PixelFont).
glyph("A", " ### ", "#   #", "#   #", "#####", "#   #", "#   #", "#   #")
glyph("B", "#### ", "#   #", "#   #", "#### ", "#   #", "#   #", "#### ")
glyph("C", " ### ", "#   #", "#    ", "#    ", "#    ", "#   #", " ### ")
glyph("D", "#### ", "#   #", "#   #", "#   #", "#   #", "#   #", "#### ")
glyph("E", "#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#####")
glyph("F", "#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#    ")
glyph("G", " ### ", "#   #", "#    ", "# ###", "#   #", "#   #", " ####")
glyph("H", "#   #", "#   #", "#   #", "#####", "#   #", "#   #", "#   #")
glyph("I", " ### ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", " ### ")
glyph("J", "  ###", "   # ", "   # ", "   # ", "   # ", "#  # ", " ##  ")
glyph("K", "#   #", "#  # ", "# #  ", "##   ", "# #  ", "#  # ", "#   #")
glyph("L", "#    ", "#    ", "#    ", "#    ", "#    ", "#    ", "#####")
glyph("M", "#   #", "## ##", "# # #", "# # #", "#   #", "#   #", "#   #")
glyph("N", "#   #", "#   #", "##  #", "# # #", "#  ##", "#   #", "#   #")
glyph("O", " ### ", "#   #", "#   #", "#   #", "#   #", "#   #", " ### ")
glyph("P", "#### ", "#   #", "#   #", "#### ", "#    ", "#    ", "#    ")
glyph("Q", " ### ", "#   #", "#   #", "#   #", "# # #", "#  # ", " ## #")
glyph("R", "#### ", "#   #", "#   #", "#### ", "# #  ", "#  # ", "#   #")
glyph("S", " ####", "#    ", "#    ", " ### ", "    #", "    #", "#### ")
glyph("T", "#####", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ")
glyph("U", "#   #", "#   #", "#   #", "#   #", "#   #", "#   #", " ### ")
glyph("V", "#   #", "#   #", "#   #", "#   #", "#   #", " # # ", "  #  ")
glyph("W", "#   #", "#   #", "#   #", "# # #", "# # #", "# # #", " # # ")
glyph("X", "#   #", "#   #", " # # ", "  #  ", " # # ", "#   #", "#   #")
glyph("Y", "#   #", "#   #", " # # ", "  #  ", "  #  ", "  #  ", "  #  ")
glyph("Z", "#####", "    #", "   # ", "  #  ", " #   ", "#    ", "#####")
glyph("0", " ### ", "#   #", "#  ##", "# # #", "##  #", "#   #", " ### ")
glyph("1", "  #  ", " ##  ", "  #  ", "  #  ", "  #  ", "  #  ", " ### ")
glyph("2", " ### ", "#   #", "    #", "   # ", "  #  ", " #   ", "#####")
glyph("3", "#####", "   # ", "  #  ", "   # ", "    #", "#   #", " ### ")
glyph("4", "   # ", "  ## ", " # # ", "#  # ", "#####", "   # ", "   # ")
glyph("5", "#####", "#    ", "#### ", "    #", "    #", "#   #", " ### ")
glyph("6", "  ## ", " #   ", "#    ", "#### ", "#   #", "#   #", " ### ")
glyph("7", "#####", "    #", "   # ", "  #  ", " #   ", " #   ", " #   ")
glyph("8", " ### ", "#   #", "#   #", " ### ", "#   #", "#   #", " ### ")
glyph("9", " ### ", "#   #", "#   #", " ####", "    #", "   # ", " ##  ")

# Lower case: x-height 5 rows, ascenders to the cap line, descenders 2 rows.
glyph("a", "", "", " ### ", "    #", " ####", "#   #", " ####")
glyph("b", "#    ", "#    ", "# ## ", "##  #", "#   #", "#   #", "#### ")
glyph("c", "", "", " ### ", "#    ", "#    ", "#   #", " ### ")
glyph("d", "    #", "    #", " ## #", "#  ##", "#   #", "#   #", " ####")
glyph("e", "", "", " ### ", "#   #", "#####", "#    ", " ### ")
glyph("f", "  ## ", " #  #", " #   ", "###  ", " #   ", " #   ", " #   ")
glyph("g", "", "", " ####", "#   #", "#   #", " ####", "    #", "#   #", " ### ")
glyph("h", "#    ", "#    ", "# ## ", "##  #", "#   #", "#   #", "#   #")
glyph("i", "  #  ", "", " ##  ", "  #  ", "  #  ", "  #  ", " ### ")
glyph("j", "   # ", "", "  ## ", "   # ", "   # ", "   # ", "   # ", "#  # ", " ##  ")
glyph("k", "#    ", "#    ", "#  # ", "# #  ", "##   ", "# #  ", "#  # ")
glyph("l", " ##  ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", " ### ")
glyph("m", "", "", "## # ", "# # #", "# # #", "#   #", "#   #")
glyph("n", "", "", "# ## ", "##  #", "#   #", "#   #", "#   #")
glyph("o", "", "", " ### ", "#   #", "#   #", "#   #", " ### ")
glyph("p", "", "", "#### ", "#   #", "#   #", "#### ", "#    ", "#    ", "#    ")
glyph("q", "", "", " ####", "#   #", "#   #", " ####", "    #", "    #", "    #")
glyph("r", "", "", "# ## ", "##  #", "#    ", "#    ", "#    ")
glyph("s", "", "", " ####", "#    ", " ### ", "    #", "#### ")
glyph("t", " #   ", " #   ", "###  ", " #   ", " #   ", " #  #", "  ## ")
glyph("u", "", "", "#   #", "#   #", "#   #", "#  ##", " ## #")
glyph("v", "", "", "#   #", "#   #", "#   #", " # # ", "  #  ")
glyph("w", "", "", "#   #", "#   #", "# # #", "# # #", " # # ")
glyph("x", "", "", "#   #", " # # ", "  #  ", " # # ", "#   #")
glyph("y", "", "", "#   #", "#   #", "#   #", " ####", "    #", "#   #", " ### ")
glyph("z", "", "", "#####", "   # ", "  #  ", " #   ", "#####")

# Punctuation and symbols.
glyph(".", "", "", "", "", "", " ##  ", " ##  ")
glyph(",", "", "", "", "", "", " ##  ", " ##  ", "  #  ", " #   ")
glyph("!", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "", "  #  ")
glyph("?", " ### ", "#   #", "    #", "   # ", "  #  ", "", "  #  ")
glyph("'", "  #  ", "  #  ", " #   ")
glyph('"', " # # ", " # # ")
glyph("`", " #   ", "  #  ")
glyph("-", "", "", "", "#####")
glyph("_", "", "", "", "", "", "", "", "#####")
glyph(":", "", " ##  ", " ##  ", "", " ##  ", " ##  ")
glyph(";", "", " ##  ", " ##  ", "", " ##  ", " ##  ", "  #  ", " #   ")
glyph("/", "    #", "    #", "   # ", "  #  ", " #   ", "#    ", "#    ")
glyph("\\", "#    ", "#    ", " #   ", "  #  ", "   # ", "    #", "    #")
glyph("|", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ")
glyph("(", "   # ", "  #  ", " #   ", " #   ", " #   ", "  #  ", "   # ")
glyph(")", " #   ", "  #  ", "   # ", "   # ", "   # ", "  #  ", " #   ")
glyph("[", " ### ", " #   ", " #   ", " #   ", " #   ", " #   ", " ### ")
glyph("]", " ### ", "   # ", "   # ", "   # ", "   # ", "   # ", " ### ")
glyph("{", "  ## ", " #   ", " #   ", "#    ", " #   ", " #   ", "  ## ")
glyph("}", " ##  ", "   # ", "   # ", "    #", "   # ", "   # ", " ##  ")
glyph("<", "   # ", "  #  ", " #   ", "#    ", " #   ", "  #  ", "   # ")
glyph(">", " #   ", "  #  ", "   # ", "    #", "   # ", "  #  ", " #   ")
glyph("=", "", "", "#####", "", "#####")
glyph("+", "", "  #  ", "  #  ", "#####", "  #  ", "  #  ")
glyph("*", "", "  #  ", "# # #", " ### ", "# # #", "  #  ")
glyph("&", " ##  ", "#  # ", "# #  ", " #   ", "# # #", "#  # ", " ## #")
glyph("#", " # # ", "#####", " # # ", " # # ", "#####", " # # ")
glyph("%", "##   ", "##  #", "   # ", "  #  ", " #   ", "#  ##", "   ##")
glyph("$", "  #  ", " ####", "# #  ", " ### ", "  # #", "#### ", "  #  ")
glyph("@", " ### ", "#   #", "# ###", "# # #", "# ###", "#    ", " ####")
glyph("^", "  #  ", " # # ", "#   #")
glyph("~", "", "", " #   ", "# # #", "   # ")
glyph("•", "", "", " ### ", " ### ", " ### ")          # bullet
glyph("’", "  #  ", "  #  ", " #   ")                  # right single quote
glyph("‘", "   # ", "  #  ", "  #  ")                  # left single quote
glyph("“", " # # ", "# #  ", "# #  ")                  # left double quote
glyph("”", " # # ", " # # ", "# #  ")                  # right double quote
glyph("—", "", "", "", "#####")                        # em dash
glyph("–", "", "", "", " ### ")                        # en dash
glyph("…", "", "", "", "", "", "", "# # #")            # ellipsis
glyph("·", "", "", "", "  #  ")                        # middle dot


def outline(rows):
    pen = TTGlyphPen(None)
    for r, row in enumerate(rows):
        c = 0
        while c < 5:
            if row[c] == "#":
                end = c
                while end + 1 < 5 and row[end + 1] == "#":
                    end += 1
                x0, x1 = c * PX + PX // 2, (end + 1) * PX + PX // 2   # half-pixel side bearing
                y1 = (CAP - r) * PX
                y0 = y1 - PX
                pen.moveTo((x0, y0))
                pen.lineTo((x0, y1))
                pen.lineTo((x1, y1))
                pen.lineTo((x1, y0))
                pen.closePath()
                c = end + 1
            else:
                c += 1
    return pen.glyph()


def build(out_path):
    names = {".notdef": None, "space": " "}
    for ch in G:
        names[f"uni{ord(ch):04X}"] = ch
    order = list(names.keys())
    fb = FontBuilder(EM, isTTF=True)
    fb.setupGlyphOrder(order)
    fb.setupCharacterMap({ord(ch): name for name, ch in names.items() if ch is not None})
    empty = TTGlyphPen(None).glyph()
    notdef_rows = ["#####", "#   #", "#   #", "#   #", "#   #", "#   #", "#####"] + [""] * 2
    glyphs = {".notdef": outline([r.ljust(5) for r in notdef_rows]), "space": empty}
    for name, ch in names.items():
        if ch and ch != " ":
            glyphs[name] = outline(G[ch])
    fb.setupGlyf(glyphs)
    metrics = {name: (SPACE if name == "space" else ADVANCE, 0) for name in order}
    for name, g in glyphs.items():
        g.recalcBounds(fb.font["glyf"]) if hasattr(g, "recalcBounds") else None
        metrics[name] = (metrics[name][0], getattr(g, "xMin", 0) or 0)
    fb.setupHorizontalMetrics(metrics)
    fb.setupHorizontalHeader(ascent=900, descent=-300)
    fb.setupNameTable({"familyName": "Tiny Us Pixel", "styleName": "Regular"})
    fb.setupOS2(sTypoAscender=900, sTypoDescender=-300, sTypoLineGap=0, usWinAscent=900, usWinDescent=300,
                sxHeight=500, sCapHeight=700, achVendID="TINY")
    fb.setupPost()
    fb.save(out_path)
    print("wrote", out_path, len(G), "glyphs")


if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    out = os.path.join(here, "..", "..", "shared", "src", "androidMain", "res", "font", "tiny_pixel.ttf")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    build(os.path.normpath(out))
