package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.games.CozySprites
import com.example.games.FishingGame
import com.example.games.PlotStage
import com.example.scene.CozyGames
import com.example.scene.SceneEngine
import com.example.scene.SceneType

/**
 * Cooking, fishing and garden care in the world (plan 07, C3-C5): the garden plots in the meadow,
 * the finished dish on the kitchen table, and the couple's rod, line and bobber at the pier.
 */
fun drawCozyGames(scope: DrawScope, engine: SceneEngine, cw: Float, ch: Float, p: Float, time: Float) {
    val cozy = engine.cozy
    when (engine.currentScene) {
        SceneType.FLOWER -> drawGarden(scope, cozy, cw, ch, p, time)
        SceneType.COOKING -> cozy.servedDish?.let { dish ->
            val sprite = CozySprites.DISHES[dish.id] ?: return@let
            // On the table, just left of the vase (the mugs sit to its right).
            val tableTop = ch * 0.65f + ch * 0.35f * 0.44f + 7f * p
            CozySprites.draw(scope, sprite, cw * 0.50f - (4 + sprite.width) * p, tableTop - sprite.height * p, p)
        }
        SceneType.SEASIDE_PIER -> if (cozy.fishingActive) drawCoupleFishing(scope, engine, cw, ch, p, time)
        else -> Unit
    }
}

private fun drawGarden(scope: DrawScope, cozy: CozyGames, cw: Float, ch: Float, p: Float, time: Float) {
    val today = com.example.data.CoupleDates.today().toEpochDays().toLong()
    // The plots are drawn at twice the game pixel, so they read at a glance in the big meadow.
    val p = p * CozyGames.PLOT_SCALE
    for ((i, plot) in cozy.garden.plots.withIndex()) {
        val base = cozy.plotBase(i, cw, ch)
        val sprite = CozySprites.PLOT.getValue(plot.stage)
        val (petal, center) = CozySprites.flowerColors(plot.flower ?: "")
        CozySprites.draw(scope, sprite, base.x - sprite.width * p / 2f, base.y - sprite.height * p, p, listOf(petal), center)
        // A small sign of what it would like: a drop when it's thirsty today, a twinkle in bloom.
        val top = base.y - sprite.height * p - 4f * p
        val bob = if ((time * 2f).toInt() % 2 == 0) 0f else p
        when {
            plot.stage == PlotStage.BLOOM -> if ((time * 3f + i).toInt() % 3 == 0) {
                scope.drawRect(Color(0xFFFFF3B0), Offset(base.x + 3f * p, top + p), Size(p, p))
            }
            cozy.garden.needsWater(i, today) -> {
                val drop = Color(0xFF9AD1F5)
                scope.drawRect(drop, Offset(base.x + 3f * p, top + bob), Size(p, p))
                scope.drawRect(drop, Offset(base.x + 2f * p, top + p + bob), Size(3f * p, 2f * p))
                scope.drawRect(Color(0xFF4A7FC1), Offset(base.x + 3f * p, top + 2f * p + bob), Size(p, p))
            }
        }
    }
    // Picked flowers waiting for a bouquet, laid in the grass beside the plots.
    for ((i, stem) in cozy.garden.stems.withIndex()) {
        val base = cozy.plotBase(CozyGames.PLOT_XS.size - 1, cw, ch)
        val x = base.x + (10f + i * 4f) * p
        scope.drawRect(Color(0xFF6BBF59), Offset(x, base.y - 3f * p), Size(p, 3f * p))
        scope.drawRect(CozySprites.flowerColors(stem).first, Offset(x - p, base.y - 5f * p), Size(3f * p, 2f * p))
    }
}

private fun drawCoupleFishing(scope: DrawScope, engine: SceneEngine, cw: Float, ch: Float, p: Float, time: Float) {
    val game = engine.cozy.fishing
    val rod = Color(0xFF8B5A2B)
    val line = Color(0xCCFFFFFF)
    val bobberX = cw * CozyGames.BOBBER_X
    val restY = ch * CozyGames.BOBBER_Y + kotlin.math.sin(time * 2.2f) * 0.6f * p
    // The rod leans from his hands out over the rail, its tip above the bobber.
    val holder = engine.boy
    val butt = Offset(cw * holder.worldX - 2f * p, ch * holder.worldY - 9f * p)
    val tip = Offset(bobberX + 2f * p, ch * 0.60f - 26f * p)
    scope.drawLine(rod, butt, tip, strokeWidth = 1.2f * p)
    scope.drawRect(Color(0xFF5C4630), Offset(butt.x - p, butt.y - 3f * p), Size(2f * p, 2f * p)) // the reel
    when (game.phase) {
        FishingGame.Phase.REELING, FishingGame.Phase.LANDED -> {
            // The catch swings up on the line.
            val c = game.caught ?: return
            val sprite = CozySprites.CATCHES.getValue(c)
            val lift = if (game.phase == FishingGame.Phase.REELING) 1f - game.timer / FishingGame.REEL_SECONDS else 1f
            val y = restY - lift * 14f * p
            scope.drawLine(line, tip, Offset(bobberX, y - sprite.height * p / 2f), strokeWidth = 0.6f * p)
            CozySprites.draw(scope, sprite, bobberX - sprite.width * p / 2f, y - sprite.height * p / 2f, p)
        }
        else -> {
            val bobberY = restY + game.dip * 2f * p
            scope.drawLine(line, tip, Offset(bobberX, bobberY), strokeWidth = 0.6f * p)
            val sprite = CozySprites.BOBBER
            // In a bite it's pulled under: only its red top shows, with a ring around it.
            val shown = if (game.phase == FishingGame.Phase.BITE) 3 else sprite.height
            val clipped = CozySprites.Sprite(sprite.rows.take(shown))
            CozySprites.draw(scope, clipped, bobberX - sprite.width * p / 2f, bobberY - shown * p / 2f, p)
            if (game.phase == FishingGame.Phase.BITE) {
                val ring = Color(0x99FFFFFF)
                val r = (3f + (time * 6f) % 3f) * p
                scope.drawRect(ring, Offset(bobberX - r, bobberY + 2f * p), Size(2f * r, p * 0.6f))
            }
        }
    }
}
