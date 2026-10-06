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

/**
 * The sunroom's floor continued below the stage on tall screens. The tiles carry on in
 * perspective, the corner leaves at the stage's edge get the planters they grow from, and the
 * floor holds a basket, a runner rug with seed trays and a few fallen leaves, so it never reads
 * as a plain orange band.
 */
fun drawSunroomFloorBelow(
    scope: DrawScope,
    cw: Float,
    stageH: Float,
    floorH: Float,
    p: Float,
    night: Boolean = false,
    sunny: Boolean = true
) {
    if (floorH <= 0f) return
    val bottom = stageH + floorH
    // Same rows as the stage's floor (it starts at 0.645 of the stage height).
    drawSunroomTiles(scope, cw, stageH * 0.645f, stageH, bottom, p)
    if (floorH < 16f * p) return

    val shadow = Color(0x33000000)
    fun rect(c: Color, x: Float, y: Float, w: Float, h: Float) {
        if (w > 0f && h > 0f) scope.drawRect(c, Offset(x, y), Size(w, h))
    }

    // Sunlight from the glass falls across the near floor too.
    if (sunny) {
        // Slanted bands, stepped like the light through the glass panes.
        val glow = Color(0xFFFFF2B2).copy(alpha = 0.12f)
        val stepH = 4f * p
        val steps = (floorH * 0.24f / stepH).toInt()
        for (b in 0..2) {
            val x0 = cw * (0.08f + b * 0.30f)
            for (k in 0 until steps) {
                rect(glow, x0 + k * 2f * p, stageH + floorH * 0.16f + k * stepH, cw * 0.14f, stepH)
            }
        }
    }

    // Planters under the big corner leaves, so the leaves grow from something.
    val planterH = minOf(24f * p, floorH * 0.42f)
    for (right in listOf(false, true)) {
        val w = 56f * p
        val x = if (right) cw - w + 4f * p else -4f * p
        rect(shadow, x - p, stageH + planterH - p, w + 2f * p, 3f * p)
        rect(Color(0xFFB9694A), x, stageH, w, planterH)
        rect(Color(0xFF8E4E33), x, stageH, w, 3f * p) // rim
        rect(Color(0xFFD08458), x + 2f * p, stageH + 5f * p, w - 4f * p, p) // highlight band
        rect(Color(0xFF9D5A3C), x, stageH + planterH - 3f * p, w, 3f * p) // foot
        // Pressed pattern of little diamonds on the front
        var dx = x + 6f * p
        while (dx < x + w - 4f * p) {
            rect(Color(0xFFA5603E), dx, stageH + planterH * 0.55f, 2f * p, 2f * p)
            dx += 9f * p
        }
    }

    // Woven basket with a folded throw, between the planters.
    val basketBase = stageH + minOf(floorH * 0.30f, 20f * p)
    run {
        val bw = 20f * p
        val bh = 10f * p
        val bx = cw * 0.5f - bw / 2f
        rect(shadow, bx - p, basketBase - p, bw + 2f * p, 2f * p)
        rect(Color(0xFFC9A36B), bx, basketBase - bh, bw, bh)
        var row = 0
        var y = basketBase - bh + 2f * p
        while (y < basketBase - p) {
            var x = bx + (if (row % 2 == 0) p else 3f * p)
            while (x < bx + bw - 2f * p) {
                rect(Color(0xFFA9824C), x, y, 2f * p, p) // weave
                x += 4f * p
            }
            y += 2f * p
            row++
        }
        rect(Color(0xFFE8B4BC), bx + 2f * p, basketBase - bh - 3f * p, bw - 4f * p, 3f * p) // throw
        rect(Color(0xFFF5D3D8), bx + 3f * p, basketBase - bh - 3f * p, bw - 8f * p, p)
    }
    if (floorH < 48f * p) {
        if (night) rect(Color(0x1A05070F), 0f, stageH, cw, floorH)
        return
    }

    // A striped runner rug across the near floor.
    val rugTop = stageH + floorH * 0.50f
    val rugH = minOf(26f * p, floorH * 0.26f)
    val rugX = cw * 0.10f
    val rugW = cw * 0.80f
    rect(shadow, rugX, rugTop + rugH, rugW, 2f * p)
    rect(Color(0xFFD9C29A), rugX, rugTop, rugW, rugH)
    rect(Color(0xFFB98B5E), rugX, rugTop + 2f * p, rugW, 2f * p)
    rect(Color(0xFFB98B5E), rugX, rugTop + rugH - 4f * p, rugW, 2f * p)
    rect(Color(0xFF84A59D), rugX, rugTop + rugH * 0.5f - p, rugW, 2f * p)
    var fx = rugX
    while (fx < rugX + rugW) { // fringe at both ends
        rect(Color(0xFFE9D8A6), fx, rugTop - 2f * p, p, 2f * p)
        rect(Color(0xFFE9D8A6), fx, rugTop + rugH, p, 2f * p)
        fx += 3f * p
    }

    // Seed trays with sprouts on the rug.
    for ((i, tx) in listOf(cw * 0.20f, cw * 0.20f + 22f * p).withIndex()) {
        val ty = rugTop + rugH * 0.5f - (if (i == 0) 2f else 1f) * p
        rect(shadow, tx - p, ty + 7f * p, 20f * p, 2f * p)
        rect(Color(0xFF5B4636), tx, ty, 18f * p, 7f * p)
        rect(Color(0xFF7A5C45), tx, ty, 18f * p, p)
        var sx = tx + 2f * p
        var n = 0
        while (sx < tx + 16f * p) {
            val h = (2f + (n * 7 % 3)) * p
            rect(Color(0xFF6BAA5E), sx, ty - h + p, p, h)
            rect(Color(0xFF8FCB7A), sx - p, ty - h + p, p, p)
            rect(Color(0xFF8FCB7A), sx + p, ty - h + 2f * p, p, p)
            sx += 4f * p
            n++
        }
    }

    // A stack of garden books with a trowel resting on top.
    run {
        val bx = cw * 0.62f
        val by = rugTop + rugH * 0.5f + 4f * p
        rect(shadow, bx - p, by, 22f * p, 2f * p)
        rect(Color(0xFF4A7C9B), bx, by - 4f * p, 20f * p, 4f * p)
        rect(Color(0xFFFFF8E7), bx + 18f * p, by - 3f * p, 2f * p, 2f * p)
        rect(Color(0xFFE76F51), bx + 2f * p, by - 8f * p, 17f * p, 4f * p)
        rect(Color(0xFFFFF8E7), bx + 17f * p, by - 7f * p, 2f * p, 2f * p)
        rect(Color(0xFFFFD166), bx + 5f * p, by - 7f * p, 6f * p, p) // title
        rect(Color(0xFF8B5A2B), bx + 4f * p, by - 10f * p, 7f * p, 2f * p) // trowel handle
        rect(Color(0xFF9DB4BC), bx + 11f * p, by - 11f * p, 6f * p, 3f * p) // blade
    }

    // Fallen leaves and petals scattered nearer the viewer.
    val scatter = listOf(
        0.08f to 0.80f, 0.27f to 0.88f, 0.45f to 0.83f, 0.58f to 0.93f, 0.74f to 0.86f, 0.90f to 0.80f, 0.36f to 0.42f, 0.70f to 0.40f
    )
    for ((i, xy) in scatter.withIndex()) {
        val c = when (i % 3) { 0 -> Color(0xFF6BAA5E); 1 -> Color(0xFFE9C46A); else -> Color(0xFFF4A3B5) }
        val lx = cw * xy.first
        val ly = stageH + floorH * xy.second
        rect(c, lx, ly, 3f * p, 2f * p)
        rect(c, lx + 2f * p, ly - p, 2f * p, p)
    }

    // The floor nearest the viewer falls into soft shade.
    rect(Color(0xFF2B1A12).copy(alpha = 0.06f), 0f, bottom - floorH * 0.22f, cw, floorH * 0.22f)
    rect(Color(0xFF2B1A12).copy(alpha = 0.08f), 0f, bottom - floorH * 0.10f, cw, floorH * 0.10f)
    if (night) rect(Color(0x1A05070F), 0f, stageH, cw, floorH)
}

/** The cafe's ceiling above its brick wall: dark planks, rafters, and two hanging plants. */
fun drawCafeCeilingAbove(scope: DrawScope, cw: Float, ceilingH: Float, p: Float) {
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
fun drawBedroomWallAbove(scope: DrawScope, cw: Float, wallH: Float, p: Float) {
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

/**
 * The kitchen's checkerboard floor carried on below the stage: the same tiles, rows and colours as
 * the floor drawn in the room, so the pattern runs on without a seam.
 */
fun drawKitchenFloorBelow(scope: DrawScope, cw: Float, stageH: Float, floorH: Float, p: Float) {
    val tile = com.example.scene.KitchenLayout.FLOOR_TILE * p
    val a = com.example.scene.KitchenLayout.FLOOR_TILE_A
    val b = com.example.scene.KitchenLayout.FLOOR_TILE_B
    var row = 0
    var y = com.example.scene.KitchenLayout.floorY(stageH)
    while (y < stageH + floorH) {
        if (y + tile > stageH) {
            var col = 0
            var x = 0f
            while (x < cw) {
                scope.drawRect(if ((row + col) % 2 == 0) a else b, Offset(x, y), Size(tile, tile))
                x += tile
                col++
            }
        }
        y += tile
        row++
    }
}

/**
 * The cozy loft's sloped timber roof above the stage. The room's posts end at a header beam, and
 * above it the plank ceiling runs on with rafters parallel to the room's diagonal beam and a small
 * skylight between two of them. [stageH] is the stage's height (the room's `ch`).
 */
fun drawLoftCeilingAbove(
    scope: DrawScope,
    cw: Float,
    stageH: Float,
    ceilingH: Float,
    p: Float,
    isNight: Boolean,
    isSunset: Boolean
) {
    if (ceilingH < 2f * p) return
    val rafter = com.example.engine.LoftSprites.RafterWood
    val beam = com.example.engine.LoftSprites.DarkWoodBeam
    val beamLight = com.example.engine.LoftSprites.PlankWoodLight
    scope.drawRect(rafter, Offset(0f, -ceilingH), Size(cw, ceilingH))
    // Plank seams, in step with the ones in the room's own ceiling.
    var y = -4.5f * p
    while (y > -ceilingH) {
        scope.drawRect(Color(0x35000000), Offset(0f, y), Size(cw, 1.2f * p))
        y -= 4.5f * p
    }

    // The room's main beam runs from (0, 1% of the stage) down to (cw, 25%); the rafters above follow it.
    val slope = stageH * 0.24f / cw
    val mainY0 = stageH * 0.01f
    val gap = 28f * p
    val thick = 6f * p
    val steps = 48
    val stepW = cw / steps
    fun rafterTop(k: Int, x: Float) = mainY0 + x * slope - k * gap
    // Skylight between the second and third rafters, on the right.
    if (ceilingH > 40f * p) {
        val glass = when {
            isNight -> Color(0xFF141B3A)
            isSunset -> Color(0xFFF4A97A)
            else -> Color(0xFF9CCBEA)
        }
        val left = cw * 0.56f
        val right = cw * 0.86f
        var x = left
        while (x < right) {
            val top = maxOf(rafterTop(3, x) + thick, -ceilingH)
            val bottom = minOf(rafterTop(2, x), 0f)
            if (bottom > top) {
                scope.drawRect(glass, Offset(x, top), Size(stepW + 0.5f, bottom - top))
            }
            x += stepW
        }
        // Frame posts at each end and a glint (or a star by night).
        for (fx in listOf(left, (left + right) / 2f, right)) {
            val top = maxOf(rafterTop(3, fx) + thick, -ceilingH)
            val bottom = minOf(rafterTop(2, fx), 0f)
            if (bottom > top) scope.drawRect(beam, Offset(fx - p, top), Size(2f * p, bottom - top))
        }
        val gx = left + (right - left) * 0.28f
        val gy = rafterTop(3, gx) + thick + 3f * p
        if (gy > -ceilingH && gy < minOf(rafterTop(2, gx), 0f) - 2f * p) {
            scope.drawRect(if (isNight) Color(0xFFFFF3C4) else Color(0xCCFFFFFF), Offset(gx, gy), Size(p, p))
            scope.drawRect(if (isNight) Color(0xFFFFF3C4) else Color(0x99FFFFFF), Offset(gx + 3f * p, gy + 2f * p), Size(p, p))
        }
    }
    // Rafters.
    var k = 1
    while (rafterTop(k, cw) > -ceilingH) {
        for (i in 0 until steps) {
            val sx = i * stepW
            val top = rafterTop(k, sx)
            val bottom = minOf(top + thick, 0f)
            if (bottom <= -ceilingH || top >= 0f) continue
            val t = maxOf(top, -ceilingH)
            scope.drawRect(beam, Offset(sx, t), Size(stepW + 0.5f, bottom - t))
            val edge = top + thick - 1.5f * p
            if (edge < 0f && edge > -ceilingH) scope.drawRect(beamLight, Offset(sx, edge), Size(stepW + 0.5f, 1.5f * p))
        }
        k++
    }
    // Header beam where the room's posts end.
    val headerH = minOf(6f * p, ceilingH)
    scope.drawRect(beam, Offset(0f, -headerH), Size(cw, headerH))
    scope.drawRect(beamLight, Offset(0f, -1.5f * p), Size(cw, 1.5f * p))
}
