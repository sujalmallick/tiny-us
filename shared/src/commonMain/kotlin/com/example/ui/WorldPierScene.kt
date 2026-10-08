package com.example.ui

import com.example.engine.drawPixelGlow
import com.example.engine.WorldViewport

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.engine.PierSprites
import com.example.engine.drawCastShadow
import com.example.engine.drawContactShadow
import com.example.scene.GullState
import com.example.scene.PierLayout
import com.example.scene.SceneEngine
import com.example.scene.WeatherType
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Seaside Pier backdrop: sky, sea, lighthouse, bottle, railing, boardwalk, ice-cream cart,
 * Grandpa Bao and a perched Pip. Everything airborne or in hand is drawn by [drawPierForeground].
 */
fun drawSeasidePierScene(
    scope: DrawScope,
    cw: Float,
    ch: Float,
    p: Float,
    time: Float,
    engine: SceneEngine,
    isNight: Boolean,
    isSunset: Boolean,
    isMorning: Boolean
) {
    drawSkyAndClouds(scope, cw, ch, isNight, isSunset, isMorning, time, p, weather = engine.weather, isPinkSunset = isSunset)

    val horizonY = ch * PierLayout.HORIZON_Y
    val railY = ch * PierLayout.RAIL_Y
    val deckY = ch * PierLayout.DECK_Y
    val isRain = engine.weather == WeatherType.RAIN

    // 1. Sea, coloured by time of day, with drifting wave highlights.
    val seaTop = when {
        isNight -> Color(0xFF13234A)
        isSunset -> Color(0xFF6A4C7D)
        isMorning -> Color(0xFF7CC3DD)
        else -> Color(0xFF4F9FD0)
    }
    val seaDeep = when {
        isNight -> Color(0xFF0B1631)
        isSunset -> Color(0xFF3F2F5C)
        isMorning -> Color(0xFF4F9FC4)
        else -> Color(0xFF2F78AB)
    }
    scope.drawRect(seaTop, Offset(0f, horizonY), Size(cw, (deckY - horizonY) * 0.45f))
    scope.drawRect(seaDeep, Offset(0f, horizonY + (deckY - horizonY) * 0.45f), Size(cw, (deckY - horizonY) * 0.55f))
    val waveLight = if (isNight) Color(0x55AFC6FF) else Color(0x88FFFFFF)
    val waveAmp = if (isRain) 1.8f else 1f
    for (row in 0..4) {
        val rowY = horizonY + (row + 0.6f) * (deckY - horizonY) / 5.5f
        val drift = time * (0.012f + row * 0.006f)
        val dashW = (4f + row * 2f) * p
        for (k in 0..8) {
            val fx = ((k * 0.127f + row * 0.05f + drift) % 1.1f) - 0.05f
            val wy = rowY + sin(time * 1.6f + k * 1.3f + row) * 1.2f * p * waveAmp
            scope.drawRect(waveLight, Offset(fx * cw, wy), Size(dashW, 0.9f * p))
        }
    }
    // Sun or moon glitter on the water.
    val glitter = when {
        isNight -> Color(0xAAE8EEFF)
        isSunset -> Color(0xCCFFB86B)
        else -> Color(0x99FFF6C8)
    }
    for (i in 0..6) {
        val gy = horizonY + (i + 0.5f) * (railY - horizonY) / 7.5f
        val gw = (3f + i * 1.6f) * p * (0.7f + 0.3f * sin(time * 3f + i))
        scope.drawRect(glitter, Offset(cw * 0.62f - gw / 2f, gy), Size(gw, 0.8f * p))
    }

    // Sailboat drifting along the horizon.
    PierLayout.boat(cw, ch, time)?.let { boat ->
        PierSprites.drawSailboat(scope, boat.x, boat.y, p, time, tooting = engine.pierBoatHornTimer > 0f, isNight = isNight)
    }

    // Dolphins leaping, staggered one after another, when spotted.
    if (engine.pierDolphinTimer > 0f) {
        val progress = 1f - engine.pierDolphinTimer / SceneEngine.PIER_DOLPHIN_SECONDS
        val water = PierLayout.dolphinWaterline(cw, ch)
        for (i in 0..2) {
            val arc = (progress * 1.6f - i * 0.3f)
            PierSprites.drawDolphin(scope, water.x + i * 16f * p, water.y + i * 2f * p, p, arc)
        }
    }

    // A little flock lifting off the lighthouse rocks after the foghorn.
    if (engine.pierFlockTimer > 0f) {
        val progress = 1f - engine.pierFlockTimer / SceneEngine.PIER_FLOCK_SECONDS
        val rocks = PierLayout.lighthouseBase(cw, ch)
        for (i in 0..2) {
            val gx = rocks.x - progress * cw * (0.45f + i * 0.08f)
            val gy = rocks.y - 6f * p - progress * ch * (0.14f + i * 0.03f) + i * 4f * p
            PierSprites.drawDistantGull(scope, gx, gy, p, time + i, isNight)
        }
    }

    // 2. Lighthouse on its rocks, with a sweeping beam at night (or after a tap).
    val base = PierLayout.lighthouseBase(cw, ch)
    scope.drawOval(Color(0xFF4A4E57), Offset(base.x - 16f * p, base.y - 4f * p), Size(32f * p, 9f * p))
    scope.drawOval(Color(0xFF636A75), Offset(base.x - 12f * p, base.y - 6f * p), Size(20f * p, 7f * p))
    for (band in 0..3) {
        val bandBottom = base.y - 3f * p - band * 8f * p
        val halfW = (6f - band * 0.8f) * p
        scope.drawRect(
            if (band % 2 == 0) Color(0xFFF4F1EA) else Color(0xFFD64545),
            Offset(base.x - halfW, bandBottom - 8f * p), Size(halfW * 2f, 8f * p)
        )
    }
    val lamp = PierLayout.lighthouseLamp(cw, ch, p)
    scope.drawRect(Color(0xFF2E3440), Offset(lamp.x - 5f * p, lamp.y + 2f * p), Size(10f * p, 1.5f * p))
    val lampOn = isNight || isSunset || engine.pierLighthouseTimer > 0f
    scope.drawRect(if (lampOn) Color(0xFFFFE9A0) else Color(0xFFB7C7D1), Offset(lamp.x - 3f * p, lamp.y - 3f * p), Size(6f * p, 5f * p))
    scope.drawRect(Color(0xFFD64545), Offset(lamp.x - 4f * p, lamp.y - 6f * p), Size(8f * p, 3f * p))
    if (lampOn) {
        val angle = time * 1.1f
        val reach = cw * 0.55f
        val end = Offset(lamp.x + cos(angle) * reach, lamp.y + sin(angle) * reach * 0.18f)
        val beamAlpha = if (engine.pierLighthouseTimer > 0f) 0.32f else 0.18f
        scope.drawLine(Color(0xFFFFF3B0).copy(alpha = beamAlpha * 0.6f), lamp, end, strokeWidth = 14f * p, cap = StrokeCap.Round)
        scope.drawLine(Color(0xFFFFF3B0).copy(alpha = beamAlpha), lamp, end, strokeWidth = 6f * p, cap = StrokeCap.Round)
        drawPixelGlow(scope, Color(0xFFFFF3B0).copy(alpha = 0.45f), 7f * p, lamp, p)
    }

    // 3. Message in a bottle, bobbing on the swell.
    if (engine.pierBottleVisible) {
        val b = PierLayout.bottle(cw, ch)
        val by = b.y + sin(time * 1.8f) * 1.5f * p
        scope.drawOval(Color(0x553E7CB1), Offset(b.x - 6f * p, by + 2f * p), Size(12f * p, 2.5f * p))
        scope.drawRoundRect(Color(0xCC6FBF8E), Offset(b.x - 2.5f * p, by - 4f * p), Size(5f * p, 7f * p), CornerRadius(1.5f * p, 1.5f * p))
        scope.drawRect(Color(0xCC6FBF8E), Offset(b.x - 1f * p, by - 6.5f * p), Size(2f * p, 2.5f * p))
        scope.drawRect(Color(0xFF8D6E63), Offset(b.x - 1f * p, by - 7.5f * p), Size(2f * p, 1.2f * p))
        scope.drawRect(Color(0xFFFFF1E6), Offset(b.x - 1.2f * p, by - 2.5f * p), Size(2.4f * p, 3.5f * p))
    }

    // 4. Railing with string lights.
    val postColor = Color(0xFF6B4A35)
    val railColor = Color(0xFF8A6246)
    var px = cw * 0.02f
    while (px < cw) {
        scope.drawRect(postColor, Offset(px, railY), Size(2f * p, deckY - railY + 2f * p))
        px += cw * 0.09f
    }
    scope.drawRect(railColor, Offset(0f, railY), Size(cw, 2.2f * p))
    scope.drawRect(railColor, Offset(0f, railY + (deckY - railY) * 0.55f), Size(cw, 1.4f * p))
    if (engine.weather == WeatherType.SNOW) {
        scope.drawRect(Color(0xFFF7FBFF), Offset(0f, railY - 1f * p), Size(cw, 1.2f * p))
    }
    val sparkle = engine.pierLightsSparkleTimer > 0f
    val lightsGlow = isNight || isSunset || sparkle
    for (i in 0..16) {
        val lx = cw * (0.03f + i * 0.06f)
        val sag = sin(i * 1.9f) * 0.8f * p
        val bulb = pierBulbColor(engine.pierLightsPalette, i)
        if (lightsGlow) {
            val twinkle = (if (sparkle) 0.45f else 0.25f) + 0.15f * sin(time * 2.2f + i)
            drawPixelGlow(scope, bulb.copy(alpha = twinkle), 3.5f * p, Offset(lx, railY - 2f * p + sag), p)
        }
        scope.drawCircle(if (lightsGlow) bulb else bulb.copy(alpha = 0.55f), 1f * p, Offset(lx, railY - 2f * p + sag))
    }

    // 5. Boardwalk planks.
    val plank = when {
        isNight -> Color(0xFF4A3A30)
        isSunset -> Color(0xFF8A5E45)
        else -> Color(0xFFA8784F)
    }
    val plankLine = when {
        isNight -> Color(0xFF33281F)
        isSunset -> Color(0xFF65412F)
        else -> Color(0xFF7D5636)
    }
    scope.drawRect(plank, Offset(0f, deckY), Size(cw, ch - deckY))
    var rowY = deckY
    var row = 0
    val plankH = 7f * p
    while (rowY < ch) {
        scope.drawRect(plankLine, Offset(0f, rowY), Size(cw, 0.8f * p))
        val stagger = if (row % 2 == 0) 0f else cw * 0.11f
        var sx = stagger
        while (sx < cw) {
            scope.drawRect(plankLine, Offset(sx, rowY), Size(0.8f * p, plankH))
            sx += cw * 0.22f
        }
        rowY += plankH
        row++
    }

    // Coin telescope by the railing; it glints while the dolphins are out.
    val scopeBase = PierLayout.telescope(cw, ch)
    PierSprites.drawTelescope(scope, scopeBase.x, scopeBase.y, p, glinting = engine.pierDolphinTimer > 0f)

    // Clouds' shadows drift over the planks (and the couple glancing up at one)
    drawCloudShadows(scope, cw, ch, p, time, engine, isNight, isSunset, isMorning)

    // 6. Bench the couple sits on (drawn behind them).
    val benchY = ch * 0.74f - 9f * p
    val benchWidthPx = (cw * 0.30f / p).roundToInt()
    drawCastShadow(scope, cw * 0.49f, benchY + 9f * p, benchWidthPx, p, heightPx = 14)
    drawContactShadow(scope, cw * 0.49f, benchY + 9f * p, benchWidthPx, p)
    scope.drawRect(Color(0xFF5B3E2B), Offset(cw * 0.34f, benchY), Size(cw * 0.30f, 2.5f * p))
    scope.drawRect(Color(0xFF5B3E2B), Offset(cw * 0.34f, benchY - 8f * p), Size(cw * 0.30f, 2f * p))
    scope.drawRect(Color(0xFF3E2A1D), Offset(cw * 0.36f, benchY), Size(1.8f * p, 9f * p))
    scope.drawRect(Color(0xFF3E2A1D), Offset(cw * 0.62f, benchY), Size(1.8f * p, 9f * p))

    // 7. Ice-cream cart with a striped awning.
    val cart = PierLayout.cart(cw, ch)
    drawCastShadow(scope, cart.x - 7f * p, cart.y, 8, p)
    drawContactShadow(scope, cart.x - 7f * p, cart.y, 8, p)
    drawCastShadow(scope, cart.x + 7f * p, cart.y, 8, p)
    drawContactShadow(scope, cart.x + 7f * p, cart.y, 8, p)
    scope.drawCircle(Color(0xFF3A3A3A), 3f * p, Offset(cart.x - 7f * p, cart.y - 3f * p))
    scope.drawCircle(Color(0xFF3A3A3A), 3f * p, Offset(cart.x + 7f * p, cart.y - 3f * p))
    scope.drawRoundRect(Color(0xFFF7F3E8), Offset(cart.x - 11f * p, cart.y - 16f * p), Size(22f * p, 12f * p), CornerRadius(2f * p, 2f * p))
    scope.drawRect(Color(0xFFFF9AA2), Offset(cart.x - 11f * p, cart.y - 10f * p), Size(22f * p, 2f * p))
    scope.drawRect(Color(0xFF8D6E63), Offset(cart.x - 10f * p, cart.y - 30f * p), Size(1.2f * p, 14f * p))
    scope.drawRect(Color(0xFF8D6E63), Offset(cart.x + 9f * p, cart.y - 30f * p), Size(1.2f * p, 14f * p))
    for (stripe in 0..5) {
        scope.drawRect(
            if (stripe % 2 == 0) Color(0xFFFF8FAB) else Color(0xFFFFFFFF),
            Offset(cart.x - 13f * p + stripe * 4.4f * p, cart.y - 33f * p), Size(4.4f * p, 4f * p)
        )
    }
    // A little cone sign on the counter.
    scope.drawRect(Color(0xFFE0B07A), Offset(cart.x - 1f * p, cart.y - 19f * p), Size(2f * p, 3f * p))
    scope.drawCircle(Color(0xFFFFC8DD), 1.8f * p, Offset(cart.x, cart.y - 20f * p))

    // 8. Bait bucket by Mochi's spot.
    val bucket = PierLayout.bucket(cw, ch)
    drawCastShadow(scope, bucket.x, bucket.y, 10, p, heightPx = 8)
    drawContactShadow(scope, bucket.x, bucket.y, 10, p)
    scope.drawRect(Color(0xFF8E9AA6), Offset(bucket.x - 3.5f * p, bucket.y - 6f * p), Size(7f * p, 6f * p))
    scope.drawRect(Color(0xFF6B7783), Offset(bucket.x - 4f * p, bucket.y - 6.5f * p), Size(8f * p, 1.2f * p))
    scope.drawRect(Color(0xFFB0C4DE), Offset(bucket.x + 1f * p, bucket.y - 9f * p), Size(1.5f * p, 3f * p))
    if (engine.pierBucketFlopTimer > 0f) {
        // A fish flips up out of the bucket and back in.
        val u = 1f - engine.pierBucketFlopTimer / 1.6f
        val hop = sin(u * kotlin.math.PI.toFloat()) * 12f * p
        val fx = bucket.x - u * 4f * p
        scope.drawOval(Color(0xFFB0C4DE), Offset(fx - 3f * p, bucket.y - 9f * p - hop), Size(6f * p, 3f * p))
        scope.drawRect(Color(0xFF8DA2BD), Offset(fx + 2.5f * p, bucket.y - 9.5f * p - hop + sin(time * 30f) * 0.5f * p), Size(1.8f * p, 3.5f * p))
    }

    // Pinchy the crab on the boardwalk.
    if (engine.isPierCrabVisible) {
        val crab = PierLayout.crab(cw, ch, engine.pierCrabX)
        PierSprites.drawCrab(scope, crab.x, crab.y, p, time, startled = engine.pierCrabStartledTimer > 0f)
    }

    // 9. Grandpa Bao at the end of the pier.
    val bao = PierLayout.bao(cw, ch)
    // Bao is drawn at the couple's scale so he reads as a person, not a prop.
    PierSprites.drawGrandpaBao(
        scope, bao.x, bao.y, PierLayout.baoScale(p), time, engine.pierFishingPhase, engine.pierLastCatch,
        waterY = horizonY + (railY - horizonY) * 0.62f,
        sipping = engine.pierBaoSipTimer > 0f || engine.isBaoTeaTime,
        waving = engine.pierBaoWaveTimer > 0f,
        dozing = engine.isBaoDozing
    )

    // 10. Pip, when standing on the railing (behind the couple).
    if (engine.pierGullState.isVisible && !engine.pierGullState.isAirborne) {
        PierSprites.drawSeagull(
            scope, cw * engine.pierGullX, ch * engine.pierGullY, p, time,
            engine.pierGullState, engine.pierGullFacingLeft,
            tucked = isRain && engine.pierGullState == GullState.PERCHED
        )
    }
}

/** Things in front of the couple: their ice-cream cones and Pip in flight. */
fun drawPierForeground(scope: DrawScope, cw: Float, ch: Float, p: Float, time: Float, engine: SceneEngine) {
    if (engine.pierIceCreamTimer > 0f) {
        val cps = WorldViewport.characterPixelScale(cw, WorldViewport.pixelRenderer)
        // Melting a little as the timer runs down.
        val scoop = 2.4f * p * (0.6f + 0.4f * (engine.pierIceCreamTimer / SceneEngine.PIER_ICE_CREAM_SECONDS))
        drawCone(scope, cw * engine.boy.worldX + 6f * cps, ch * engine.boy.worldY - 11f * cps, p, scoop)
        drawCone(scope, cw * engine.girl.worldX - 6f * cps, ch * engine.girl.worldY - 11f * cps, p, scoop)
    }
    if (engine.pierGullState.isAirborne) {
        PierSprites.drawSeagull(
            scope, cw * engine.pierGullX, ch * engine.pierGullY, p, time,
            engine.pierGullState, engine.pierGullFacingLeft
        )
    }
}

private fun drawCone(scope: DrawScope, handX: Float, handY: Float, p: Float, scoop: Float) {
    scope.drawRect(Color(0xFFE0B07A), Offset(handX - 1.6f * p, handY), Size(3.2f * p, 2f * p))
    scope.drawRect(Color(0xFFE0B07A), Offset(handX - 1f * p, handY + 2f * p), Size(2f * p, 2f * p))
    scope.drawRect(Color(0xFFC98F55), Offset(handX - 0.4f * p, handY + 4f * p), Size(0.8f * p, 1.5f * p))
    scope.drawCircle(Color(0xFFFFC8DD), scoop, Offset(handX, handY - scoop * 0.5f))
}

/** Three string-light colour schemes, cycled by tapping the railing. */
private fun pierBulbColor(palette: Int, index: Int): Color = when (palette) {
    1 -> if (index % 2 == 0) Color(0xFFFFF3B0) else Color(0xFFFFE0A3) // warm white
    2 -> when (index % 4) { 0 -> Color(0xFFFF6B6B); 1 -> Color(0xFFFFD93D); 2 -> Color(0xFF6BCB77); else -> Color(0xFF4D96FF) } // party
    else -> when (index % 3) { 0 -> Color(0xFFFFD166); 1 -> Color(0xFFFF9AA2); else -> Color(0xFFBDE0FE) } // pastel
}
