package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.games.Constellation
import com.example.games.StarPuzzle
import kotlin.math.sin

/** Stargazing (plan 07, C2): the constellation being connected, or one glowing when found. */
internal fun drawStarPuzzle(scope: DrawScope, puzzle: StarPuzzle, cw: Float, ch: Float, p: Float, time: Float, drift: Float) {
    puzzle.current?.let { c ->
        // Lines between the stars connected so far.
        for (i in 1 until puzzle.connected) line(scope, puzzle, c, i - 1, i, cw, ch, p, drift, Color(0xFFFFE8A3), 1f)
        c.stars.indices.forEach { i ->
            val x = puzzle.starX(c, i, drift) * cw + wiggle(puzzle, i, time, p)
            val y = c.stars[i].second * ch
            when {
                i < puzzle.connected -> star(scope, x, y, p, Color.White, arms = 1)
                // The next star is gold, its arms pulsing out and back.
                i == puzzle.connected -> star(scope, x, y, p, Color(0xFFFFD166), arms = if (sin(time * 6f) > 0f) 2 else 1)
                else -> star(scope, x, y, p, Color(0xFFB8C8E8), arms = 0)
            }
        }
    }
    puzzle.glowing?.let { c ->
        val a = (puzzle.glowTimer / StarPuzzle.GLOW_SECONDS).coerceIn(0f, 1f)
        val n = c.stars.size
        for (i in 1 until n) line(scope, puzzle, c, i - 1, i, cw, ch, p, drift, Color(0xFFFFE8A3), a)
        if (c.closed) line(scope, puzzle, c, n - 1, 0, cw, ch, p, drift, Color(0xFFFFE8A3), a)
        c.stars.indices.forEach { i ->
            star(scope, puzzle.starX(c, i, drift) * cw, c.stars[i].second * ch, p, Color.White.copy(alpha = a), arms = 1)
        }
    }
}

private fun wiggle(puzzle: StarPuzzle, i: Int, time: Float, p: Float) =
    if (i == puzzle.wiggleStar && puzzle.wiggleTimer > 0f) sin(time * 40f) * 1.5f * p else 0f

/** A 2 x 2 star with sparkle arms [arms] pixels long (pixel art, no soft glows). */
private fun star(scope: DrawScope, x: Float, y: Float, p: Float, color: Color, arms: Int) {
    scope.drawRect(color, Offset(x, y), Size(2f * p, 2f * p))
    if (arms > 0) {
        val arm = color.copy(alpha = color.alpha * 0.75f)
        val len = arms * p
        scope.drawRect(arm, Offset(x + 0.5f * p, y - len), Size(p, len))
        scope.drawRect(arm, Offset(x + 0.5f * p, y + 2f * p), Size(p, len))
        scope.drawRect(arm, Offset(x - len, y + 0.5f * p), Size(len, p))
        scope.drawRect(arm, Offset(x + 2f * p, y + 0.5f * p), Size(len, p))
    }
}

/** A line of pixel blocks from star [a] to star [b] (skipped where the sky wraps round). */
private fun line(scope: DrawScope, puzzle: StarPuzzle, c: Constellation, a: Int, b: Int, cw: Float, ch: Float, p: Float, drift: Float, color: Color, alpha: Float) {
    val x1 = puzzle.starX(c, a, drift) * cw + p
    val y1 = c.stars[a].second * ch + p
    val x2 = puzzle.starX(c, b, drift) * cw + p
    val y2 = c.stars[b].second * ch + p
    if (kotlin.math.abs(x2 - x1) > cw * 0.5f) return
    val steps = (kotlin.math.hypot(x2 - x1, y2 - y1) / p).toInt().coerceAtLeast(1)
    for (s in 0..steps) {
        val t = s / steps.toFloat()
        scope.drawRect(color.copy(alpha = 0.9f * alpha), Offset(x1 + (x2 - x1) * t, y1 + (y2 - y1) * t), Size(p, p))
    }
}
