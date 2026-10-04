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
