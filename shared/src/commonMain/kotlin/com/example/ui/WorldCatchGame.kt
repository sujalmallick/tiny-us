package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.games.CatchGame

/** "Catch together" (plan 07, C1): the woven basket on the ground and the golden stars falling. */
fun drawCatchGame(scope: DrawScope, game: CatchGame, cw: Float, ch: Float, p: Float, time: Float) {
    if (!game.active) return
    // Golden stars, twinkling as they fall.
    for (g in game.goldens) {
        val x = g.x * cw
        val y = g.y * ch
        val gold = Color(0xFFFFD166)
        val glint = if ((time * 6f + g.x * 10f).toInt() % 2 == 0) Color(0xFFFFF3B0) else gold
        scope.drawCircle(gold.copy(alpha = 0.25f), 6f * p, Offset(x, y))
        scope.drawRect(gold, Offset(x - p, y - 3f * p), Size(2f * p, 6f * p))
        scope.drawRect(gold, Offset(x - 3f * p, y - p), Size(6f * p, 2f * p))
        scope.drawRect(glint, Offset(x - p, y - p), Size(2f * p, 2f * p))
    }
    // The basket: its rim is the catching line.
    val cx = game.basketX * cw
    val halfW = CatchGame.HALF_WIDTH * cw
    val rim = CatchGame.RIM_Y * ch
    val h = 10f * p
    val wicker = Color(0xFFC9A36B)
    val wickerDark = Color(0xFFA9824C)
    scope.drawRect(Color(0x33000000), Offset(cx - halfW, rim + h - p), Size(2f * halfW, 2f * p)) // shadow
    scope.drawRect(wicker, Offset(cx - halfW, rim), Size(2f * halfW, h))
    var row = 0
    var y = rim + 2f * p
    while (y < rim + h - p) {
        var x = cx - halfW + (if (row % 2 == 0) p else 3f * p)
        while (x < cx + halfW - 2f * p) {
            scope.drawRect(wickerDark, Offset(x, y), Size(2f * p, p))
            x += 4f * p
        }
        y += 2f * p
        row++
    }
    scope.drawRect(wickerDark, Offset(cx - halfW - p, rim - p), Size(2f * halfW + 2f * p, 2f * p)) // rim
    // A handle arching over.
    scope.drawRect(wickerDark, Offset(cx - halfW * 0.6f, rim - 6f * p), Size(p, 5f * p))
    scope.drawRect(wickerDark, Offset(cx + halfW * 0.6f - p, rim - 6f * p), Size(p, 5f * p))
    scope.drawRect(wickerDark, Offset(cx - halfW * 0.6f, rim - 7f * p), Size(halfW * 1.2f, p))
}
