package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/*
 * Extra props for the background continued above or below a scene's stage on tall screens
 * (see WorldCamera), where the continued wall or floor would otherwise be plain. Drawn in world
 * units with the stage's own origin: above the stage y is negative, below it y passes worldH.
 */

/** The kitchen's taller wall: sage cabinets to match the fridge, and a shelf of jars and a plant. */
internal fun drawKitchenWallAbove(scope: DrawScope, cw: Float, wallH: Float, p: Float) {
    if (wallH < 30f * p) return
    val sage = Color(0xFFA7C4B5)
    val sageDark = Color(0xFF8BA899)
    val sageLight = Color(0xFFC3DBD0)
    val brass = Color(0xFFE0B868)
    val wood = Color(0xFF8B5A2B)
    val woodDark = Color(0xFF6B4423)

    // Upper cabinets, hung just above the stage's top edge on both sides.
    val cabH = 22f * p
    val cabTop = -cabH - 6f * p
    for ((left, doors) in listOf(cw * 0.04f to 2, cw * 0.66f to 2)) {
        val doorW = 13f * p
        val w = doors * doorW + (doors + 1) * p
        scope.drawRect(sageDark, Offset(left, cabTop), Size(w, cabH))
        scope.drawRect(sageDark, Offset(left - p, cabTop + cabH), Size(w + 2 * p, 2 * p)) // underside lip
        for (d in 0 until doors) {
            val dx = left + p + d * (doorW + p)
            scope.drawRect(sage, Offset(dx, cabTop + p), Size(doorW, cabH - 2 * p))
            scope.drawRect(sageLight, Offset(dx + p, cabTop + 2 * p), Size(doorW - 2 * p, p)) // top bevel
            val knobX = if (d % 2 == 0) dx + doorW - 3 * p else dx + p
            scope.drawRect(brass, Offset(knobX, cabTop + cabH - 7 * p), Size(2 * p, 2 * p))
        }
    }

    // A shelf in between, higher up, with jars and a little trailing plant.
    if (wallH < 60f * p) return
    val shelfY = cabTop - 20f * p
    val shelfL = cw * 0.36f
    val shelfW = cw * 0.26f
    scope.drawRect(wood, Offset(shelfL, shelfY), Size(shelfW, 2 * p))
    scope.drawRect(woodDark, Offset(shelfL, shelfY + 2 * p), Size(shelfW, p))
    scope.drawRect(woodDark, Offset(shelfL + 3 * p, shelfY + 3 * p), Size(p, 3 * p)) // brackets
    scope.drawRect(woodDark, Offset(shelfL + shelfW - 4 * p, shelfY + 3 * p), Size(p, 3 * p))
    val jarColors = listOf(Color(0xFFF2D492), Color(0xFFE8A07A), Color(0xFFCFE3D6))
    for ((i, c) in jarColors.withIndex()) {
        val jx = shelfL + 4 * p + i * 7 * p
        val jh = (6 + i % 2 * 2) * p
        scope.drawRect(Color(0xFFEDE6D6), Offset(jx, shelfY - jh), Size(5 * p, jh)) // glass
        scope.drawRect(c, Offset(jx + p, shelfY - jh + 2 * p), Size(3 * p, jh - 3 * p)) // contents
        scope.drawRect(woodDark, Offset(jx, shelfY - jh - p), Size(5 * p, p)) // lid
    }
    val potX = shelfL + shelfW - 10 * p
    scope.drawRect(Color(0xFFC7764E), Offset(potX, shelfY - 5 * p), Size(6 * p, 5 * p))
    scope.drawRect(Color(0xFFA5603E), Offset(potX, shelfY - 5 * p), Size(6 * p, p))
    val leaf = Color(0xFF5E9C61)
    scope.drawRect(leaf, Offset(potX - p, shelfY - 8 * p), Size(4 * p, 3 * p))
    scope.drawRect(leaf, Offset(potX + 3 * p, shelfY - 9 * p), Size(4 * p, 4 * p))
    scope.drawRect(leaf, Offset(potX + 5 * p, shelfY - 2 * p), Size(2 * p, 6 * p)) // trailing vine
    scope.drawRect(leaf, Offset(potX + 6 * p, shelfY + 4 * p), Size(2 * p, 3 * p))
}

/** The sunroom's larger floor: potted plants along the front and a woven basket. */
internal fun drawSunroomFloorBelow(scope: DrawScope, cw: Float, stageH: Float, floorH: Float, p: Float) {
    if (floorH < 24f * p) return
    val terracotta = Color(0xFFC7764E)
    val terracottaDark = Color(0xFFA5603E)
    val leafDark = Color(0xFF3F7A4A)
    val leaf = Color(0xFF5E9C61)
    val leafLight = Color(0xFF86BE7E)
    val base = stageH + minOf(floorH * 0.55f, 26f * p)

    fun pot(cx: Float, size: Float) {
        val w = 10f * size * p
        val h = 8f * size * p
        scope.drawRect(Color(0x33000000), Offset(cx - w / 2f - p, base - p), Size(w + 2 * p, 2 * p)) // shadow
        scope.drawRect(terracotta, Offset(cx - w / 2f, base - h), Size(w, h))
        scope.drawRect(terracottaDark, Offset(cx - w / 2f - p, base - h), Size(w + 2 * p, 2 * p)) // rim
        scope.drawRect(terracottaDark, Offset(cx + w / 2f - 2 * p, base - h + 2 * p), Size(2 * p, h - 2 * p))
        // A round, leafy plant.
        val r = 7f * size * p
        val top = base - h - r * 1.6f
        scope.drawRect(leafDark, Offset(cx - r, top + r * 0.6f), Size(2 * r, r))
        scope.drawRect(leaf, Offset(cx - r * 0.8f, top + r * 0.2f), Size(r * 1.6f, r))
        scope.drawRect(leaf, Offset(cx - r * 0.4f, top), Size(r * 0.8f, r * 0.5f))
        scope.drawRect(leafLight, Offset(cx - r * 0.5f, top + r * 0.3f), Size(r * 0.4f, r * 0.3f))
    }
    pot(cw * 0.12f, 1.1f)
    pot(cw * 0.86f, 0.9f)

    // Woven basket with a folded throw.
    val bx = cw * 0.52f
    val bw = 18f * p
    val bh = 9f * p
    scope.drawRect(Color(0x33000000), Offset(bx - p, base - p), Size(bw + 2 * p, 2 * p))
    scope.drawRect(Color(0xFFC9A36B), Offset(bx, base - bh), Size(bw, bh))
    var row = 0
    var y = base - bh + 2 * p
    while (y < base - p) {
        var x = bx + (if (row % 2 == 0) p else 3 * p)
        while (x < bx + bw - 2 * p) {
            scope.drawRect(Color(0xFFA9824C), Offset(x, y), Size(2 * p, p)) // weave
            x += 4 * p
        }
        y += 2 * p
        row++
    }
    scope.drawRect(Color(0xFFE8B4BC), Offset(bx + 2 * p, base - bh - 3 * p), Size(bw - 4 * p, 3 * p)) // throw
    scope.drawRect(Color(0xFFF5D3D8), Offset(bx + 3 * p, base - bh - 3 * p), Size(bw - 8 * p, p))
}

/** The cafe's ceiling above its brick wall: dark planks, rafters, and two hanging plants. */
internal fun drawCafeCeilingAbove(scope: DrawScope, cw: Float, ceilingH: Float, p: Float) {
    if (ceilingH < 10f * p) return
    val seam = Color(0xFF2E1F16)
    val rafter = Color(0xFF4A3426)
    val rafterDark = Color(0xFF33231A)
    // Plank seams running across.
    var y = -6f * p
    while (y > -ceilingH) {
        scope.drawRect(seam, Offset(0f, y), Size(cw, p))
        y -= 7f * p
    }
    // Rafters coming toward the viewer, in line with the stubs at the top of the wall.
    var x = cw * 0.045f
    while (x < cw) {
        scope.drawRect(rafter, Offset(x - 2f * p, -ceilingH), Size(5f * p, ceilingH))
        scope.drawRect(rafterDark, Offset(x + 2f * p, -ceilingH), Size(p, ceilingH))
        x += cw * 0.19f
    }
    if (ceilingH < 34f * p) return
    // Macrame hangers with trailing plants.
    for ((fx, drop) in listOf(0.30f to 0.55f, 0.74f to 0.40f)) {
        val cx = cw * fx
        val potY = -ceilingH * (1f - drop)
        val cord = Color(0xFFD9C3A0)
        scope.drawRect(cord, Offset(cx, -ceilingH), Size(p, potY + ceilingH - 3f * p))
        scope.drawRect(cord, Offset(cx - 3f * p, potY - 4f * p), Size(p, 4f * p))
        scope.drawRect(cord, Offset(cx + 3f * p, potY - 4f * p), Size(p, 4f * p))
        scope.drawRect(Color(0xFFC7764E), Offset(cx - 4f * p, potY), Size(9f * p, 6f * p))
        scope.drawRect(Color(0xFFA5603E), Offset(cx - 4f * p, potY), Size(9f * p, p))
        val leaf = Color(0xFF5E9C61)
        val leafDark = Color(0xFF3F7A4A)
        scope.drawRect(leaf, Offset(cx - 6f * p, potY - 2f * p), Size(13f * p, 3f * p))
        scope.drawRect(leafDark, Offset(cx - 6f * p, potY + 2f * p), Size(2f * p, 8f * p)) // trailing vines
        scope.drawRect(leaf, Offset(cx + 5f * p, potY + 3f * p), Size(2f * p, 10f * p))
        scope.drawRect(leafDark, Offset(cx + 1f * p, potY + 6f * p), Size(2f * p, 6f * p))
    }
}

/** The bedroom's taller wall: a heart garland, a framed photo, and a shelf with books and a plant. */
internal fun drawBedroomWallAbove(scope: DrawScope, cw: Float, wallH: Float, p: Float) {
    if (wallH < 24f * p) return
    val wood = Color(0xFF8B5A2B)
    val woodDark = Color(0xFF6B4423)
    // Shelf just above the room, on the left.
    val shelfY = -10f * p
    val shelfL = cw * 0.08f
    val shelfW = cw * 0.30f
    scope.drawRect(wood, Offset(shelfL, shelfY), Size(shelfW, 2f * p))
    scope.drawRect(woodDark, Offset(shelfL, shelfY + 2f * p), Size(shelfW, p))
    scope.drawRect(woodDark, Offset(shelfL + 3f * p, shelfY + 3f * p), Size(p, 3f * p))
    scope.drawRect(woodDark, Offset(shelfL + shelfW - 4f * p, shelfY + 3f * p), Size(p, 3f * p))
    val books = listOf(Color(0xFFE88AA8), Color(0xFF6E92C4), Color(0xFFF2D492), Color(0xFF86BE7E))
    for ((i, c) in books.withIndex()) {
        val h = (7 + (i % 2) * 2) * p
        scope.drawRect(c, Offset(shelfL + 3f * p + i * 3f * p, shelfY - h), Size(3f * p, h))
        scope.drawRect(Color(0x33000000), Offset(shelfL + 5f * p + i * 3f * p, shelfY - h), Size(p, h))
    }
    val potX = shelfL + shelfW - 12f * p
    scope.drawRect(Color(0xFFF4F1EA), Offset(potX, shelfY - 5f * p), Size(6f * p, 5f * p))
    scope.drawRect(Color(0xFF5E9C61), Offset(potX - p, shelfY - 9f * p), Size(8f * p, 4f * p))
    scope.drawRect(Color(0xFF3F7A4A), Offset(potX + 2f * p, shelfY - 11f * p), Size(2f * p, 3f * p))
    scope.drawRect(Color(0xFFF4F1EA), Offset(potX + 8f * p, shelfY - 4f * p), Size(3f * p, 4f * p)) // candle
    scope.drawRect(Color(0xFFF6BD60), Offset(potX + 9f * p, shelfY - 6f * p), Size(p, 2f * p))

    // Framed photo of the two of them, on the right.
    if (wallH >= 40f * p) {
        val fx = cw * 0.66f
        val fy = -32f * p
        scope.drawRect(woodDark, Offset(fx, fy), Size(16f * p, 13f * p))
        scope.drawRect(Color(0xFFFCEDEF), Offset(fx + p, fy + p), Size(14f * p, 11f * p))
        scope.drawRect(Color(0xFFA9D6E5), Offset(fx + 2f * p, fy + 2f * p), Size(12f * p, 5f * p)) // sky
        scope.drawRect(Color(0xFF86BE7E), Offset(fx + 2f * p, fy + 7f * p), Size(12f * p, 4f * p)) // grass
        scope.drawRect(Color(0xFF2A2829), Offset(fx + 5f * p, fy + 4f * p), Size(2f * p, 2f * p)) // two little heads
        scope.drawRect(Color(0xFF6A381F), Offset(fx + 9f * p, fy + 4f * p), Size(2f * p, 2f * p))
        scope.drawRect(Color(0xFFE88AA8), Offset(fx + 7f * p, fy + 3f * p), Size(2f * p, p))
    }

    // A garland of little hearts across the top of the wall.
    if (wallH >= 52f * p) {
        val gy = -wallH + 10f * p
        val string = Color(0xFFB79A82)
        var x = 0f
        var i = 0
        while (x < cw) {
            val sag = if (i % 2 == 0) 0f else 2f * p
            scope.drawRect(string, Offset(x, gy + sag), Size(cw / 10f, p))
            val heart = if (i % 3 == 0) Color(0xFFE88AA8) else if (i % 3 == 1) Color(0xFFF5B3C8) else Color(0xFFF6BD60)
            val hx = x + cw / 20f
            scope.drawRect(heart, Offset(hx - 2f * p, gy + sag + p), Size(2f * p, 2f * p))
            scope.drawRect(heart, Offset(hx + p, gy + sag + p), Size(2f * p, 2f * p))
            scope.drawRect(heart, Offset(hx - 2f * p, gy + sag + 3f * p), Size(5f * p, p))
            scope.drawRect(heart, Offset(hx - p, gy + sag + 4f * p), Size(3f * p, p))
            scope.drawRect(heart, Offset(hx, gy + sag + 5f * p), Size(p, p))
            x += cw / 10f
            i++
        }
    }
}
