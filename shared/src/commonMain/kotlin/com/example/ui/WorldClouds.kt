package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.scene.SceneEngine
import com.example.scene.WeatherLayout
import com.example.scene.WeatherType
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * The day sky's clouds (depth): the same blocky pixel clouds as always, given a little detail on
 * their own silhouette (a bright top edge, a shaded belly over the darker underside, the side away
 * from the sun a step darker), and their shadows drifting across the ground below.
 */

/**
 * The cloud's silhouette on its own pixel grid, from the rects it has always been drawn with: a
 * body, a top puff, a puff each side and the darker underside showing below the body. [originX],
 * [originY] is where the grid's corner sits relative to the cloud's anchor, in pixels.
 */
private object CloudShape {
    const val w = 38
    const val h = 18
    const val originX = -5
    const val originY = -6
    private const val UNDER_TOP = 16

    // x, y, width, height on the grid
    private val parts = listOf(
        intArrayOf(5, 6, 28, 10), // body
        intArrayOf(11, 0, 18, 6), // top puff
        intArrayOf(0, 9, 6, 6), // left puff
        intArrayOf(32, 9, 6, 6), // right puff
        intArrayOf(5, UNDER_TOP, 28, 2) // underside
    )
    private val filled = BooleanArray(w * h) { i ->
        val x = i % w
        val y = i / w
        parts.any { (px, py, pw, ph) -> x in px until px + pw && y in py until py + ph }
    }

    fun at(x: Int, y: Int) = x in 0 until w && y in 0 until h && filled[y * w + x]
    fun isUnderside(y: Int) = y >= UNDER_TOP
    /** The main body, which was always drawn over the underside's colour (the puffs over the sky). */
    fun inBody(x: Int, y: Int) = x in 5 until 33 && y in 6 until UNDER_TOP
}

private const val HIGHLIGHT = 0
private const val BODY = 1
private const val SHADE = 2
private const val UNDERSIDE = 3
private const val PUFF = 4

/**
 * The cloud's pixels as runs of one tone (row, first column, length, tone), worked out once per
 * side the sun is on, so a cloud is a few dozen rects a frame.
 */
private val cloudRuns = HashMap<Boolean, IntArray>()

private fun runsFor(sunLeft: Boolean): IntArray {
    cloudRuns[sunLeft]?.let { return it }
    val s = CloudShape
    val away = if (sunLeft) 1 else -1
    fun tone(x: Int, y: Int): Int = when {
        s.isUnderside(y) -> UNDERSIDE
        // The top edge of each puff catches the light
        !s.at(x, y - 1) -> HIGHLIGHT
        // A dithered shaded row just above the underside, and under each side puff
        !s.at(x, y + 1) || s.isUnderside(y + 1) -> if ((x + y) % 2 == 0) SHADE else BODY
        // The outermost column away from the sun is a step darker
        !s.at(x + away, y) -> SHADE
        s.inBody(x, y) -> BODY
        else -> PUFF
    }
    val runs = ArrayList<Int>()
    for (y in 0 until s.h) {
        var x = 0
        while (x < s.w) {
            if (!s.at(x, y)) { x++; continue }
            val t = tone(x, y)
            var end = x
            while (end + 1 < s.w && s.at(end + 1, y) && tone(end + 1, y) == t) end++
            runs += y; runs += x; runs += end - x + 1; runs += t
            x = end + 1
        }
    }
    return runs.toIntArray().also { cloudRuns[sunLeft] = it }
}

/** A halfway colour between [a] and [b]. */
private fun mid(a: Color, b: Color) = Color((a.red + b.red) / 2f, (a.green + b.green) / 2f, (a.blue + b.blue) / 2f, (a.alpha + b.alpha) / 2f)

/** A cloud drifting right at [scaleFactor] speed from [baseX]: its left edge, wrapping round the world. */
private fun cloudLeft(cw: Float, baseX: Float, scaleFactor: Float, time: Float, p: Float, shape: CloudShape = CloudShape): Float {
    val span = shape.w * p
    // The old cloud's left puff sat 5 pixels left of its anchor, which started 70 screen px back
    val start = baseX - 70f + shape.originX * p
    val x = (start + time * 10f * scaleFactor + span) % (cw + span)
    return x - span
}

/**
 * The day sky's clouds as anchors (share of the width, share of the height, drift speed).
 * Higher clouds are further off, so their shadows land further back on the ground.
 */
private val SKY_CLOUDS = listOf(
    floatArrayOf(0.08f, 0.10f, 0.7f),
    floatArrayOf(0.60f, 0.18f, 1.0f),
    floatArrayOf(0.35f, 0.28f, 0.5f)
)

/** One drifting pixel cloud, lit from the sun's side; it wraps around a world [cw] wide. */
fun drawSkyCloud(
    scope: DrawScope,
    cw: Float,
    baseX: Float,
    y: Float,
    scaleFactor: Float,
    time: Float,
    p: Float,
    isSunset: Boolean,
    isMorning: Boolean
) {
    // Whole pixels only, so it drifts a pixel at a time instead of smearing
    val left = (cloudLeft(cw, baseX, scaleFactor, time, p) / p).roundToInt() * p
    val top = ((y / p).roundToInt() + CloudShape.originY) * p
    // The cloud's own colours (body and underside), with a lit edge and a shade between them
    val puff = when {
        isSunset -> Color(0xFFFFDDD2)
        isMorning -> Color(0xFFFFF0F5)
        else -> Color(0xF2FFFFFF)
    }
    // The body as it always looked: the puff colour over the underside colour
    val body = if (puff.alpha < 1f) Color(0xFFFDFDFE) else puff
    val under = when {
        isSunset -> Color(0xFFE29578)
        isMorning -> Color(0xFFF7CAD0)
        else -> Color(0xFFD6E2E9)
    }
    val light = if (isSunset) Color(0xFFFFF1EA) else Color.White
    val shade = mid(body, under)
    val sunLeft = celestialProgress(isNight = false, isSunset = isSunset, isMorning = isMorning) < 0.5f
    val runs = runsFor(sunLeft)
    var i = 0
    while (i < runs.size) {
        val color = when (runs[i + 3]) {
            HIGHLIGHT -> light
            SHADE -> shade
            UNDERSIDE -> under
            PUFF -> puff
            else -> body
        }
        scope.drawRect(color, Offset(left + runs[i + 1] * p, top + runs[i] * p), Size(runs[i + 2] * p, p))
        i += 4
    }
}

/** The day sky's three clouds (see [SKY_CLOUDS]). */
fun drawDayClouds(scope: DrawScope, cw: Float, ch: Float, time: Float, p: Float, isSunset: Boolean, isMorning: Boolean) {
    for (c in SKY_CLOUDS) drawSkyCloud(scope, cw, cw * c[0], ch * c[1], c[2], time, p, isSunset, isMorning)
}

/** The colour of a cloud's shadow on the ground: a faint cool wash, so grass and planks stay themselves. */
private val CloudShadowColor = Color(0x1C0E1A30)

/**
 * A flat pixel blob of shade on the ground, [wPx] by [hPx] pixels around ([cx], [cy]), with
 * checkerboard edges (so it never shows a hard line) and a lumpy outline from [seed].
 */
private fun drawGroundShade(scope: DrawScope, cx: Float, cy: Float, wPx: Int, hPx: Int, p: Float, seed: Int) {
    val left = (cx / p).roundToInt() - wPx / 2
    val top = (cy / p).roundToInt() - hPx / 2
    for (r in 0 until hPx) {
        val dy = (r + 0.5f - hPx / 2f) / (hPx / 2f)
        val t = 1f - dy * dy
        if (t <= 0f) continue
        // A slightly lumpy edge, the same every frame for this cloud
        val wobble = ((seed * 7 + r * 13) % 5 - 2) * 0.5f
        val half = (wPx / 2f * sqrt(t) + wobble).roundToInt()
        if (half <= 1) continue
        val row = top + r
        val y = row * p
        val from = left + wPx / 2 - half
        val to = left + wPx / 2 + half - 1
        fun dot(col: Int) {
            if (((col + row) and 1) == 0) scope.drawRect(CloudShadowColor, Offset(col * p, y), Size(p, p))
        }
        if (to - from > 3) scope.drawRect(CloudShadowColor, Offset((from + 2) * p, y), Size((to - from - 3) * p, p))
        dot(from); dot(from + 1); dot(to - 1); dot(to)
    }
}

/**
 * The clouds' shadows drifting across the ground on a fair day (not at night, at sunset, in rain or
 * snow): one under each sky cloud, moving with it and nudged away from the sun. Drawn just after the
 * ground, before the props and the couple. While the couple stop to look up at a passing cloud
 * ([SceneEngine.cloudPassTimer]) a bigger shadow drifts over where they stand.
 */
fun drawCloudShadows(scope: DrawScope, cw: Float, ch: Float, p: Float, time: Float, engine: SceneEngine, isNight: Boolean, isSunset: Boolean, isMorning: Boolean) {
    if (isNight || isSunset) return
    if (engine.weather == WeatherType.RAIN || engine.weather == WeatherType.SNOW) return
    val sunSide = 0.5f - celestialProgress(isNight = false, isSunset = false, isMorning = isMorning)
    for ((i, c) in SKY_CLOUDS.withIndex()) {
        val shape = CloudShape
        val left = cloudLeft(cw, cw * c[0], c[2], time, p)
        // Higher clouds are further away: their shadows land further back, and look smaller
        val depth = ((c[1] - 0.10f) / 0.18f).coerceIn(0f, 1f)
        val gy = ch * (0.745f + 0.17f * depth)
        val scale = 1.1f + 0.5f * depth
        val cx = left + shape.w * p / 2f + sunSide * cw * 0.35f
        drawGroundShade(scope, cx, gy, (shape.w * scale).roundToInt(), (4 + 3 * depth).roundToInt() + 2, p, i + 1)
    }
    if (engine.cloudPassTimer > 0f) {
        val t = 1f - engine.cloudPassTimer / WeatherLayout.CLOUD_PASS_SECONDS
        val wPx = (cw * 0.45f / p).roundToInt()
        val x = -wPx * p / 2f + t * (cw + wPx * p)
        val feet = ch * maxOf(engine.boy.worldY, engine.girl.worldY)
        drawGroundShade(scope, x, feet, wPx, 12, p, 7)
    }
}
