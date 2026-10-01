package com.example.engine

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.abs

private val TREE_FLOWER_OFFSETS = arrayOf(
    -20 to -16,
    16 to -18,
    -10 to 8,
    22 to 6,
    -28 to 2,
    4 to -28,
    28 to -8,
    -14 to -4,
    8 to 12,
    -4 to 18
)

private val TREE_LEAF_ACCENT_OFFSETS = arrayOf(
    -18 to -12,
    18 to -14,
    -12 to 10,
    20 to 8,
    2 to -24
)

private val COTTAGE_ICICLE_FRACS = floatArrayOf(0.08f, 0.22f, 0.38f, 0.52f, 0.68f, 0.82f, 0.94f)

private val KITCHEN_SPICE_COLORS = arrayOf(Color(0xFFE63946), Color(0xFFE9C46A), Color(0xFF2A9D8F))
private val MOMO_LIGHT_COLORS = arrayOf(Color(0xFFFFD166), Color(0xFFFF5D8F), Color(0xFF70E000), Color(0xFF48CAE4), Color(0xFFFFD166))
private val SPLITS_RIDE_SKY = listOf(0.25f, 0.50f, 0.75f)

private val RIDE_SKY_NIGHT_SNOW = listOf(Color(0xFF040816), Color(0xFF0A132C), Color(0xFF101E42), Color(0xFF1A2A56))
private val RIDE_SKY_NIGHT_SAKURA = listOf(Color(0xFF080616), Color(0xFF120E2C), Color(0xFF1E1742), Color(0xFF2C2258))
private val RIDE_SKY_NIGHT_AUTUMN = listOf(Color(0xFF070614), Color(0xFF100E26), Color(0xFF1C1838), Color(0xFF28224C))
private val RIDE_SKY_NIGHT_RAIN = listOf(Color(0xFF050712), Color(0xFF0C1022), Color(0xFF141930), Color(0xFF1C223E))
private val RIDE_SKY_NIGHT_SUMMER = listOf(Color(0xFF070B1E), Color(0xFF0E1638), Color(0xFF162250), Color(0xFF202C5E))

private val RIDE_SKY_SUNSET_SAKURA = listOf(Color(0xFF38184C), Color(0xFF7A205A), Color(0xFFC44275), Color(0xFFFFB5C2))
private val RIDE_SKY_SUNSET_AUTUMN = listOf(Color(0xFF2B0E1E), Color(0xFF7A1C16), Color(0xFFC4451C), Color(0xFFF7BA3E))
private val RIDE_SKY_SUNSET_SNOW = listOf(Color(0xFF1C1A3A), Color(0xFF4C3A66), Color(0xFF8E5C7E), Color(0xFFDF9FB8))
private val RIDE_SKY_SUNSET_RAIN = listOf(Color(0xFF1E172B), Color(0xFF48334E), Color(0xFF7A4A58), Color(0xFFA66D60))
private val RIDE_SKY_SUNSET_SUMMER = listOf(Color(0xFF241434), Color(0xFF451E49), Color(0xFF7E3159), Color(0xFFCE5A4B))

private val RIDE_SKY_MORNING_SAKURA = listOf(Color(0xFF343B68), Color(0xFF755E88), Color(0xFFFF9EAA), Color(0xFFFFE0E9))
private val RIDE_SKY_MORNING_AUTUMN = listOf(Color(0xFF2E334D), Color(0xFF6B5868), Color(0xFFE29578), Color(0xFFFFDDD2))
private val RIDE_SKY_MORNING_SNOW = listOf(Color(0xFF24324E), Color(0xFF5E6E8E), Color(0xFFA5B4D0), Color(0xFFE2EAFC))
private val RIDE_SKY_MORNING_RAIN = listOf(Color(0xFF202A3D), Color(0xFF43526A), Color(0xFF64748B), Color(0xFF94A3B8))
private val RIDE_SKY_MORNING_SUMMER = listOf(Color(0xFF2C3E7A), Color(0xFF5E5080), Color(0xFFFF8C69), Color(0xFFFFD1A0))

private val RIDE_SKY_DAY_SAKURA = listOf(Color(0xFF6BA8D6), Color(0xFF8CC0E2), Color(0xFFC0DEF0), Color(0xFFFFD6E7))
private val RIDE_SKY_DAY_AUTUMN = listOf(Color(0xFF4A85B8), Color(0xFF6E9EC5), Color(0xFFA5C5DC), Color(0xFFEAD5A8))
private val RIDE_SKY_DAY_SNOW = listOf(Color(0xFF5D99C6), Color(0xFF7FB0D6), Color(0xFFB0D4EA), Color(0xFFEBF4FA))
private val RIDE_SKY_DAY_RAIN = listOf(Color(0xFF334B68), Color(0xFF4A6580), Color(0xFF6E889E), Color(0xFF9BB1C2))
private val RIDE_SKY_DAY_SUMMER = listOf(Color(0xFF3B9FE2), Color(0xFF68BCE8), Color(0xFFA5DCF4), Color(0xFFD4EFFC))

object WorldSprites {

    // Foliage & Nature Colors
    val TreeTrunk = Color(0xFF6B4226)
    val TreeTrunkDark = Color(0xFF4A2810)
    val LeavesDark = Color(0xFF2D5A27)
    val LeavesMid = Color(0xFF40916C)
    val LeavesLight = Color(0xFF74C69D)
    val LeavesBlossomPink = Color(0xFFFFB5C2)

    // Furniture & Interior Colors
    val WoodFloor = Color(0xFFDDA15E)
    val WoodFloorDark = Color(0xFFBC6C25)
    val WallCream = Color(0xFFFEFAE0)
    val CouchFabric = Color(0xFF6B9080)
    val CouchFabricDark = Color(0xFF4F6D60)
    val CushionPink = Color(0xFFF4ACB7)
    val KitchenCounter = Color(0xFFF8F9FA)
    val KitchenStove = Color(0xFF343A40)
    val PotMetal = Color(0xFF6C757D)
    val SoupBroth = Color(0xFFF39A59)

    // Cottage Exterior
    val RoofTerracotta = Color(0xFF9C4F34)
    val RoofTerracottaDark = Color(0xFF7A3822)
    val WallStucco = Color(0xFFFBF5EE)
    val WallTimber = Color(0xFF58311B)
    val ChimneyBrick = Color(0xFF8B4F39)
    val ChimneyBrickDark = Color(0xFF6B3A26)
    val DoorWood = Color(0xFF7A4419)
    val BrassGold = Color(0xFFFFD166)

    fun drawTree(
        scope: DrawScope,
        baseX: Float,
        groundY: Float,
        p: Float = 3.5f,
        timeSeconds: Float = 0f,
        weather: com.example.scene.WeatherType = com.example.scene.WeatherType.SUNNY,
        mossStage: Int = 0,
        boyInitial: Char = 'Y',
        girlInitial: Char = 'M',
        gfInitial: Char = girlInitial
    ) {
        val trunkW = 18 * p
        val trunkH = 68 * p
        val trunkX = baseX - trunkW / 2f
        val trunkY = groundY - trunkH

        // Trunk & Roots
        scope.drawRect(TreeTrunk, Offset(trunkX, trunkY), Size(trunkW, trunkH))
        scope.drawRect(TreeTrunkDark, Offset(trunkX, trunkY), Size(4 * p, trunkH))
        // Root buttresses
        scope.drawRect(TreeTrunk, Offset(trunkX - 8 * p, groundY - 10 * p), Size(8 * p, 10 * p))
        scope.drawRect(TreeTrunkDark, Offset(trunkX - 8 * p, groundY - 4 * p), Size(8 * p, 4 * p))
        scope.drawRect(TreeTrunk, Offset(trunkX + trunkW, groundY - 8 * p), Size(7 * p, 8 * p))

        // Permanent carved heart with couple initials & ivy growth
        val effectiveGirlInitial = if (gfInitial != girlInitial) gfInitial else girlInitial
        drawTreeBarkCarving(scope, baseX, groundY, p, weather, mossStage, boyInitial, effectiveGirlInitial)

        // Canopy wind sway
        val sway = sin(timeSeconds * 1.5f) * 3f * p
        val canopyCx = baseX + sway
        val canopyCy = trunkY - 22 * p

        val isSnow = weather == com.example.scene.WeatherType.SNOW
        val isSakura = weather == com.example.scene.WeatherType.SAKURA
        val isAutumn = weather == com.example.scene.WeatherType.AUTUMN

        val leafDarkColor = when {
            isSnow -> Color(0xFF1E3A2F)
            isSakura -> Color(0xFFC75D7B)
            isAutumn -> Color(0xFF8B1E1E)
            else -> LeavesDark
        }
        val leafMidColor = when {
            isSnow -> Color(0xFF2C5545)
            isSakura -> Color(0xFFF291B0)
            isAutumn -> Color(0xFFD46A28)
            else -> LeavesMid
        }
        val leafLightColor = when {
            isSnow -> Color(0xFF4A7C68)
            isSakura -> Color(0xFFFFB5C2)
            isAutumn -> Color(0xFFF4B942)
            else -> LeavesLight
        }

        // Layered circular pixel clouds of leaves
        fun leafCluster(cx: Float, cy: Float, radiusP: Float, color: Color) {
            val r = radiusP * p
            scope.drawRect(color, Offset(cx - r, cy - r), Size(r * 2, r * 2))
            scope.drawRect(color, Offset(cx - r - 3 * p, cy - r + 3 * p), Size(3 * p, (r * 2) - 6 * p))
            scope.drawRect(color, Offset(cx + r, cy - r + 3 * p), Size(3 * p, (r * 2) - 6 * p))
            scope.drawRect(color, Offset(cx - r + 3 * p, cy - r - 3 * p), Size((r * 2) - 6 * p, 3 * p))
            scope.drawRect(color, Offset(cx - r + 3 * p, cy + r), Size((r * 2) - 6 * p, 3 * p))

            if (isSnow) {
                // Fluffy snow caps on top of foliage cluster
                val capW = (r * 2) - 4 * p
                scope.drawRect(Color(0xFFE2EAF0), Offset(cx - r + 2 * p, cy - r - 2 * p), Size(capW, 4 * p))
                scope.drawRect(Color.White, Offset(cx - r + 4 * p, cy - r - 4 * p), Size(capW - 4 * p, 3 * p))
                scope.drawRect(Color.White, Offset(cx - 2 * p, cy - r + 2 * p), Size(3 * p, 2.5f * p))
            }
        }

        // Back / Dark leaves layer
        leafCluster(canopyCx - 26 * p, canopyCy + 8 * p, 22f, leafDarkColor)
        leafCluster(canopyCx + 26 * p, canopyCy + 10 * p, 24f, leafDarkColor)
        leafCluster(canopyCx, canopyCy - 18 * p, 28f, leafDarkColor)

        // Mid foliage
        leafCluster(canopyCx - 20 * p, canopyCy + 4 * p, 20f, leafMidColor)
        leafCluster(canopyCx + 20 * p, canopyCy + 5 * p, 21f, leafMidColor)
        leafCluster(canopyCx, canopyCy - 10 * p, 24f, leafMidColor)

        // Front / Sun highlights
        leafCluster(canopyCx - 12 * p, canopyCy - 8 * p, 15f, leafLightColor)
        leafCluster(canopyCx + 14 * p, canopyCy - 4 * p, 16f, leafLightColor)
        leafCluster(canopyCx, canopyCy + 6 * p, 16f, leafLightColor)

        if (isSakura) {
            // Blooming cherry blossoms with bright white-pink petals and golden pistils
            val blossomPetal = Color(0xFFFFF0F5)
            val blossomDeep = Color(0xFFFF758F)
            TREE_FLOWER_OFFSETS.forEach { (ox, oy) ->
                val fx = canopyCx + ox * p
                val fy = canopyCy + oy * p
                scope.drawRect(blossomDeep, Offset(fx - 2 * p, fy - 2 * p), Size(5 * p, 5 * p))
                scope.drawRect(blossomPetal, Offset(fx - 3 * p, fy), Size(2 * p, 2 * p))
                scope.drawRect(blossomPetal, Offset(fx + 2 * p, fy), Size(2 * p, 2 * p))
                scope.drawRect(blossomPetal, Offset(fx, fy - 3 * p), Size(2 * p, 2 * p))
                scope.drawRect(blossomPetal, Offset(fx, fy + 2 * p), Size(2 * p, 2 * p))
                scope.drawRect(Color(0xFFFFE66D), Offset(fx, fy), Size(2 * p, 2 * p))
            }
        } else if (isAutumn) {
            // Golden and amber autumn leaf highlights
            val autumnGold = Color(0xFFFFD166)
            val autumnScarlet = Color(0xFFD62828)
            TREE_LEAF_ACCENT_OFFSETS.forEachIndexed { i, (ox, oy) ->
                val lx = canopyCx + ox * p
                val ly = canopyCy + oy * p
                val col = if (i % 2 == 0) autumnGold else autumnScarlet
                scope.drawRect(col, Offset(lx, ly), Size(4 * p, 4 * p))
            }
        } else if (isSnow) {
            // Snow on branches and roots
            scope.drawRect(Color.White, Offset(trunkX - 8 * p, groundY - 11 * p), Size(8 * p, 3 * p))
            scope.drawRect(Color.White, Offset(trunkX + trunkW, groundY - 9 * p), Size(7 * p, 3 * p))
            scope.drawRect(Color(0xFFE2EAF0), Offset(canopyCx + 26 * p, canopyCy + 18 * p), Size(7 * p, 2.5f * p))
        } else {
            // Pink cherry blossoms / sweet flower clusters for sunny days
            val blossomColor = LeavesBlossomPink
            scope.drawRect(blossomColor, Offset(canopyCx - 16 * p, canopyCy - 14 * p), Size(3 * p, 3 * p))
            scope.drawRect(blossomColor, Offset(canopyCx + 18 * p, canopyCy + 2 * p), Size(3 * p, 3 * p))
            scope.drawRect(blossomColor, Offset(canopyCx - 24 * p, canopyCy + 8 * p), Size(3 * p, 3 * p))
            scope.drawRect(blossomColor, Offset(canopyCx + 8 * p, canopyCy - 22 * p), Size(3 * p, 3 * p))
            scope.drawRect(blossomColor, Offset(canopyCx - 4 * p, canopyCy + 14 * p), Size(3 * p, 3 * p))
        }
    }

    /**
     * Tree Bark Growth — Permanent carved heart with couple initials
     * and moss/ivy growth advancing with elapsed calendar years (stages 0 to 4).
     */
    private fun drawTreeBarkCarving(
        scope: DrawScope,
        baseX: Float,
        groundY: Float,
        p: Float,
        weather: com.example.scene.WeatherType,
        mossStage: Int,
        boyInitial: Char = 'Y',
        girlInitial: Char = 'M'
    ) {
        val cx = baseX + 1.5f * p
        val cy = groundY - 29f * p

        val carveGroove = Color(0xFF381A08)     // Deep carved groove tone
        val carveHighlight = Color(0xFF7A4824)  // Outer carved wood lip highlight
        val initialColor = Color(0xFF5A2A10)    // Inset initials tone

        // 1. Carved Wooden Heart Base (outline & groove)
        scope.drawRect(carveHighlight, Offset(cx - 5f * p, cy - 4.5f * p), Size(4f * p, 2f * p))
        scope.drawRect(carveHighlight, Offset(cx + 1f * p, cy - 4.5f * p), Size(4f * p, 2f * p))
        scope.drawRect(carveGroove, Offset(cx - 4.5f * p, cy - 4f * p), Size(3.5f * p, 2f * p))
        scope.drawRect(carveGroove, Offset(cx + 1f * p, cy - 4f * p), Size(3.5f * p, 2f * p))
        scope.drawRect(carveGroove, Offset(cx - 1f * p, cy - 3f * p), Size(2f * p, 2f * p))
        scope.drawRect(carveGroove, Offset(cx - 5f * p, cy - 2f * p), Size(10f * p, 3.5f * p))
        scope.drawRect(carveHighlight, Offset(cx - 5.5f * p, cy - 1.5f * p), Size(1f * p, 3f * p))
        scope.drawRect(carveHighlight, Offset(cx + 4.5f * p, cy - 1.5f * p), Size(1f * p, 3f * p))
        scope.drawRect(carveGroove, Offset(cx - 4f * p, cy + 1.5f * p), Size(8f * p, 2f * p))
        scope.drawRect(carveGroove, Offset(cx - 2.5f * p, cy + 3.5f * p), Size(5f * p, 1.8f * p))
        scope.drawRect(carveGroove, Offset(cx - 1f * p, cy + 5.3f * p), Size(2f * p, 1.4f * p))
        scope.drawRect(carveHighlight, Offset(cx - 0.5f * p, cy + 6.7f * p), Size(1f * p, 1f * p))

        // 2. Initials Carving: Left boyInitial, Center Heart notch, Right girlInitial
        scope.drawRect(Color(0xFF8B3A4A), Offset(cx - 0.5f * p, cy - 0.5f * p), Size(1f * p, 1.5f * p))

        fun drawLetter(initial: Char, rx: Float, ry: Float) {
            when (initial.uppercaseChar()) {
                'A' -> {
                    scope.drawRect(initialColor, Offset(rx, ry), Size(2.2f * p, 0.8f * p))
                    scope.drawRect(initialColor, Offset(rx, ry + 0.8f * p), Size(0.8f * p, 3.4f * p))
                    scope.drawRect(initialColor, Offset(rx + 1.4f * p, ry + 0.8f * p), Size(0.8f * p, 3.4f * p))
                    scope.drawRect(initialColor, Offset(rx, ry + 2f * p), Size(2.2f * p, 0.8f * p))
                }
                'M' -> {
                    scope.drawRect(initialColor, Offset(rx, ry), Size(0.8f * p, 4.2f * p))
                    scope.drawRect(initialColor, Offset(rx + 1.6f * p, ry), Size(0.8f * p, 4.2f * p))
                    scope.drawRect(initialColor, Offset(rx + 0.8f * p, ry + 1f * p), Size(0.8f * p, 1.2f * p))
                }
                'Y' -> {
                    scope.drawRect(initialColor, Offset(rx, ry), Size(0.8f * p, 2.0f * p))
                    scope.drawRect(initialColor, Offset(rx + 1.4f * p, ry), Size(0.8f * p, 2.0f * p))
                    scope.drawRect(initialColor, Offset(rx + 0.7f * p, ry + 2.0f * p), Size(0.8f * p, 2.2f * p))
                }
                'H' -> {
                    scope.drawRect(initialColor, Offset(rx, ry), Size(0.8f * p, 4.2f * p))
                    scope.drawRect(initialColor, Offset(rx + 1.4f * p, ry), Size(0.8f * p, 4.2f * p))
                    scope.drawRect(initialColor, Offset(rx, ry + 2.0f * p), Size(2.2f * p, 0.8f * p))
                }
                'S' -> {
                    scope.drawRect(initialColor, Offset(rx, ry), Size(2.2f * p, 0.8f * p))
                    scope.drawRect(initialColor, Offset(rx, ry + 0.8f * p), Size(0.8f * p, 0.9f * p))
                    scope.drawRect(initialColor, Offset(rx, ry + 1.7f * p), Size(2.2f * p, 0.8f * p))
                    scope.drawRect(initialColor, Offset(rx + 1.4f * p, ry + 2.5f * p), Size(0.8f * p, 0.9f * p))
                    scope.drawRect(initialColor, Offset(rx, ry + 3.4f * p), Size(2.2f * p, 0.8f * p))
                }
                else -> {
                    scope.drawRect(initialColor, Offset(rx, ry), Size(2.4f * p, 0.8f * p))
                    scope.drawRect(initialColor, Offset(rx + 0.8f * p, ry + 0.8f * p), Size(0.8f * p, 3.4f * p))
                }
            }
        }

        // Left initial
        drawLetter(boyInitial, cx - 3.8f * p, cy - 2f * p)
        // Right initial
        drawLetter(girlInitial, cx + 1.6f * p, cy - 2f * p)

        // 3. Moss & Ivy Growth Stages (0 = none, 1..4 = progressive natural growth)
        if (mossStage <= 0) return

        val mossDark = Color(0xFF2E531B)
        val mossMid = Color(0xFF4C7C38)
        val mossLight = Color(0xFF6B9E5A)
        val ivyLeaf = Color(0xFF7DC26A)

        // Stage 1+: Tender moss patch at bottom V-tip and cleft
        scope.drawRect(mossDark, Offset(cx - 2f * p, cy + 6f * p), Size(4f * p, 1.8f * p))
        scope.drawRect(mossMid, Offset(cx - 1.2f * p, cy + 6.8f * p), Size(2.4f * p, 1.2f * p))
        scope.drawRect(mossLight, Offset(cx - 0.6f * p, cy + 7.5f * p), Size(1.2f * p, 0.9f * p))
        scope.drawRect(mossMid, Offset(cx - 0.8f * p, cy - 3.8f * p), Size(1.6f * p, 1.2f * p))

        // Stage 2+: Moss spreads along lower left & right curves with tiny ivy buds
        if (mossStage >= 2) {
            scope.drawRect(mossMid, Offset(cx - 5.5f * p, cy + 0.5f * p), Size(1.6f * p, 2f * p))
            scope.drawRect(mossDark, Offset(cx - 4.5f * p, cy + 2.5f * p), Size(1.5f * p, 1.8f * p))
            scope.drawRect(ivyLeaf, Offset(cx - 6.2f * p, cy + 0.8f * p), Size(1.2f * p, 1.2f * p))

            scope.drawRect(mossMid, Offset(cx + 4f * p, cy + 0.5f * p), Size(1.6f * p, 2f * p))
            scope.drawRect(mossDark, Offset(cx + 3.2f * p, cy + 2.5f * p), Size(1.5f * p, 1.8f * p))
            scope.drawRect(ivyLeaf, Offset(cx + 5f * p, cy + 0.8f * p), Size(1.2f * p, 1.2f * p))
        }

        // Stage 3+: Ivy tendrils climb up the flanks toward top lobes
        if (mossStage >= 3) {
            scope.drawRect(mossDark, Offset(cx - 6f * p, cy - 1.8f * p), Size(1.2f * p, 2.5f * p))
            scope.drawRect(ivyLeaf, Offset(cx - 6.8f * p, cy - 2.8f * p), Size(1.5f * p, 1.5f * p))
            scope.drawRect(mossLight, Offset(cx - 5.5f * p, cy - 3.8f * p), Size(1.5f * p, 1.4f * p))

            scope.drawRect(mossDark, Offset(cx + 4.8f * p, cy - 1.8f * p), Size(1.2f * p, 2.5f * p))
            scope.drawRect(ivyLeaf, Offset(cx + 5.3f * p, cy - 2.8f * p), Size(1.5f * p, 1.5f * p))
            scope.drawRect(mossLight, Offset(cx + 4f * p, cy - 3.8f * p), Size(1.5f * p, 1.4f * p))
        }

        // Stage 4: Full Ivy Crown Garland arching across the top with seasonal tint adaptation
        if (mossStage >= 4) {
            val seasonalAccent = when (weather) {
                com.example.scene.WeatherType.AUTUMN -> Color(0xFFE09F3E)
                com.example.scene.WeatherType.SNOW   -> Color(0xFFE8EEF5)
                else -> Color(0xFF98DF76)
            }
            scope.drawRect(mossDark, Offset(cx - 4.5f * p, cy - 5.2f * p), Size(9f * p, 1.2f * p))
            scope.drawRect(ivyLeaf, Offset(cx - 3.5f * p, cy - 5.8f * p), Size(2f * p, 1.4f * p))
            scope.drawRect(ivyLeaf, Offset(cx + 1.5f * p, cy - 5.8f * p), Size(2f * p, 1.4f * p))
            scope.drawRect(seasonalAccent, Offset(cx - 4f * p, cy - 6.2f * p), Size(1.2f * p, 1.2f * p))
            scope.drawRect(seasonalAccent, Offset(cx + 2.8f * p, cy - 6.2f * p), Size(1.2f * p, 1.2f * p))
            scope.drawRect(seasonalAccent, Offset(cx - 0.6f * p, cy - 5.2f * p), Size(1.2f * p, 1f * p))
        }
    }

    /**
     * Draw the Cozy Couple's Cottage House in exterior scenes.
     */
    fun drawCottage(
        scope: DrawScope,
        cx: Float,
        groundY: Float,
        p: Float = 3.5f,
        timeSeconds: Float = 0f,
        isNight: Boolean = false,
        weather: com.example.scene.WeatherType = com.example.scene.WeatherType.SUNNY
    ) {
        val houseW = 60 * p
        val wallH = 34 * p
        val houseLeft = cx - houseW / 2f
        val wallTop = groundY - wallH
        val isSnow = weather == com.example.scene.WeatherType.SNOW

        // Chimney on left side
        val chimneyW = 10 * p
        val chimneyH = 22 * p
        val chimneyLeft = houseLeft + 6 * p
        val chimneyTop = wallTop - 20 * p
        scope.drawRect(ChimneyBrickDark, Offset(chimneyLeft, chimneyTop), Size(chimneyW, chimneyH))
        scope.drawRect(ChimneyBrick, Offset(chimneyLeft + 2 * p, chimneyTop), Size(chimneyW - 4 * p, chimneyH))
        scope.drawRect(ChimneyBrickDark, Offset(chimneyLeft - 2 * p, chimneyTop), Size(chimneyW + 4 * p, 3 * p)) // cap
        if (isSnow) {
            // Fluffy snow mound on chimney cap
            scope.drawRect(Color.White, Offset(chimneyLeft - 3 * p, chimneyTop - 3 * p), Size(chimneyW + 6 * p, 3 * p))
            scope.drawRect(Color(0xFFE2EAF0), Offset(chimneyLeft - 2 * p, chimneyTop), Size(chimneyW + 4 * p, 1.5f * p))
        }

        // Stone foundation
        scope.drawRect(Color(0xFF6C757D), Offset(houseLeft - 2 * p, groundY - 4 * p), Size(houseW + 4 * p, 4 * p))
        if (isSnow) {
            // Snow accumulation at base of cottage
            scope.drawRect(Color.White, Offset(houseLeft - 4 * p, groundY - 3 * p), Size(houseW + 8 * p, 3 * p))
            scope.drawRect(Color(0xFFE2EAF0), Offset(houseLeft - 2 * p, groundY), Size(houseW + 4 * p, 1.5f * p))
        }

        // Main Wall (warm cream stucco)
        scope.drawRect(WallStucco, Offset(houseLeft, wallTop), Size(houseW, wallH - 4 * p))
        // Timber framing beams
        scope.drawRect(WallTimber, Offset(houseLeft, wallTop), Size(3 * p, wallH - 4 * p))
        scope.drawRect(WallTimber, Offset(houseLeft + houseW - 3 * p, wallTop), Size(3 * p, wallH - 4 * p))
        scope.drawRect(WallTimber, Offset(houseLeft, wallTop), Size(houseW, 3 * p))
        scope.drawRect(WallTimber, Offset(houseLeft + 26 * p, wallTop), Size(3 * p, wallH - 4 * p))

        // Terracotta pitched roof
        val roofOverhang = 8 * p
        val roofPeakY = wallTop - 22 * p
        val roofW = houseW + roofOverhang * 2
        // Draw tiered pixel roof
        for (i in 0 until 12) {
            val tierY = wallTop - i * 2 * p
            val inset = i * 2.5f * p
            scope.drawRect(
                if (i % 2 == 0) RoofTerracotta else RoofTerracottaDark,
                Offset(houseLeft - roofOverhang + inset, tierY),
                Size(roofW - inset * 2, 2 * p)
            )
            if (isSnow) {
                // Blanket of snow covering each roof tier
                val snowThick = if (i >= 8) 3 * p else 2 * p
                scope.drawRect(
                    Color.White,
                    Offset(houseLeft - roofOverhang + inset, tierY - p),
                    Size(roofW - inset * 2, snowThick)
                )
                scope.drawRect(
                    Color(0xFFE2EAF0),
                    Offset(houseLeft - roofOverhang + inset + 2 * p, tierY),
                    Size(roofW - inset * 2 - 4 * p, p)
                )
            }
        }

        if (isSnow) {
            // Hanging icicles from roof eaves
            COTTAGE_ICICLE_FRACS.forEachIndexed { idx, frac ->
                val ix = houseLeft - roofOverhang + roofW * frac
                val iLen = (3f + (idx % 3) * 2f) * p
                scope.drawRect(Color(0xEEFFFFFF), Offset(ix, wallTop + 2 * p), Size(2 * p, iLen))
                scope.drawRect(Color(0xDDE0F2FE), Offset(ix + 0.5f * p, wallTop + 2 * p + iLen), Size(p, 2 * p))
            }
        }

        // Attic Round Window
        val atticWindowX = cx
        val atticWindowY = wallTop - 8 * p
        scope.drawRect(WallTimber, Offset(atticWindowX - 4 * p, atticWindowY - 4 * p), Size(8 * p, 8 * p))
        scope.drawRect(if (isNight) Color(0xFFFFD166) else Color(0xFFA2D2FF), Offset(atticWindowX - 3 * p, atticWindowY - 3 * p), Size(6 * p, 6 * p))

        // Wooden Front Door (Enterable!)
        val doorW = 12 * p
        val doorH = 20 * p
        val doorX = houseLeft + 10 * p
        val doorY = groundY - doorH - 4 * p
        scope.drawRect(WallTimber, Offset(doorX - p, doorY - p), Size(doorW + 2 * p, doorH + 2 * p))
        scope.drawRect(DoorWood, Offset(doorX, doorY), Size(doorW, doorH))
        // Door panels & brass knocker
        scope.drawRect(Color(0xFF5B3012), Offset(doorX + 2 * p, doorY + 3 * p), Size(8 * p, 6 * p))
        scope.drawRect(Color(0xFF5B3012), Offset(doorX + 2 * p, doorY + 11 * p), Size(8 * p, 6 * p))
        scope.drawRect(BrassGold, Offset(doorX + 9 * p, doorY + 10 * p), Size(2 * p, 2 * p)) // knob

        // Glowing Ground Window
        val winW = 14 * p
        val winH = 14 * p
        val winX = houseLeft + 36 * p
        val winY = wallTop + 6 * p
        scope.drawRect(WallTimber, Offset(winX - p, winY - p), Size(winW + 2 * p, winH + 2 * p))
        val winGlass = if (isNight) Color(0xFFFFE66D) else Color(0xFFBDE0FE)
        scope.drawRect(winGlass, Offset(winX, winY), Size(winW, winH))
        // Window mullions
        scope.drawRect(WallTimber, Offset(winX + winW / 2f - p, winY), Size(2 * p, winH))
        scope.drawRect(WallTimber, Offset(winX, winY + winH / 2f - p), Size(winW, 2 * p))
        // Window flower box
        scope.drawRect(Color(0xFF7F5539), Offset(winX - p, winY + winH), Size(winW + 2 * p, 3 * p))
        if (isSnow) {
            // Snow pillow on the flower box
            scope.drawRect(Color.White, Offset(winX - 2 * p, winY + winH - 2 * p), Size(winW + 4 * p, 3 * p))
        } else if (weather == com.example.scene.WeatherType.SAKURA) {
            // Blooming pink cherry blossoms in flower box
            scope.drawRect(Color(0xFFFFB5C2), Offset(winX + 2 * p, winY + winH - 2 * p), Size(3 * p, 2 * p))
            scope.drawRect(Color(0xFFFF758F), Offset(winX + 6 * p, winY + winH - 2 * p), Size(3 * p, 2 * p))
            scope.drawRect(Color(0xFFFFF0F5), Offset(winX + 10 * p, winY + winH - 2 * p), Size(3 * p, 2 * p))
        } else {
            scope.drawRect(Color(0xFFFF4D6D), Offset(winX + 2 * p, winY + winH - 2 * p), Size(3 * p, 2 * p))
            scope.drawRect(Color(0xFFFFCAD4), Offset(winX + 6 * p, winY + winH - 2 * p), Size(3 * p, 2 * p))
            scope.drawRect(Color(0xFFFFE66D), Offset(winX + 10 * p, winY + winH - 2 * p), Size(3 * p, 2 * p))
        }
    }

    /**
     * Draw the cute Kitty Cat ("Mochi").
     */
    fun drawCat(
        scope: DrawScope,
        cx: Float,
        groundY: Float,
        p: Float = 3.5f,
        timeSeconds: Float = 0f,
        catState: com.example.scene.CatState = com.example.scene.CatState.SLEEPING,
        isSleeping: Boolean = catState == com.example.scene.CatState.SLEEPING,
        isSnow: Boolean = false,
        facingLeft: Boolean = false
    ) {
        scope.withTransform({
            if (facingLeft) {
                scale(-1f, 1f, Offset(cx, groundY))
            }
        }) {
            val catWhite = Color(0xFFF8F9FA)
            val catGinger = Color(0xFFF77F00)
            val catDark = Color(0xFF343A40)
            val catPink = Color(0xFFFFCAD4)
            val catGreen = Color(0xFF2B9348)
            val scarfRed = Color(0xFFD90429)
            val scarfDark = Color(0xFFA0001E)
            val scarfFringe = Color(0xFFFF4D6D)

            val effectiveState = if (!isSleeping && catState == com.example.scene.CatState.SLEEPING) {
                com.example.scene.CatState.SITTING_PURR
            } else {
                catState
            }

            // Grounding contact drop-shadow beneath Mochi
            val catShadowW = when (effectiveState) {
                com.example.scene.CatState.BELLY_ROLL -> 15f * p
                com.example.scene.CatState.SLEEPING -> 13f * p
                com.example.scene.CatState.PLAYFUL_POUNCE -> 14f * p
                else -> 11f * p
            }
            val catShadowH = 3.4f * p
            scope.drawOval(
                color = Color(0xFF151820).copy(alpha = 0.24f),
                topLeft = Offset(cx - catShadowW / 2f, groundY - catShadowH * 0.65f),
                size = Size(catShadowW, catShadowH)
            )

            when (effectiveState) {
            com.example.scene.CatState.SLEEPING -> {
                // Curled sleeping loaf with peaceful living breathing, ear twitch, and curled tail flick
                val breath = (sin(timeSeconds * 2.2f) * 1.5f * p).coerceAtLeast(0f)
                val bodyW = 13 * p
                val bodyH = 8 * p
                val left = cx - bodyW / 2f
                val top = groundY - bodyH - breath

                // Body loaf with rhythmic breathing expansion
                scope.drawRect(catWhite, Offset(left, top + p), Size(bodyW, bodyH - p + breath))
                scope.drawRect(catGinger, Offset(left + 2 * p, top + p), Size(5 * p, 3.5f * p))

                // Curled tail wrapped around front with gentle lazy tail flick
                val tailFlick = sin(timeSeconds * 1.8f) * 1.4f * p
                scope.drawRect(catGinger, Offset(left - 2 * p, top + 4 * p + tailFlick * 0.4f), Size(2.5f * p, 3.5f * p))
                scope.drawRect(catWhite, Offset(left - 3 * p + tailFlick * 0.8f, top + 2.5f * p), Size(2 * p, 2 * p))

                // Sleeping head with gentle ear twitch
                val earTwitch = if (sin(timeSeconds * 0.9f) > 0.80f) sin(timeSeconds * 16f) * 0.6f * p else 0f
                scope.drawRect(catWhite, Offset(left + 8 * p, top + 1 * p), Size(4.5f * p, 4.5f * p))
                scope.drawRect(catGinger, Offset(left + 9 * p + earTwitch, top - 1.5f * p), Size(2 * p, 2.5f * p))
                scope.drawRect(catPink, Offset(left + 9.5f * p + earTwitch, top - 0.5f * p), Size(p, 1.5f * p))
                scope.drawRect(catWhite, Offset(left + 11.5f * p - earTwitch * 0.5f, top - 1.5f * p), Size(2 * p, 2.5f * p))

                // Soft closed sleeping eye and cute pink nose
                scope.drawRect(catDark, Offset(left + 9 * p, top + 3 * p), Size(2.5f * p, p))
                scope.drawRect(catPink, Offset(left + 11.5f * p, top + 4 * p), Size(p, p))

                if (isSnow) {
                    // Festive warm red knitted winter scarf around sleeping cat loaf
                    scope.drawRect(scarfRed, Offset(left + 6.8f * p, top + 1.8f * p), Size(2.4f * p, 4.2f * p))
                    scope.drawRect(scarfDark, Offset(left + 6.8f * p, top + 1.8f * p), Size(2.4f * p, 0.8f * p))
                    scope.drawRect(scarfRed, Offset(left + 5.2f * p, top + 4.2f * p), Size(2.2f * p, 2.5f * p))
                    scope.drawRect(scarfFringe, Offset(left + 4.8f * p, top + 6.2f * p), Size(2.6f * p, 1f * p))
                }
            }
            com.example.scene.CatState.SITTING_PURR -> {
                // Upright sitting cat, happy smiling squint eyes, vibrating purr, wagging tail
                val purrShake = sin(timeSeconds * 25f) * 0.4f * p
                val bodyW = 12 * p
                val bodyH = 13 * p
                val left = cx - bodyW / 2f + purrShake
                val top = groundY - bodyH

                // Front paws sitting
                scope.drawRect(catWhite, Offset(left + 3 * p, top + 9 * p), Size(2.5f * p, 4 * p))
                scope.drawRect(catWhite, Offset(left + 6.5f * p, top + 9 * p), Size(2.5f * p, 4 * p))

                // Sitting body
                scope.drawRect(catWhite, Offset(left + 2 * p, top + 4 * p), Size(8 * p, 6 * p))
                scope.drawRect(catGinger, Offset(left + 3 * p, top + 4 * p), Size(4 * p, 4 * p))

                // Head
                scope.drawRect(catWhite, Offset(left + 1 * p, top - 2 * p), Size(10 * p, 7 * p))
                // Ears twitching
                val earTwitch = sin(timeSeconds * 6f) * 0.5f * p
                scope.drawRect(catGinger, Offset(left + 1 * p + earTwitch, top - 5 * p), Size(3 * p, 3.5f * p))
                scope.drawRect(catPink, Offset(left + 1.8f * p + earTwitch, top - 4 * p), Size(1.5f * p, 2 * p))
                scope.drawRect(catWhite, Offset(left + 8 * p - earTwitch, top - 5 * p), Size(3 * p, 3.5f * p))
                scope.drawRect(catPink, Offset(left + 8.8f * p - earTwitch, top - 4 * p), Size(1.5f * p, 2 * p))

                // Happy curved closed smiling eyes (^ ^)
                scope.drawRect(catDark, Offset(left + 3 * p, top), Size(2 * p, p))
                scope.drawRect(catDark, Offset(left + 2.5f * p, top + p), Size(p, p))
                scope.drawRect(catDark, Offset(left + 7 * p, top), Size(2 * p, p))
                scope.drawRect(catDark, Offset(left + 8.5f * p, top + p), Size(p, p))

                // Pink nose & smile
                scope.drawRect(catPink, Offset(left + 5.5f * p, top + 2 * p), Size(1.2f * p, p))
                scope.drawRect(catDark, Offset(left + 5 * p, top + 3.2f * p), Size(2.2f * p, 0.8f * p))

                // Rosy blushing cheeks
                scope.drawRect(Color(0xFFFF85A1), Offset(left + 2 * p, top + 2 * p), Size(2 * p, 1.2f * p))
                scope.drawRect(Color(0xFFFF85A1), Offset(left + 8 * p, top + 2 * p), Size(2 * p, 1.2f * p))

                // Joyful tail wagging high
                val tailWave = sin(timeSeconds * 6f) * 3.5f * p
                scope.drawRect(catGinger, Offset(left + 9 * p, top + 5 * p), Size(2 * p, 4 * p))
                scope.drawRect(catGinger, Offset(left + 10 * p + tailWave, top + 1 * p), Size(2.5f * p, 4.5f * p))
                scope.drawRect(catWhite, Offset(left + 11 * p + tailWave * 1.3f, top - 2 * p), Size(2 * p, 3 * p))

                if (isSnow) {
                    // Knitted red winter scarf around sitting cat's neck
                    scope.drawRect(scarfRed, Offset(left + 1.8f * p, top + 3.4f * p), Size(8.4f * p, 2.6f * p))
                    scope.drawRect(scarfDark, Offset(left + 1.8f * p, top + 5.2f * p), Size(8.4f * p, 0.8f * p))
                    // Scarf knot
                    scope.drawRect(scarfDark, Offset(left + 3.5f * p, top + 4.5f * p), Size(2.2f * p, 2f * p))
                    // Trailing scarf tail with fringe
                    val knotSway = sin(timeSeconds * 4f) * 0.5f * p
                    scope.drawRect(scarfRed, Offset(left + 3.2f * p + knotSway, top + 6.2f * p), Size(2.2f * p, 4.2f * p))
                    scope.drawRect(scarfFringe, Offset(left + 3f * p + knotSway, top + 10f * p), Size(2.6f * p, 1.2f * p))
                }
            }
            com.example.scene.CatState.BELLY_ROLL -> {
                // Playful belly roll onto back! Waving paws in the air
                val bodyW = 16 * p
                val bodyH = 9 * p
                val left = cx - bodyW / 2f
                val top = groundY - bodyH

                // Body on back
                scope.drawRect(catGinger, Offset(left + 3 * p, top + 3 * p), Size(10 * p, 5 * p))
                scope.drawRect(catWhite, Offset(left + 4 * p, top + 2 * p), Size(8 * p, 4 * p)) // fluffy white belly!
                scope.drawRect(catGinger, Offset(left + 6 * p, top + 3 * p), Size(2 * p, 2 * p)) // tummy spot

                // Head tilted sideways
                scope.drawRect(catWhite, Offset(left - p, top + 2 * p), Size(5.5f * p, 5.5f * p))
                scope.drawRect(catGinger, Offset(left - 2 * p, top), Size(2 * p, 2.5f * p))
                scope.drawRect(catWhite, Offset(left + p, top), Size(2 * p, 2.5f * p))
                // Big happy eyes looking up
                scope.drawRect(catGreen, Offset(left, top + 3.5f * p), Size(1.5f * p, 1.5f * p))
                scope.drawRect(catPink, Offset(left + 1.5f * p, top + 5 * p), Size(p, p))

                // Front paws waving playfully in air!
                val pawWave1 = sin(timeSeconds * 9f) * 2f * p
                val pawWave2 = cos(timeSeconds * 9f) * 2f * p
                scope.drawRect(catWhite, Offset(left + 4 * p, top - 2 * p + pawWave1), Size(2 * p, 4 * p))
                scope.drawRect(catPink, Offset(left + 4.5f * p, top - 2.5f * p + pawWave1), Size(1.2f * p, 1.2f * p)) // toe beans
                scope.drawRect(catWhite, Offset(left + 9 * p, top - 2 * p + pawWave2), Size(2 * p, 4 * p))
                scope.drawRect(catPink, Offset(left + 9.5f * p, top - 2.5f * p + pawWave2), Size(1.2f * p, 1.2f * p))

                // Back feet sticking out
                scope.drawRect(catWhite, Offset(left + 13 * p, top + 3 * p), Size(2.5f * p, 3 * p))
                scope.drawRect(catPink, Offset(left + 14 * p, top + 4 * p), Size(1.2f * p, 1.2f * p))

                // Curled tail on the ground
                val tailTwitch = sin(timeSeconds * 7f) * 1.5f * p
                scope.drawRect(catGinger, Offset(left + 12 * p, top + 6 * p), Size(3.5f * p, 2 * p))
                scope.drawRect(catGinger, Offset(left + 15 * p + tailTwitch, top + 4 * p), Size(2 * p, 3 * p))

                if (isSnow) {
                    scope.drawRect(scarfRed, Offset(left + 2f * p, top + 2.5f * p), Size(2.4f * p, 4.2f * p))
                    scope.drawRect(scarfDark, Offset(left + 2.8f * p, top + 5.5f * p), Size(2.2f * p, 1.6f * p))
                }
            }
            com.example.scene.CatState.PLAYFUL_POUNCE -> {
                // Playful crouch and butt wiggle before a pounce!
                val bodyW = 15 * p
                val bodyH = 10 * p
                val left = cx - bodyW / 2f
                val top = groundY - bodyH
                val buttWiggle = sin(timeSeconds * 16f) * 1.6f * p

                // Paws planted low
                scope.drawRect(catWhite, Offset(left + 2 * p, top + 6 * p), Size(3 * p, 4 * p))
                scope.drawRect(catWhite, Offset(left + 9 * p, top + 6 * p + buttWiggle), Size(4 * p, 4 * p))

                // Crouched body with wiggling hips
                scope.drawRect(catGinger, Offset(left + 3 * p, top + 3 * p + buttWiggle * 0.7f), Size(9 * p, 5 * p))
                scope.drawRect(catWhite, Offset(left + 4 * p, top + 4 * p + buttWiggle * 0.7f), Size(5 * p, 3 * p))

                // Low alert head
                scope.drawRect(catWhite, Offset(left - p, top + 2 * p), Size(6 * p, 6 * p))
                scope.drawRect(catGinger, Offset(left - 2 * p, top), Size(2.5f * p, 3 * p)) // alert ear
                scope.drawRect(catWhite, Offset(left + 2 * p, top), Size(2.5f * p, 3 * p)) // alert ear

                // Dilated round green eyes focused forward
                scope.drawRect(catGreen, Offset(left, top + 3.5f * p), Size(1.8f * p, 1.8f * p))
                scope.drawRect(catDark, Offset(left + 0.4f * p, top + 3.9f * p), Size(p, p))
                scope.drawRect(catPink, Offset(left + 1.2f * p, top + 5.2f * p), Size(1.2f * p, p))

                // Tail held upright with twitching tip
                val tailSway = sin(timeSeconds * 14f) * 2.5f * p
                scope.drawRect(catGinger, Offset(left + 11 * p, top - 3 * p + buttWiggle), Size(2 * p, 7 * p))
                scope.drawRect(catWhite, Offset(left + 11 * p + tailSway, top - 6 * p + buttWiggle), Size(2.5f * p, 3.5f * p))

                if (isSnow) {
                    scope.drawRect(scarfRed, Offset(left + 2f * p, top + 3f * p), Size(2.4f * p, 4f * p))
                    val scarfFlutter = sin(timeSeconds * 12f) * 0.8f * p
                    scope.drawRect(scarfRed, Offset(left + 3.5f * p, top + 2f * p + scarfFlutter), Size(3f * p, 1.8f * p))
                    scope.drawRect(scarfFringe, Offset(left + 6.2f * p, top + 2f * p + scarfFlutter), Size(1.2f * p, 1.8f * p))
                }
            }
            com.example.scene.CatState.WALK_FOLLOW -> {
                // Cheerful walking cat following the couple
                val bodyW = 14 * p
                val bodyH = 12 * p
                val left = cx - bodyW / 2f
                val walkBounce = abs(sin(timeSeconds * 9f)) * 1.5f * p
                val top = groundY - bodyH - walkBounce

                val legSwing1 = sin(timeSeconds * 9f) * 2.2f * p
                val legSwing2 = -legSwing1

                // Four walking paws
                scope.drawRect(catWhite, Offset(left + 2 * p + legSwing1, top + 8 * p), Size(2 * p, 4 * p + walkBounce))
                scope.drawRect(catWhite, Offset(left + 4.5f * p + legSwing2, top + 8 * p), Size(2 * p, 4 * p + walkBounce))
                scope.drawRect(catWhite, Offset(left + 8.5f * p + legSwing2, top + 8 * p), Size(2 * p, 4 * p + walkBounce))
                scope.drawRect(catWhite, Offset(left + 11 * p + legSwing1, top + 8 * p), Size(2 * p, 4 * p + walkBounce))

                // Walking body
                scope.drawRect(catWhite, Offset(left + 2 * p, top + 3 * p), Size(10 * p, 6 * p))
                scope.drawRect(catGinger, Offset(left + 4 * p, top + 3 * p), Size(5 * p, 4 * p))

                // Head held proudly high
                scope.drawRect(catWhite, Offset(left + 8 * p, top - p), Size(6 * p, 6 * p))
                scope.drawRect(catGinger, Offset(left + 8 * p, top - 3.5f * p), Size(2 * p, 3 * p))
                scope.drawRect(catWhite, Offset(left + 11.5f * p, top - 3.5f * p), Size(2 * p, 3 * p))

                // Friendly smiling eyes and nose
                scope.drawRect(catGreen, Offset(left + 11 * p, top + p), Size(1.5f * p, 1.5f * p))
                scope.drawRect(catPink, Offset(left + 12.5f * p, top + 2.5f * p), Size(p, p))

                // Happy high tail with S-curve waving gently
                val tailWave = sin(timeSeconds * 7f) * 2f * p
                scope.drawRect(catGinger, Offset(left - p, top + p), Size(2 * p, 5 * p))
                scope.drawRect(catGinger, Offset(left - 2 * p + tailWave, top - 3 * p), Size(2.2f * p, 5 * p))
                scope.drawRect(catWhite, Offset(left - 1.5f * p + tailWave, top - 6 * p), Size(2.5f * p, 3.5f * p))

                if (isSnow) {
                    scope.drawRect(scarfRed, Offset(left + 7f * p, top + 1.2f * p), Size(2.5f * p, 4.5f * p))
                    val walkFlutter = sin(timeSeconds * 10f) * 1.4f * p
                    scope.drawRect(scarfRed, Offset(left + 4f * p, top + 2f * p + walkFlutter), Size(3.5f * p, 2f * p))
                    scope.drawRect(scarfFringe, Offset(left + 2.8f * p, top + 2f * p + walkFlutter), Size(1.4f * p, 2f * p))
                }
            }
        }
        }
    }

    fun drawCouch(
        scope: DrawScope,
        centerX: Float,
        groundY: Float,
        p: Float = 3.5f
    ) {
        val w = 58 * p
        val h = 28 * p
        val left = centerX - w / 2f
        val top = groundY - h

        // Couch Backrest
        scope.drawRect(CouchFabricDark, Offset(left, top), Size(w, 15 * p))
        scope.drawRect(CouchFabric, Offset(left + 2 * p, top + 2 * p), Size(w - 4 * p, 12 * p))

        // Couch Armrests
        scope.drawRect(CouchFabricDark, Offset(left, top + 8 * p), Size(8 * p, 16 * p))
        scope.drawRect(CouchFabricDark, Offset(left + w - 8 * p, top + 8 * p), Size(8 * p, 16 * p))
        scope.drawRect(CouchFabric, Offset(left + 2 * p, top + 9 * p), Size(5 * p, 14 * p))
        scope.drawRect(CouchFabric, Offset(left + w - 7 * p, top + 9 * p), Size(5 * p, 14 * p))

        // Seat Cushion
        scope.drawRect(CouchFabric, Offset(left + 7 * p, top + 14 * p), Size(w - 14 * p, 10 * p))
        scope.drawRect(CouchFabricDark, Offset(left + 7 * p, top + 23 * p), Size(w - 14 * p, 2 * p))

        // Heart throw pillow on left
        scope.drawRect(CushionPink, Offset(left + 9 * p, top + 10 * p), Size(9 * p, 9 * p))
        scope.drawRect(Color.White, Offset(left + 11 * p, top + 12 * p), Size(2 * p, 2 * p))

        // Wooden legs
        scope.drawRect(WoodFloorDark, Offset(left + 3 * p, top + 24 * p), Size(4 * p, 4 * p))
        scope.drawRect(WoodFloorDark, Offset(left + w - 7 * p, top + 24 * p), Size(4 * p, 4 * p))
    }

    fun drawKitchen(
        scope: DrawScope,
        counterX: Float,
        groundY: Float,
        p: Float = 3.5f,
        timeSeconds: Float = 0f,
        isNight: Boolean = false,
        cabinetOpen: Boolean = false
    ) {
        val counterW = 46 * p
        val counterH = 16 * p
        val left = counterX - counterW / 2f
        val top = groundY - counterH

        // 1. Wall Backsplash tiles - subway tile pattern behind counter
        val tileBg = if (isNight) Color(0xFFE2D6CA) else Color(0xFFF3ECE4)
        val tileGrout = if (isNight) Color(0xFFC7B9AC) else Color(0xFFDDD2C6)
        val tileW = counterW + 8 * p
        val tileH = 26 * p
        val tileLeft = left - 4 * p
        val tileTop = top - tileH
        scope.drawRect(tileBg, Offset(tileLeft, tileTop), Size(tileW, tileH))
        for (i in 0..5) {
            val gy = tileTop + i * 5 * p
            scope.drawRect(tileGrout, Offset(tileLeft, gy), Size(tileW, p))
            val offsetCol = (i % 2) * 6 * p
            var gx = tileLeft + offsetCol
            while (gx < tileLeft + tileW) {
                scope.drawRect(tileGrout, Offset(gx, gy), Size(p, 5 * p))
                gx += 12 * p
            }
        }

        // 2. Upper Open Oak Shelf with kitchen essentials
        val shelfY = top - 24 * p
        val shelfW = counterW + 4 * p
        val shelfX = left - 2 * p
        scope.drawRect(Color(0xFF8B5E3C), Offset(shelfX, shelfY), Size(shelfW, 2f * p))
        scope.drawRect(Color(0xFF6F4528), Offset(shelfX, shelfY + 2f * p), Size(shelfW, 0.8f * p))
        scope.drawRect(Color(0xFF4A3525), Offset(shelfX + 4 * p, shelfY + 2f * p), Size(1.5f * p, 3 * p))
        scope.drawRect(Color(0xFF4A3525), Offset(shelfX + shelfW - 5.5f * p, shelfY + 2f * p), Size(1.5f * p, 3 * p))

        // Couple mugs (boy's blue mug and girl's pink heart mug)
        val boyMugX = shelfX + 4 * p
        scope.drawRect(Color(0xFF457B9D), Offset(boyMugX, shelfY - 4.5f * p), Size(3.5f * p, 4.5f * p))
        scope.drawRect(Color(0xFF1D3557), Offset(boyMugX + 3.5f * p, shelfY - 3.5f * p), Size(1f * p, 2.5f * p))
        val girlMugX = shelfX + 10 * p
        scope.drawRect(Color(0xFFFFB5C2), Offset(girlMugX, shelfY - 4.5f * p), Size(3.5f * p, 4.5f * p))
        scope.drawRect(Color(0xFFE07A5F), Offset(girlMugX + 3.5f * p, shelfY - 3.5f * p), Size(1f * p, 2.5f * p))
        scope.drawRect(Color(0xFFFF4D6D), Offset(girlMugX + 1f * p, shelfY - 3f * p), Size(1.5f * p, 1.5f * p))

        // Glass Honey Jar
        val honeyX = shelfX + 18 * p
        scope.drawRect(Color(0xBBFAF0CA), Offset(honeyX, shelfY - 5 * p), Size(3.5f * p, 5 * p))
        scope.drawRect(Color(0xFFEE9B00), Offset(honeyX + 0.5f * p, shelfY - 4f * p), Size(2.5f * p, 3.5f * p))
        scope.drawRect(Color(0xFF7F5539), Offset(honeyX + 0.8f * p, shelfY - 6 * p), Size(2f * p, 1.2f * p))

        // Row of 3 Glass Spice Jars
        val spiceStartX = shelfX + 26 * p
        for (idx in 0 until KITCHEN_SPICE_COLORS.size) {
            val sCol = KITCHEN_SPICE_COLORS[idx]
            val sx = spiceStartX + idx * 4.5f * p
            scope.drawRect(Color(0xBBFAF0CA), Offset(sx, shelfY - 4.5f * p), Size(3f * p, 4.5f * p))
            scope.drawRect(sCol, Offset(sx + 0.5f * p, shelfY - 3f * p), Size(2f * p, 2.5f * p))
            scope.drawRect(Color(0xFFB08968), Offset(sx + 0.5f * p, shelfY - 5.5f * p), Size(2f * p, 1f * p))
        }

        // 3. Hanging utensil and cookware rack
        val rackY = top - 13 * p
        val rackW = counterW - 6 * p
        val rackX = left + 3 * p
        scope.drawRect(Color(0xFF4A4E69), Offset(rackX, rackY), Size(rackW, 1.4f * p))
        for (h in 0..3) {
            scope.drawRect(Color(0xFF22223B), Offset(rackX + 4 * p + h * 9 * p, rackY + 1.4f * p), Size(1f * p, 1.5f * p))
        }
        // Copper frying pan
        val panX = rackX + 4 * p
        scope.drawRect(Color(0xFFB85D19), Offset(panX - 2 * p, rackY + 3.5f * p), Size(5 * p, 5 * p))
        scope.drawRect(Color(0xFFD47C38), Offset(panX - 1.2f * p, rackY + 4.2f * p), Size(3.5f * p, 3.5f * p))
        scope.drawRect(Color(0xFF333333), Offset(panX, rackY + 2f * p), Size(1f * p, 2f * p))
        // Cast-iron skillet
        val ironX = rackX + 13 * p
        scope.drawRect(Color(0xFF2B2D42), Offset(ironX - 2 * p, rackY + 3.8f * p), Size(4.5f * p, 4.5f * p))
        scope.drawRect(Color(0xFF333333), Offset(ironX, rackY + 2f * p), Size(1f * p, 2f * p))
        // Wooden ladle
        val spoonX = rackX + 22 * p
        scope.drawRect(Color(0xFFB08968), Offset(spoonX, rackY + 2.5f * p), Size(1f * p, 6 * p))
        scope.drawRect(Color(0xFF7F5539), Offset(spoonX - 0.8f * p, rackY + 7.5f * p), Size(2.6f * p, 2.2f * p))
        // Wire whisk
        val whiskX = rackX + 31 * p
        scope.drawRect(Color(0xFF8D99AE), Offset(whiskX, rackY + 2.5f * p), Size(1f * p, 3 * p))
        scope.drawRect(Color(0xFFC0C0C0), Offset(whiskX - 0.8f * p, rackY + 5.5f * p), Size(2.6f * p, 3.5f * p))

        // 4. Main Counter Body & Cabinets
        scope.drawRect(Color(0xFFECE4DB), Offset(left, top), Size(counterW, counterH))
        // Countertop rim
        scope.drawRect(Color(0xFFFAF7F2), Offset(left, top), Size(counterW, 2 * p))
        scope.drawRect(Color(0xFFC8BDB0), Offset(left, top + 1.8f * p), Size(counterW, 0.8f * p))

        // Base cabinets - warm olive-sage
        val cabBg = Color(0xFF6B7F6E)
        val cabDark = Color(0xFF556658)
        val cabBevel = Color(0xFF819985)
        scope.drawRect(cabBg, Offset(left + 1.5f * p, top + 2.5f * p), Size(counterW - 3 * p, counterH - 2.5f * p))

        // Built-in Oven under the stove on left
        val ovenW = 20 * p
        val ovenH = counterH - 4.5f * p
        val ovenLeft = left + 2.5f * p
        val ovenTop = top + 3.5f * p
        scope.drawRect(cabDark, Offset(ovenLeft, ovenTop), Size(ovenW, ovenH))
        val ovenGlassBg = if (isNight) Color(0xFF2B1B17) else Color(0xFF3D261C)
        scope.drawRect(ovenGlassBg, Offset(ovenLeft + 2.5f * p, ovenTop + 3.5f * p), Size(ovenW - 5 * p, ovenH - 5.5f * p))
        // Warm interior baking glow
        val bakeGlow = Color(0x99FFAA00)
        scope.drawRect(bakeGlow, Offset(ovenLeft + 3.5f * p, ovenTop + 4.5f * p), Size(ovenW - 7 * p, ovenH - 7.5f * p))
        // Baking cookies inside
        scope.drawRect(Color(0xFFB08968), Offset(ovenLeft + 4.5f * p, ovenTop + 6.5f * p), Size(ovenW - 9 * p, 1.5f * p))
        scope.drawRect(Color(0xFFE9C46A), Offset(ovenLeft + 6 * p, ovenTop + 5.5f * p), Size(2.5f * p, 1.5f * p))
        scope.drawRect(Color(0xFFE9C46A), Offset(ovenLeft + 11 * p, ovenTop + 5.5f * p), Size(2.5f * p, 1.5f * p))
        // Oven door handle & knobs
        scope.drawRect(Color(0xFFE0E0E0), Offset(ovenLeft + 3 * p, ovenTop + 1.2f * p), Size(ovenW - 6 * p, 1.2f * p))
        scope.drawRect(Color(0xFF333333), Offset(ovenLeft + 4 * p, ovenTop + 0.2f * p), Size(1.2f * p, 0.9f * p))
        scope.drawRect(Color(0xFF333333), Offset(ovenLeft + 7 * p, ovenTop + 0.2f * p), Size(1.2f * p, 0.9f * p))

        // Right side cabinet drawers & doors
        val rightCabLeft = left + 24 * p
        val rightCabW = counterW - 26 * p
        scope.drawRect(cabDark, Offset(rightCabLeft, ovenTop), Size(rightCabW, ovenH))
        // Drawer
        scope.drawRect(cabBevel, Offset(rightCabLeft + 1.5f * p, ovenTop + 1 * p), Size(rightCabW - 3 * p, 3.5f * p))
        scope.drawRect(BrassGold, Offset(rightCabLeft + rightCabW / 2f - 1.5f * p, ovenTop + 2.2f * p), Size(3 * p, 1f * p))
        // Lower Cabinet Door
        if (cabinetOpen) {
            // Lower cabinet door swung open with neat baking dish & spices inside
            scope.drawRect(Color(0xFF332219), Offset(rightCabLeft + 1.5f * p, ovenTop + 5.5f * p), Size(rightCabW - 3 * p, ovenH - 6.5f * p))
            // Wooden interior shelf
            scope.drawRect(Color(0xFF7F5539), Offset(rightCabLeft + 2 * p, ovenTop + 9.5f * p), Size(rightCabW - 4 * p, 1.2f * p))
            // Ceramic mixing bowl & spice bottle inside
            scope.drawRect(Color(0xFFE76F51), Offset(rightCabLeft + 3f * p, ovenTop + 6.8f * p), Size(4 * p, 2.5f * p))
            scope.drawRect(Color(0xFFFAF7F2), Offset(rightCabLeft + 8.5f * p, ovenTop + 6.2f * p), Size(2.5f * p, 3.2f * p))
            // Door swung open to the right
            scope.drawRect(cabBevel, Offset(rightCabLeft + rightCabW - 1.5f * p, ovenTop + 5.5f * p), Size(4 * p, ovenH - 6.5f * p))
        } else {
            scope.drawRect(cabBevel, Offset(rightCabLeft + 1.5f * p, ovenTop + 5.5f * p), Size(rightCabW - 3 * p, ovenH - 6.5f * p))
            scope.drawRect(BrassGold, Offset(rightCabLeft + 3f * p, ovenTop + 8 * p), Size(1.2f * p, 1.2f * p))
        }

        // 5. Stovetop
        scope.drawRect(Color(0xFF212529), Offset(left + 2 * p, top - 2.2f * p), Size(20 * p, 2.4f * p))
        scope.drawRect(Color(0xFF343A40), Offset(left + 3.5f * p, top - 2.8f * p), Size(8 * p, 0.8f * p))
        scope.drawRect(Color(0xFF343A40), Offset(left + 12.5f * p, top - 2.8f * p), Size(7 * p, 0.8f * p))

        // 6. Stew Pot with animated bubbles and steam curls (on left burner)
        val potX = left + 3.5f * p
        val potY = top - 11 * p
        scope.drawRect(Color(0xFF6C757D), Offset(potX, potY), Size(9 * p, 9 * p))
        scope.drawRect(Color(0xFF495057), Offset(potX, potY + 8 * p), Size(9 * p, 1.2f * p))
        scope.drawRect(Color(0xFFADB5BD), Offset(potX + 0.8f * p, potY + 0.8f * p), Size(1.2f * p, 7.5f * p))
        scope.drawRect(Color(0xFF343A40), Offset(potX - 1.5f * p, potY + 2.5f * p), Size(1.5f * p, 2.5f * p))
        scope.drawRect(Color(0xFF343A40), Offset(potX + 9 * p, potY + 2.5f * p), Size(1.5f * p, 2.5f * p))
        scope.drawRect(Color(0xFF868E96), Offset(potX - 0.8f * p, potY - 0.8f * p), Size(10.6f * p, 1.6f * p))

        // Broth and bubbles
        scope.drawRect(SoupBroth, Offset(potX + 1.2f * p, potY + 1.2f * p), Size(6.6f * p, 2.2f * p))
        val bOffset1 = (sin(timeSeconds * 5f) * 1f * p).toFloat()
        val bOffset2 = (cos(timeSeconds * 6f) * 1f * p).toFloat()
        scope.drawRect(Color(0xFFFFD166), Offset(potX + 2.5f * p + bOffset1, potY + 1.5f * p), Size(1.5f * p, 1f * p))
        scope.drawRect(Color(0xFFFFD166), Offset(potX + 5.5f * p + bOffset2, potY + 1.5f * p), Size(1.8f * p, 1f * p))
        scope.drawRect(Color(0xFF2D6A4F), Offset(potX + 4f * p, potY + 2f * p), Size(1f * p, 1f * p))

        // Animated steam curls rising from the stew pot
        for (s in 0..2) {
            val sPhase = timeSeconds * 2.2f + s * 1.8f
            val sLift = (sPhase % 3.0f) / 3.0f
            val steamY = potY - 2 * p - sLift * 16 * p
            val steamX = potX + 3 * p + s * 2f * p + sin(sPhase * 2.5f) * 2f * p
            val steamAlpha = (1f - sLift).coerceIn(0f, 0.75f)
            val steamColor = Color.White.copy(alpha = steamAlpha * 0.7f)
            scope.drawRect(steamColor, Offset(steamX, steamY), Size(2f * p, 2f * p))
            scope.drawRect(steamColor.copy(alpha = steamAlpha * 0.5f), Offset(steamX + 0.8f * p, steamY - 1.5f * p), Size(1.5f * p, 1.5f * p))
        }

        // 7. Whistling Tea Kettle (on right burner)
        val kettleX = left + 13 * p
        val kettleY = top - 8 * p
        scope.drawRect(Color(0xFFC1121F), Offset(kettleX, kettleY), Size(6 * p, 6 * p))
        scope.drawRect(Color(0xFF780000), Offset(kettleX, kettleY + 4.8f * p), Size(6 * p, 1.2f * p))
        scope.drawRect(Color(0xFFE63946), Offset(kettleX + 0.8f * p, kettleY + 0.8f * p), Size(1.2f * p, 4.5f * p))
        scope.drawRect(Color(0xFFC0C0C0), Offset(kettleX - 2 * p, kettleY + 0.8f * p), Size(2 * p, 1.5f * p))
        scope.drawRect(Color(0xFF333333), Offset(kettleX - 2.5f * p, kettleY), Size(1f * p, 1.5f * p))
        scope.drawRect(Color(0xFF222222), Offset(kettleX + 0.8f * p, kettleY - 2.5f * p), Size(4.5f * p, 1f * p))

        // 8. Olive Oil & Vinegar bottles
        val oilX = left + 24 * p
        scope.drawRect(Color(0xDD2D6A4F), Offset(oilX, top - 8 * p), Size(2.2f * p, 8 * p))
        scope.drawRect(Color(0xFFD4A373), Offset(oilX + 0.4f * p, top - 9.2f * p), Size(1.4f * p, 1.2f * p))
        val vinX = left + 27 * p
        scope.drawRect(Color(0xDD800E13), Offset(vinX, top - 6.5f * p), Size(2f * p, 6.5f * p))
        scope.drawRect(Color(0xFFD4A373), Offset(vinX + 0.4f * p, top - 7.5f * p), Size(1.2f * p, 1f * p))

        // 9. Cutting board with vegetables & chef's knife
        val boardX = left + 31 * p
        val boardW = 13 * p
        scope.drawRect(Color(0xFFD4A373), Offset(boardX, top - 2.5f * p), Size(boardW, 2.5f * p))
        scope.drawRect(Color(0xFFB08968), Offset(boardX, top - 0.5f * p), Size(boardW, 0.8f * p))
        scope.drawRect(Color(0xFFF77F00), Offset(boardX + 2 * p, top - 4.2f * p), Size(2.5f * p, 1.8f * p))
        scope.drawRect(Color(0xFFF77F00), Offset(boardX + 5 * p, top - 4.2f * p), Size(2.5f * p, 1.8f * p))
        scope.drawRect(Color(0xFF52B788), Offset(boardX + 8.5f * p, top - 3.8f * p), Size(1.8f * p, 1.5f * p))
        scope.drawRect(Color(0xFFE0E0E0), Offset(boardX + 1f * p, top - 5.5f * p), Size(6.5f * p, 1.2f * p))
        scope.drawRect(Color(0xFF333333), Offset(boardX + 7.5f * p, top - 5.5f * p), Size(3f * p, 1.2f * p))
    }

    fun drawWindow(
        scope: DrawScope,
        cx: Float,
        topY: Float,
        isNight: Boolean,
        p: Float = 3.5f,
        weather: com.example.scene.WeatherType = com.example.scene.WeatherType.SUNNY,
        isSunset: Boolean = false
    ) {
        val w = 34 * p
        val h = 42 * p
        val left = cx - w / 2f

        // Frame
        scope.drawRect(Color(0xFF6B4226), Offset(left, topY), Size(w, h))
        val isSnowOutside = weather == com.example.scene.WeatherType.SNOW
        val isSakuraOutside = weather == com.example.scene.WeatherType.SAKURA
        val isRainOutside = weather == com.example.scene.WeatherType.RAIN
        val isAutumnOutside = weather == com.example.scene.WeatherType.AUTUMN

        val skyColor = when {
            isSunset -> Color(0xFFE76F51) // Warm sunset orange
            isNight -> Color(0xFF1B263B)
            isSnowOutside -> Color(0xFFD6E8F5)
            isRainOutside -> Color(0xFF8AADBC)
            isAutumnOutside -> Color(0xFFE8C68A)
            else -> Color(0xFFA2D2FF)
        }
        scope.drawRect(skyColor, Offset(left + 3 * p, topY + 3 * p), Size(w - 6 * p, h - 6 * p))

        if (isSunset) {
            // Golden sunset glow & low setting sun
            scope.drawRect(Color(0xFFFFD166), Offset(left + 7 * p, topY + h - 16 * p), Size(7 * p, 7 * p))
            scope.drawRect(Color(0xFFF4A261).copy(alpha = 0.6f), Offset(left + 3 * p, topY + 11 * p), Size(w - 6 * p, 4 * p))
        } else if (isNight) {
            // Moon in window
            scope.drawRect(Color(0xFFFFE66D), Offset(left + 9 * p, topY + 8 * p), Size(7 * p, 7 * p))
            scope.drawRect(Color.White, Offset(left + 22 * p, topY + 12 * p), Size(2 * p, 2 * p))
        } else if (isSnowOutside) {
            // Snow accumulation inside glass sill & falling flakes
            scope.drawRect(Color.White, Offset(left + 3 * p, topY + h - 8 * p), Size(w - 6 * p, 5 * p))
            for (i in 0 until 6) {
                val sx = left + 5 * p + (i * 4f) * p
                val sy = topY + 5 * p + (i % 3) * 6 * p
                scope.drawRect(Color.White, Offset(sx, sy), Size(1.4f * p, 1.4f * p))
            }
        } else if (isSakuraOutside) {
            // Cherry blossom petals outside
            scope.drawRect(Color.White, Offset(left + 8 * p, topY + 12 * p), Size(15 * p, 5 * p))
            for (i in 0 until 6) {
                val px = left + 4 * p + (i * 4.5f) * p
                val py = topY + 5 * p + (i % 3) * 5 * p
                scope.drawRect(Color(0xFFFFCAD4), Offset(px, py), Size(2f * p, 1.6f * p))
            }
        } else if (isRainOutside) {
            // Rain streaks on glass
            for (i in 0 until 7) {
                val rx = left + 5 * p + (i * 3.8f) * p
                val ry = topY + 4 * p + (i % 3) * 7 * p
                scope.drawRect(Color(0xAAB0C4CC), Offset(rx, ry), Size(0.9f * p, 5 * p))
            }
        } else if (isAutumnOutside) {
            // Drifting autumn leaves
            scope.drawRect(Color.White, Offset(left + 8 * p, topY + 12 * p), Size(15 * p, 5 * p))
            for (i in 0 until 6) {
                val ax = left + 4 * p + (i * 4.5f) * p
                val ay = topY + 6 * p + (i % 3) * 6 * p
                val ac = if (i % 2 == 0) Color(0xFFE76F51) else Color(0xFFF4A261)
                scope.drawRect(ac, Offset(ax, ay), Size(2.2f * p, 1.8f * p))
            }
        } else {
            // Fluffy Cloud in window
            scope.drawRect(Color.White, Offset(left + 8 * p, topY + 12 * p), Size(15 * p, 6 * p))
            scope.drawRect(Color.White, Offset(left + 11 * p, topY + 9 * p), Size(9 * p, 3 * p))
        }

        // Crossbars
        scope.drawRect(Color(0xFF6B4226), Offset(left + w / 2f - p, topY + 3 * p), Size(2 * p, h - 6 * p))
        scope.drawRect(Color(0xFF6B4226), Offset(left + 3 * p, topY + h / 2f - p), Size(w - 6 * p, 2 * p))

        // Curtains
        val curtainColor = Color(0xFFFFCAD4)
        scope.drawRect(curtainColor, Offset(left + 3 * p, topY + 3 * p), Size(6 * p, h - 6 * p))
        scope.drawRect(curtainColor, Offset(left + w - 9 * p, topY + 3 * p), Size(6 * p, h - 6 * p))
    }

    fun drawPhotoFrame(
        scope: DrawScope,
        cx: Float,
        cy: Float,
        p: Float = 3.5f
    ) {
        val w = 20 * p
        val h = 20 * p
        val left = cx - w / 2f
        val top = cy - h / 2f

        // Wooden frame
        scope.drawRect(Color(0xFF7F5539), Offset(left, top), Size(w, h))
        scope.drawRect(Color.White, Offset(left + 2 * p, top + 2 * p), Size(w - 4 * p, h - 4 * p))

        // Portrait of couple cuddling inside
        val picBg = Color(0xFFFFCAD4)
        scope.drawRect(picBg, Offset(left + 3 * p, top + 3 * p), Size(w - 6 * p, h - 6 * p))
        // Two heads cuddling
        scope.drawRect(Color(0xFF2C2A29), Offset(left + 5 * p, top + 6 * p), Size(4 * p, 4 * p))
        scope.drawRect(Color(0xFF6B4226), Offset(left + 10 * p, top + 7 * p), Size(4 * p, 4 * p))
        // Tiny heart above
        scope.drawRect(Color(0xFFFF3366), Offset(left + 9 * p, top + 4 * p), Size(2 * p, 2 * p))
    }

    fun drawMailbox(
        scope: DrawScope,
        cx: Float,
        groundY: Float,
        hasLetter: Boolean,
        p: Float = 3.5f
    ) {
        val postW = 4 * p
        val postH = 24 * p
        val postX = cx - postW / 2f
        val postY = groundY - postH

        // Post
        scope.drawRect(Color(0xFF6B4226), Offset(postX, postY), Size(postW, postH))

        // Mailbox Box
        val boxW = 18 * p
        val boxH = 13 * p
        val boxX = cx - boxW / 2f
        val boxY = postY - boxH

        scope.drawRect(Color(0xFF3A86FF), Offset(boxX, boxY), Size(boxW, boxH))
        scope.drawRect(Color(0xFF2667FF), Offset(boxX + 2 * p, boxY + 2 * p), Size(boxW - 4 * p, boxH - 4 * p))

        // Flag
        val flagColor = Color(0xFFFF0054)
        if (hasLetter) {
            // Flag Up
            scope.drawRect(flagColor, Offset(boxX + boxW, boxY - 5 * p), Size(2 * p, 9 * p))
            scope.drawRect(flagColor, Offset(boxX + boxW + 2 * p, boxY - 5 * p), Size(5 * p, 3 * p))
            // Envelope sticking out
            scope.drawRect(Color.White, Offset(boxX + 4 * p, boxY + 3 * p), Size(10 * p, 7 * p))
            scope.drawRect(Color(0xFFFF3366), Offset(boxX + 8 * p, boxY + 5 * p), Size(2 * p, 2 * p)) // red heart seal
        } else {
            // Flag Down
            scope.drawRect(flagColor, Offset(boxX + boxW, boxY + 4 * p), Size(6 * p, 2 * p))
        }
    }

    fun drawStreetlamp(
        scope: DrawScope,
        cx: Float,
        groundY: Float,
        isLit: Boolean,
        p: Float = 3.5f
    ) {
        val postW = 4 * p
        val postH = 64 * p
        val postX = cx - postW / 2f
        val postY = groundY - postH

        // Metal Pole
        scope.drawRect(Color(0xFF2B2D42), Offset(postX, postY), Size(postW, postH))
        scope.drawRect(Color(0xFF2B2D42), Offset(postX - 5 * p, groundY - 4 * p), Size(14 * p, 4 * p))

        // Lamp Head
        val headW = 16 * p
        val headH = 14 * p
        val headX = cx - headW / 2f
        val headY = postY - headH

        scope.drawRect(Color(0xFF2B2D42), Offset(headX, headY), Size(headW, headH))
        val glowColor = if (isLit) Color(0xFFFFD166) else Color(0xFF6C757D)
        scope.drawRect(glowColor, Offset(headX + 2 * p, headY + 2 * p), Size(headW - 4 * p, headH - 4 * p))

        // Soft ambient warm light cone & ground pool
        if (isLit) {
            val headCenter = Offset(cx, headY + headH / 2f)
            // 1. Stepped radiant concentric halos around lantern glass
            scope.drawCircle(Color(0xFFFFD166).copy(alpha = 0.08f), radius = 28 * p, center = headCenter)
            scope.drawCircle(Color(0xFFFFEAA7).copy(alpha = 0.16f), radius = 16 * p, center = headCenter)
            scope.drawCircle(Color(0xFFFFF3B0).copy(alpha = 0.28f), radius = 9 * p, center = headCenter)

            // 2. Tiered light cone expanding downward toward the street
            val coneTiers = 5
            for (t in 0 until coneTiers) {
                val frac = (t + 1).toFloat() / coneTiers
                val tierH = (groundY - (headY + headH)) / coneTiers
                val tierY = headY + headH + t * tierH
                val tierW = headW + frac * 48 * p
                val alpha = (0.16f * (1f - frac * 0.45f)).coerceIn(0.04f, 0.20f)
                scope.drawRect(
                    color = Color(0xFFFFE66D).copy(alpha = alpha),
                    topLeft = Offset(cx - tierW / 2f, tierY),
                    size = Size(tierW, tierH + 1.5f)
                )
            }

            // 3. Warm ambient light pool on the cobblestone ground
            scope.drawOval(
                color = Color(0xFFFFD166).copy(alpha = 0.20f),
                topLeft = Offset(cx - 30 * p, groundY - 6 * p),
                size = Size(60 * p, 12 * p)
            )
            scope.drawOval(
                color = Color(0xFFFFF3B0).copy(alpha = 0.30f),
                topLeft = Offset(cx - 16 * p, groundY - 4 * p),
                size = Size(32 * p, 8 * p)
            )
        }
    }

    fun drawMomoStall(
        scope: DrawScope,
        cx: Float,
        groundY: Float,
        p: Float = 3.5f,
        timeSeconds: Float = 0f
    ) {
        // Stall Dimensions
        val stallW = 96 * p
        val stallLeft = cx - stallW / 2f

        // 1. Wooden Counter Base (From groundY - 26 * p down to groundY)
        val counterH = 26 * p
        val counterY = groundY - counterH
        scope.drawRect(Color(0xFF6B4226), Offset(stallLeft, counterY), Size(stallW, counterH))
        scope.drawRect(Color(0xFF4A2810), Offset(stallLeft, counterY), Size(stallW, 3 * p)) // counter trim top
        // Vertical wood slats
        for (i in 1..7) {
            val slatX = stallLeft + i * (stallW / 8f)
            scope.drawRect(Color(0xFF3D210F), Offset(slatX, counterY + 3 * p), Size(2 * p, counterH - 3 * p))
        }
        // Counter top slab
        val slabW = stallW + 8 * p
        val slabX = stallLeft - 4 * p
        val slabY = counterY - 3 * p
        scope.drawRect(Color(0xFFDDA15E), Offset(slabX, slabY), Size(slabW, 4 * p))
        scope.drawRect(Color(0xFFBC6C25), Offset(slabX, slabY + 3 * p), Size(slabW, 1 * p))

        // 2. Wooden Pillars supporting the roof
        val pillarW = 4 * p
        val pillarH = 55 * p
        val roofY = counterY - pillarH
        // Left pillar
        scope.drawRect(Color(0xFF58311B), Offset(stallLeft + 2 * p, roofY), Size(pillarW, pillarH))
        scope.drawRect(Color(0xFF3D210F), Offset(stallLeft + 2 * p, roofY), Size(1.5f * p, pillarH))
        // Right pillar
        scope.drawRect(Color(0xFF58311B), Offset(stallLeft + stallW - 6 * p, roofY), Size(pillarW, pillarH))
        scope.drawRect(Color(0xFF3D210F), Offset(stallLeft + stallW - 6 * p, roofY), Size(1.5f * p, pillarH))

        // 3. Striped Awning / Canopy
        val awningW = stallW + 16 * p
        val awningLeft = cx - awningW / 2f
        val awningTop = roofY - 14 * p
        val awningH = 22 * p

        // Alternating Crimson Red and Warm Gold stripes
        val stripeCount = 10
        val stripeW = awningW / stripeCount
        for (i in 0 until stripeCount) {
            val stripeX = awningLeft + i * stripeW
            val isRed = i % 2 == 0
            val stripeColor = if (isRed) Color(0xFFC1121F) else Color(0xFFFFEEAA)
            val shadowColor = if (isRed) Color(0xFF780000) else Color(0xFFE9D8A6)
            scope.drawRect(stripeColor, Offset(stripeX, awningTop), Size(stripeW, awningH))
            scope.drawRect(shadowColor, Offset(stripeX, awningTop + awningH - 3 * p), Size(stripeW, 3 * p))
        }
        // Scalloped fringe at bottom of awning
        for (i in 0 until stripeCount) {
            val fringeX = awningLeft + i * stripeW
            val isRed = i % 2 == 0
            scope.drawRect(if (isRed) Color(0xFFC1121F) else Color(0xFFFFEEAA), Offset(fringeX + 1 * p, awningTop + awningH), Size(stripeW - 2 * p, 3 * p))
            scope.drawRect(Color(0xFFFFD166), Offset(fringeX + 2 * p, awningTop + awningH + 2 * p), Size(stripeW - 4 * p, 1.5f * p))
        }

        // 4. Fairy Lights Hanging Below Awning
        val lightY = awningTop + awningH + 5 * p
        for (i in 0 until 5) {
            val lx = awningLeft + 8 * p + i * (awningW - 16 * p) / 4f
            val bulbColor = MOMO_LIGHT_COLORS[i % MOMO_LIGHT_COLORS.size]
            // Wire drop
            scope.drawRect(Color(0xFF2B2D42), Offset(lx, awningTop + awningH), Size(1 * p, 4 * p))
            // Bulb
            scope.drawRect(bulbColor, Offset(lx - 1.5f * p, lightY), Size(4 * p, 4 * p))
            // Ambient glow
            scope.drawRect(bulbColor.copy(alpha = 0.25f), Offset(lx - 4 * p, lightY - 2 * p), Size(9 * p, 9 * p))
        }

        // 5. Signboard on top of the roof
        val signW = 78 * p
        val signH = 18 * p
        val signX = cx - signW / 2f
        val signY = awningTop - signH - 3 * p

        // Signboard back frame & neon border
        scope.drawRect(Color(0xFF1D2D44), Offset(signX, signY), Size(signW, signH))
        scope.drawRect(Color(0xFFFFD166), Offset(signX, signY), Size(signW, 2 * p)) // top neon
        scope.drawRect(Color(0xFFFFD166), Offset(signX, signY + signH - 2 * p), Size(signW, 2 * p)) // bottom neon
        scope.drawRect(Color(0xFFFFD166), Offset(signX, signY), Size(2 * p, signH)) // left
        scope.drawRect(Color(0xFFFFD166), Offset(signX + signW - 2 * p, signY), Size(2 * p, signH)) // right
        // Inner sign card
        scope.drawRect(Color(0xFF0D1B2A), Offset(signX + 2 * p, signY + 2 * p), Size(signW - 4 * p, signH - 4 * p))

        // Cute Pixel Momo Icon on left side of the sign
        val momoIconX = signX + 4 * p
        val momoIconY = signY + 4 * p
        // Dumpling shape
        scope.drawRect(Color(0xFFFFFDF0), Offset(momoIconX, momoIconY + 3 * p), Size(8 * p, 6 * p))
        scope.drawRect(Color(0xFFFAF0CA), Offset(momoIconX + 2 * p, momoIconY + 1 * p), Size(4 * p, 3 * p))
        // Pleated creases
        scope.drawRect(Color(0xFFE9D8A6), Offset(momoIconX + 2 * p, momoIconY + 4 * p), Size(1 * p, 3 * p))
        scope.drawRect(Color(0xFFE9D8A6), Offset(momoIconX + 5 * p, momoIconY + 4 * p), Size(1 * p, 3 * p))
        // Rising mini steam lines above icon
        val steamSway = sin(timeSeconds * 4f) * 1.5f * p
        scope.drawRect(Color(0xCCFFFFFF), Offset(momoIconX + 3 * p + steamSway, momoIconY - 2 * p), Size(1.5f * p, 2.5f * p))
        scope.drawRect(Color(0xAAFFFFFF), Offset(momoIconX + 5 * p - steamSway, momoIconY - 3 * p), Size(1.5f * p, 2.5f * p))

        // Draw stall signboard text using native canvas
        val signboardText = com.example.data.ProfileManager.getProfile().stallSignboardText
        scope.drawContext.canvas.nativeCanvas.apply {
            val textPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#FFF3B0")
                textSize = 9.5f * p
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                isAntiAlias = false // crisp retro pixel look
                setShadowLayer(4f * p, 0f, 0f, android.graphics.Color.parseColor("#E63946"))
            }
            val textX = signX + 15 * p
            val textY = signY + 12.5f * p
            drawText(signboardText, textX, textY, textPaint)
        }

        // 6. Momo Steamer Pots (Aluminum Steamer on Counter)
        val steamerW = 18 * p
        val steamerH = 22 * p
        val steamerX = cx - 22 * p
        val steamerY = counterY - steamerH + 2 * p

        // 3 Tiers of steamer drums
        val tierH = 5.5f * p
        for (tier in 0..2) {
            val ty = steamerY + 4 * p + tier * tierH
            // Metal body
            scope.drawRect(Color(0xFFCED4DA), Offset(steamerX, ty), Size(steamerW, tierH))
            // Metal highlight & shadow
            scope.drawRect(Color(0xFFF8F9FA), Offset(steamerX + 2 * p, ty + 1 * p), Size(steamerW - 4 * p, 1.5f * p))
            scope.drawRect(Color(0xFF6C757D), Offset(steamerX, ty + tierH - 1.5f * p), Size(steamerW, 1.5f * p))
            // Tier seam line
            scope.drawRect(Color(0xFF495057), Offset(steamerX - 1 * p, ty), Size(steamerW + 2 * p, 1 * p))
            // Side handle on tier 1
            if (tier == 1) {
                scope.drawRect(Color(0xFF343A40), Offset(steamerX - 2.5f * p, ty + 1.5f * p), Size(2.5f * p, 2.5f * p))
                scope.drawRect(Color(0xFF343A40), Offset(steamerX + steamerW, ty + 1.5f * p), Size(2.5f * p, 2.5f * p))
            }
        }
        // Steamer Domed Lid on top
        scope.drawRect(Color(0xFFADB5BD), Offset(steamerX + 2 * p, steamerY + 1 * p), Size(steamerW - 4 * p, 3 * p))
        scope.drawRect(Color(0xFFDEE2E6), Offset(steamerX + 4 * p, steamerY + 1.5f * p), Size(steamerW - 8 * p, 1.5f * p))
        // Lid knob
        scope.drawRect(Color(0xFF212529), Offset(steamerX + steamerW / 2f - 1.5f * p, steamerY - 2 * p), Size(3 * p, 3 * p))

        // 7. Counter Plate with Steamed Momos
        val plateX = cx + 4 * p
        val plateY = counterY - 5 * p
        val plateW = 20 * p
        val plateH = 5 * p
        // Plate (White ceramic oval)
        scope.drawRect(Color(0xFFE9ECEF), Offset(plateX, plateY), Size(plateW, plateH))
        scope.drawRect(Color(0xFFCED4DA), Offset(plateX + 1 * p, plateY + plateH - 1.5f * p), Size(plateW - 2 * p, 1.5f * p))
        // 4 Plump Steamed Momos on the plate
        fun drawMomoOnPlate(mx: Float, my: Float) {
            scope.drawRect(Color(0xFFFFFDF0), Offset(mx, my), Size(4 * p, 3 * p))
            scope.drawRect(Color(0xFFFAF0CA), Offset(mx + 1.5f * p, my - 1 * p), Size(p, p))
            scope.drawRect(Color(0xFFE9D8A6), Offset(mx + 2 * p, my + 1 * p), Size(p, p))
        }
        drawMomoOnPlate(plateX + 2 * p, plateY - 2 * p)
        drawMomoOnPlate(plateX + 6 * p, plateY - 2.5f * p)
        drawMomoOnPlate(plateX + 10 * p, plateY - 2 * p)
        drawMomoOnPlate(plateX + 14 * p, plateY - 2.5f * p)

        // 8. Fiery Red Spicy Chutney Bowl with Dipping Spoon!
        val bowlX = cx + 27 * p
        val bowlY = counterY - 6 * p
        val bowlW = 9 * p
        val bowlH = 6 * p
        // Ceramic bowl
        scope.drawRect(Color(0xFF2B2D42), Offset(bowlX, bowlY), Size(bowlW, bowlH))
        scope.drawRect(Color(0xFFFFFFFF), Offset(bowlX + 1 * p, bowlY + 1 * p), Size(bowlW - 2 * p, bowlH - 2 * p))
        // Spicy Red Schezwan / Tomato Chili Chutney
        scope.drawRect(Color(0xFFD90429), Offset(bowlX + 1.5f * p, bowlY + 1.5f * p), Size(bowlW - 3 * p, bowlH - 3 * p))
        // Chili flakes / seeds (yellow-orange speckles)
        scope.drawRect(Color(0xFFFFD166), Offset(bowlX + 3 * p, bowlY + 2.5f * p), Size(1 * p, 1 * p))
        scope.drawRect(Color(0xFFFFD166), Offset(bowlX + 5 * p, bowlY + 3.5f * p), Size(1 * p, 1 * p))
        // Dipping spoon sticking out
        scope.drawRect(Color(0xFFCED4DA), Offset(bowlX + 6 * p, bowlY - 3 * p), Size(1.5f * p, 5 * p))

        // 9. Side Menu Blackboard Sign
        val boardX = stallLeft - 18 * p
        val boardY = groundY - 32 * p
        val boardW = 15 * p
        val boardH = 26 * p
        // Wooden stand
        scope.drawRect(Color(0xFF58311B), Offset(boardX, boardY), Size(boardW, boardH))
        scope.drawRect(Color(0xFF1E1E24), Offset(boardX + 1.5f * p, boardY + 1.5f * p), Size(boardW - 3 * p, boardH - 3 * p))
        // Mini chalk art
        scope.drawRect(Color(0xFFFFEEAA), Offset(boardX + 3 * p, boardY + 4 * p), Size(boardW - 6 * p, 1.5f * p))
        scope.drawRect(Color(0xFFFFFDF0), Offset(boardX + 5 * p, boardY + 8 * p), Size(5 * p, 3 * p)) // chalk momo
        scope.drawRect(Color(0xFFFF5D8F), Offset(boardX + 6 * p, boardY + 13 * p), Size(3 * p, 3 * p)) // chalk heart
        scope.drawRect(Color(0xFFD90429), Offset(boardX + 4 * p, boardY + 18 * p), Size(boardW - 8 * p, 1.5f * p)) // chili price

        // 10. Two Wooden Street Stools for couple
        fun drawStool(sx: Float) {
            val stoolW = 12 * p
            val stoolH = 14 * p
            val sy = groundY - stoolH
            // Seat
            scope.drawRect(Color(0xFFDDA15E), Offset(sx, sy), Size(stoolW, 3 * p))
            scope.drawRect(Color(0xFFBC6C25), Offset(sx, sy + 2 * p), Size(stoolW, 1 * p))
            // Legs
            scope.drawRect(Color(0xFF6B4226), Offset(sx + 1 * p, sy + 3 * p), Size(2 * p, stoolH - 3 * p))
            scope.drawRect(Color(0xFF6B4226), Offset(sx + stoolW - 3 * p, sy + 3 * p), Size(2 * p, stoolH - 3 * p))
        }
        drawStool(cx - 38 * p)
        drawStool(cx + 28 * p)
    }

    fun drawEveningRoadEnvironment(
        scope: DrawScope,
        cw: Float,
        ch: Float,
        p: Float = 3.5f,
        timeSeconds: Float = 0f,
        isNight: Boolean = false,
        isSunset: Boolean = true,
        isMorning: Boolean = false,
        weather: com.example.scene.WeatherType = com.example.scene.WeatherType.SUNNY
    ) {
        val groundY = ch * 0.70f

        // 1. Sky gradient bands per season and time-of-day
        val skyH = groundY
        val skyBands = when {
            isNight -> when (weather) {
                com.example.scene.WeatherType.SNOW -> RIDE_SKY_NIGHT_SNOW
                com.example.scene.WeatherType.SAKURA -> RIDE_SKY_NIGHT_SAKURA
                com.example.scene.WeatherType.AUTUMN -> RIDE_SKY_NIGHT_AUTUMN
                com.example.scene.WeatherType.RAIN -> RIDE_SKY_NIGHT_RAIN
                else -> RIDE_SKY_NIGHT_SUMMER
            }
            isSunset -> when (weather) {
                com.example.scene.WeatherType.SAKURA -> RIDE_SKY_SUNSET_SAKURA
                com.example.scene.WeatherType.AUTUMN -> RIDE_SKY_SUNSET_AUTUMN
                com.example.scene.WeatherType.SNOW -> RIDE_SKY_SUNSET_SNOW
                com.example.scene.WeatherType.RAIN -> RIDE_SKY_SUNSET_RAIN
                else -> RIDE_SKY_SUNSET_SUMMER
            }
            isMorning -> when (weather) {
                com.example.scene.WeatherType.SAKURA -> RIDE_SKY_MORNING_SAKURA
                com.example.scene.WeatherType.AUTUMN -> RIDE_SKY_MORNING_AUTUMN
                com.example.scene.WeatherType.SNOW -> RIDE_SKY_MORNING_SNOW
                com.example.scene.WeatherType.RAIN -> RIDE_SKY_MORNING_RAIN
                else -> RIDE_SKY_MORNING_SUMMER
            }
            else -> when (weather) { // Daytime
                com.example.scene.WeatherType.SAKURA -> RIDE_SKY_DAY_SAKURA
                com.example.scene.WeatherType.AUTUMN -> RIDE_SKY_DAY_AUTUMN
                com.example.scene.WeatherType.SNOW -> RIDE_SKY_DAY_SNOW
                com.example.scene.WeatherType.RAIN -> RIDE_SKY_DAY_RAIN
                else -> RIDE_SKY_DAY_SUMMER
            }
        }

        for (i in skyBands.indices) {
            val yStart = if (i == 0) 0f else skyH * SPLITS_RIDE_SKY[i - 1]
            val yEnd = if (i == skyBands.size - 1) skyH else skyH * SPLITS_RIDE_SKY[i]
            scope.drawRect(skyBands[i], Offset(0f, yStart), Size(cw, yEnd - yStart))
        }

        // Evening / night stars gently twinkling
        if (isNight || isSunset) {
            val starAlpha1 = (sin(timeSeconds * 2f) * 0.35f + 0.65f).coerceIn(0f, 1f)
            val starAlpha2 = (cos(timeSeconds * 2.5f) * 0.35f + 0.65f).coerceIn(0f, 1f)
            scope.drawRect(Color(0xFFFFF3B0).copy(alpha = starAlpha1), Offset(cw * 0.12f, skyH * 0.12f), Size(2 * p, 2 * p))
            scope.drawRect(Color(0xFFFFF3B0).copy(alpha = starAlpha2), Offset(cw * 0.48f, skyH * 0.08f), Size(2.5f * p, 2.5f * p))
            scope.drawRect(Color(0xFFFFF3B0).copy(alpha = starAlpha1), Offset(cw * 0.82f, skyH * 0.14f), Size(2 * p, 2 * p))
        }

        // 2. PARALLAX LAYER 1: Distant Kalinga Temples (Lingaraj and Rajarani architecture)
        // Drifting slowly past in the background as the scooter cruises!
        val templeBaseY = groundY
        val templeColorFar = if (isNight || isSunset) Color(0xFF2E1A47) else Color(0xFF6B5875)
        val templeColorMid = if (isNight || isSunset) Color(0xFF1E0F30) else Color(0xFF533F5E)

        val farParallaxSpeed = 22f * p
        val templeLoopW = cw + 360f * p

        fun posMod(v: Float, m: Float): Float = ((v % m) + m) % m

        fun drawSingleTemple(tx: Float, isMain: Boolean) {
            if (tx < -180f * p || tx > cw + 180f * p) return
            if (isMain) {
                // Grand Lingaraj-style Temple (Deula tower + Jagamohana hall)
                val th = 135 * p
                scope.drawRect(templeColorMid, Offset(tx - 28 * p, templeBaseY - 55 * p), Size(56 * p, 55 * p))
                scope.drawRect(templeColorMid, Offset(tx - 23 * p, templeBaseY - 80 * p), Size(46 * p, 25 * p))
                scope.drawRect(templeColorMid, Offset(tx - 18 * p, templeBaseY - 102 * p), Size(36 * p, 22 * p))
                scope.drawRect(templeColorMid, Offset(tx - 12 * p, templeBaseY - 118 * p), Size(24 * p, 16 * p))
                scope.drawRect(templeColorMid, Offset(tx - 7 * p, templeBaseY - 128 * p), Size(14 * p, 10 * p))
                scope.drawRect(templeColorMid, Offset(tx - 10 * p, templeBaseY - 132 * p), Size(20 * p, 4 * p)) // Amlaka
                scope.drawRect(templeColorMid, Offset(tx - 2.5f * p, templeBaseY - th), Size(5 * p, 7 * p)) // Kalasa

                // Sacred Patitapabana flag waving in the temple breeze
                val flagWave = sin(timeSeconds * 6f) * 2.5f * p
                scope.drawRect(Color(0xFFE63946), Offset(tx + 2.5f * p, templeBaseY - th + 1 * p), Size(8 * p + flagWave, 4 * p))
                scope.drawRect(Color(0xFFFFD166), Offset(tx + 2.5f * p, templeBaseY - th + 2 * p), Size(8 * p + flagWave, 2 * p))

                // Jagamohana pyramid hall
                val porchX = tx - 44 * p
                scope.drawRect(templeColorMid, Offset(porchX - 16 * p, templeBaseY - 32 * p), Size(32 * p, 32 * p))
                scope.drawRect(templeColorMid, Offset(porchX - 12 * p, templeBaseY - 44 * p), Size(24 * p, 12 * p))
                scope.drawRect(templeColorMid, Offset(porchX - 7 * p, templeBaseY - 52 * p), Size(14 * p, 8 * p))
                scope.drawRect(templeColorMid, Offset(porchX - 2 * p, templeBaseY - 56 * p), Size(4 * p, 4 * p))
            } else {
                // Secondary Kalinga Temple Spire
                val th = 110 * p
                scope.drawRect(templeColorFar, Offset(tx - 22 * p, templeBaseY - 45 * p), Size(44 * p, 45 * p))
                scope.drawRect(templeColorFar, Offset(tx - 18 * p, templeBaseY - 65 * p), Size(36 * p, 20 * p))
                scope.drawRect(templeColorFar, Offset(tx - 14 * p, templeBaseY - 82 * p), Size(28 * p, 17 * p))
                scope.drawRect(templeColorFar, Offset(tx - 9 * p, templeBaseY - 95 * p), Size(18 * p, 13 * p))
                scope.drawRect(templeColorFar, Offset(tx - 5 * p, templeBaseY - 104 * p), Size(10 * p, 9 * p))
                scope.drawRect(templeColorFar, Offset(tx - 7 * p, templeBaseY - 108 * p), Size(14 * p, 4 * p))
                scope.drawRect(templeColorFar, Offset(tx - 2 * p, templeBaseY - th), Size(4 * p, 6 * p))
            }
        }

        val t1X = posMod(cw + 80f * p - timeSeconds * farParallaxSpeed, templeLoopW) - 180f * p
        val t2X = posMod(cw + 280f * p - timeSeconds * farParallaxSpeed, templeLoopW) - 180f * p
        drawSingleTemple(t1X, isMain = false)
        drawSingleTemple(t2X, isMain = true)

        // 3. PARALLAX LAYER 2: Roadside Trees, Palms, Streetlamps & Milestone
        val midParallaxSpeed = 85f * p
        val midLoopW = cw + 360f * p
        val roadTop = groundY

        // Roadside Streetlamp
        val lampX = posMod(cw + 120f * p - timeSeconds * midParallaxSpeed, midLoopW) - 60f * p
        if (lampX in -40f * p..(cw + 40f * p)) {
            val lpostH = 52 * p
            val ly = roadTop - lpostH
            scope.drawRect(Color(0xFF2B2D42), Offset(lampX - 2 * p, ly), Size(4 * p, lpostH))
            scope.drawRect(Color(0xFF2B2D42), Offset(lampX - 6 * p, ly - 4 * p), Size(12 * p, 4 * p))
            scope.drawRect(Color(0xFFFFD166), Offset(lampX - 4 * p, ly), Size(8 * p, 4 * p))
            scope.drawRect(Color(0x2AFFE66D), Offset(lampX - 24 * p, roadTop), Size(48 * p, 32 * p))
        }

        // Roadside Gulmohar Tree
        val treeX = posMod(cw + 220f * p - timeSeconds * midParallaxSpeed, midLoopW) - 100f * p
        if (treeX in -80f * p..(cw + 80f * p)) {
            scope.drawRect(Color(0xFF4A2810), Offset(treeX - 4 * p, templeBaseY - 70 * p), Size(8 * p, 70 * p))
            scope.drawRect(Color(0xFF4A2810), Offset(treeX - 18 * p, templeBaseY - 78 * p), Size(24 * p, 6 * p))
            scope.drawRect(Color(0xFF4A2810), Offset(treeX + 2 * p, templeBaseY - 82 * p), Size(26 * p, 6 * p))
            val gulmoharRed = Color(0xFFE63946)
            val gulmoharCoral = Color(0xFFFF5D8F)
            val gulmoharGreen = Color(0xFF2D5A27)
            scope.drawRect(gulmoharGreen, Offset(treeX - 28 * p, templeBaseY - 105 * p), Size(56 * p, 32 * p))
            scope.drawRect(gulmoharRed, Offset(treeX - 25 * p, templeBaseY - 102 * p), Size(22 * p, 18 * p))
            scope.drawRect(gulmoharCoral, Offset(treeX - 6 * p, templeBaseY - 110 * p), Size(24 * p, 20 * p))
            scope.drawRect(gulmoharRed, Offset(treeX + 12 * p, templeBaseY - 98 * p), Size(20 * p, 16 * p))
        }

        // Roadside Coconut Palm
        val palmX = posMod(cw + 340f * p - timeSeconds * midParallaxSpeed, midLoopW) - 100f * p
        if (palmX in -60f * p..(cw + 60f * p)) {
            val palmTopY = templeBaseY - 95 * p
            for (i in 0 until 18) {
                val ty = templeBaseY - i * 5.2f * p
                val tx = palmX - (i * i * 0.05f) * p
                scope.drawRect(Color(0xFF2C1810), Offset(tx, ty), Size(4.5f * p, 6 * p))
            }
            val frondColor = Color(0xFF1E3A20)
            scope.drawRect(frondColor, Offset(palmX - 30 * p, palmTopY), Size(30 * p, 5 * p))
            scope.drawRect(frondColor, Offset(palmX - 25 * p, palmTopY + 8 * p), Size(26 * p, 4 * p))
            scope.drawRect(frondColor, Offset(palmX + 4 * p, palmTopY + 2 * p), Size(24 * p, 4 * p))
            scope.drawRect(frondColor, Offset(palmX - 12 * p, palmTopY - 14 * p), Size(20 * p, 16 * p))
        }

        // Roadside Milestone
        val stoneX = posMod(cw + 440f * p - timeSeconds * midParallaxSpeed, midLoopW) - 80f * p
        if (stoneX in -40f * p..(cw + 40f * p)) {
            val stoneW = 16 * p
            val stoneH = 24 * p
            val sy = groundY - stoneH
            // Dome curved top (Yellow)
            scope.drawRect(Color(0xFFFFD166), Offset(stoneX + 2 * p, sy), Size(stoneW - 4 * p, 3 * p))
            scope.drawRect(Color(0xFFFFD166), Offset(stoneX, sy + 3 * p), Size(stoneW, 7 * p))
            // Body (White)
            scope.drawRect(Color(0xFFF8F9FA), Offset(stoneX, sy + 10 * p), Size(stoneW, stoneH - 10 * p))
            scope.drawRect(Color(0xFFCED4DA), Offset(stoneX, sy + stoneH - 2 * p), Size(stoneW, 2 * p))
            // Text simulated markings
            scope.drawRect(Color(0xFF1B4332), Offset(stoneX + 3 * p, sy + 5 * p), Size(stoneW - 6 * p, 2 * p))
            scope.drawRect(Color(0xFF212529), Offset(stoneX + 3 * p, sy + 13 * p), Size(stoneW - 6 * p, 2.5f * p))
            scope.drawRect(Color(0xFF212529), Offset(stoneX + 5 * p, sy + 17 * p), Size(stoneW - 10 * p, 2 * p))
        }

        // 4. PARALLAX LAYER 3: Road Curb (Alternating Black & Yellow)
        val curbH = 6 * p
        val curbBlockW = 18 * p
        val curbCycle = curbBlockW * 2
        val curbShift = (timeSeconds * 200f * p) % curbCycle
        var cxCurb = -curbShift
        var curbIdx = 0
        while (cxCurb < cw + curbBlockW * 2) {
            val isYellow = (curbIdx % 2 == 0)
            scope.drawRect(if (isYellow) Color(0xFFFFD166) else Color(0xFF212529), Offset(cxCurb, roadTop), Size(curbBlockW, curbH))
            scope.drawRect(if (isYellow) Color(0xFFFFF3B0) else Color(0xFF495057), Offset(cxCurb, roadTop), Size(curbBlockW, 1.5f * p))
            cxCurb += curbBlockW
            curbIdx++
        }

        // 5. Asphalt Roadway
        val roadH = ch - roadTop - curbH
        val roadY = roadTop + curbH
        scope.drawRect(Color(0xFF1F2226), Offset(0f, roadY), Size(cw, roadH))
        scope.drawRect(Color(0xFF282D34), Offset(0f, roadY + 8 * p), Size(cw, 3 * p))
        scope.drawRect(Color(0xFF17191C), Offset(0f, roadY + roadH - 12 * p), Size(cw, 12 * p))

        // 6. Fast Scrolling Center Road Striping
        val stripeY = roadY + roadH * 0.50f
        val stripeW = 28 * p
        val gapW = 22 * p
        val stripeCycle = stripeW + gapW
        val stripeOffset = (timeSeconds * 200f * p) % stripeCycle
        var sx = -stripeOffset
        while (sx < cw + stripeCycle) {
            scope.drawRect(Color(0xFFFFD166), Offset(sx, stripeY), Size(stripeW, 3.5f * p))
            sx += stripeCycle
        }

        // 7. Atmospheric Wind Streaks & Flying Gulmohar Petals
        for (i in 0 until 5) {
            val windY = roadTop - (14f + i * 22f) * p
            val windCycle = cw + 120f * p
            val windX = cw - ((timeSeconds * (230f + i * 40f) * p + i * 85f * p) % windCycle)
            scope.drawRect(Color(0x35FFFFFF), Offset(windX, windY), Size(32f * p, 1.5f * p))
        }

        for (i in 0 until 8) {
            val petCycle = cw + 80f * p
            val petX = cw - ((timeSeconds * (180f + (i % 3) * 35f) * p + i * 70f * p) % petCycle)
            val petY = groundY - (10f + (i * 12f) % 65f) * p + sin(timeSeconds * 6f + i) * 8f * p
            scope.drawRect(Color(0xFFE63946), Offset(petX, petY), Size(3f * p, 2f * p))
            scope.drawRect(Color(0xFFFF758F), Offset(petX + p, petY), Size(1.5f * p, 1.5f * p))
        }
    }


    fun drawScooterWithCouple(
        scope: DrawScope,
        cx: Float,
        groundY: Float,
        p: Float = 3.5f,
        timeSeconds: Float = 0f,
        boyEmotion: CharacterEmotion = CharacterEmotion.HAPPY,
        girlEmotion: CharacterEmotion = CharacterEmotion.HAPPY,
        boyOutfitIndex: Int = 0,
        girlOutfitIndex: Int = 0,
        boyAccessoryIndex: Int = 0,
        girlAccessoryIndex: Int = 0,
        boyWearsGlasses: Boolean = true
    ) {
        val boyOutfit = PixelArtRenderer.getBoyOutfitPalette(boyOutfitIndex)
        val girlDress = PixelArtRenderer.getGirlDressPalette(girlOutfitIndex)

        val rideBounce = sin(timeSeconds * 18f) * 0.8f * p
        // Ground wheels flush onto the asphalt roadway (curb top is groundY, road surface is groundY + 6*p)
        val scootY = groundY + 5.5f * p + rideBounce

        val wheelAngle = -timeSeconds * 24f
        val spokeCos = cos(wheelAngle) * 4.5f * p
        val spokeSin = sin(wheelAngle) * 4.5f * p

        // 1. SCOOTER WHEELS (Front & Rear)
        val rearWheelX = cx - 34 * p
        val frontWheelX = cx + 30 * p
        val wheelCenterY = scootY - 8 * p
        val wheelRadius = 8.5f * p

        // Contact tire shadows on asphalt road surface
        val roadContactY = groundY + 6.0f * p
        scope.drawOval(Color(0x55000000), Offset(rearWheelX - 7 * p, roadContactY - 1.5f * p), Size(14 * p, 3 * p))
        scope.drawOval(Color(0x55000000), Offset(frontWheelX - 7 * p, roadContactY - 1.5f * p), Size(14 * p, 3 * p))

        fun drawWheel(wx: Float) {
            // Dark tire with bevel
            scope.drawRect(Color(0xFF1B1B1E), Offset(wx - wheelRadius, wheelCenterY - wheelRadius), Size(wheelRadius * 2, wheelRadius * 2))
            val rimR = wheelRadius - 2.5f * p
            scope.drawRect(Color(0xFFCED4DA), Offset(wx - rimR, wheelCenterY - rimR), Size(rimR * 2, rimR * 2))
            scope.drawRect(Color(0xFF495057), Offset(wx - 2 * p, wheelCenterY - 2 * p), Size(4 * p, 4 * p))
            // Fast spinning spokes
            scope.drawRect(Color(0xFF495057), Offset(wx + spokeCos - p, wheelCenterY + spokeSin - p), Size(2 * p, 2 * p))
            scope.drawRect(Color(0xFF495057), Offset(wx - spokeCos - p, wheelCenterY - spokeSin - p), Size(2 * p, 2 * p))
            scope.drawRect(Color(0xFF495057), Offset(wx - spokeSin - p, wheelCenterY + spokeCos - p), Size(2 * p, 2 * p))
            scope.drawRect(Color(0xFF495057), Offset(wx + spokeSin - p, wheelCenterY - spokeCos - p), Size(2 * p, 2 * p))
        }
        drawWheel(rearWheelX)
        drawWheel(frontWheelX)

        // 2. SCOOTER CHASSIS & BODYWORK — Pearl White / Silver
        val scootPearl = Color(0xFFEDF2F4)   // main pearl-white body panels
        val scootSilver = Color(0xFFADB5BD)  // shadow / depth panels
        val scootAccent = Color(0xFF48CAE4)  // sky-blue accent stripe
        val scootWhite = Color(0xFFF8F9FA)   // bright highlight

        scope.drawRect(scootSilver, Offset(rearWheelX - 4 * p, wheelCenterY - 10 * p), Size(18 * p, 10 * p))
        // Taillight (stays red — it's a brake light, not bodywork)
        scope.drawRect(Color(0xFFFF0054), Offset(rearWheelX - 6 * p, wheelCenterY - 9 * p), Size(3 * p, 5 * p))
        scope.drawRect(Color(0x55FF0054), Offset(rearWheelX - 10 * p, wheelCenterY - 9 * p), Size(4 * p, 5 * p))

        val floorY = wheelCenterY - 3 * p
        scope.drawRect(Color(0xFF343A40), Offset(cx - 16 * p, floorY), Size(32 * p, 4 * p))
        scope.drawRect(Color(0xFF212529), Offset(cx - 14 * p, floorY + 1 * p), Size(28 * p, 2 * p))

        scope.drawRect(scootPearl, Offset(cx - 28 * p, wheelCenterY - 18 * p), Size(24 * p, 16 * p))
        scope.drawRect(scootSilver, Offset(cx - 28 * p, wheelCenterY - 18 * p), Size(24 * p, 3 * p))   // top shadow band
        scope.drawRect(scootAccent, Offset(cx - 24 * p, wheelCenterY - 14 * p), Size(18 * p, 2.5f * p)) // accent stripe

        val seatY = wheelCenterY - 24 * p
        scope.drawRect(Color(0xFF2B2D42), Offset(cx - 30 * p, seatY), Size(36 * p, 7 * p))
        scope.drawRect(Color(0xFF495057), Offset(cx - 29 * p, seatY + 1 * p), Size(34 * p, 2 * p))
        scope.drawRect(scootSilver, Offset(cx - 12 * p, seatY + 6 * p), Size(2 * p, p))

        // Rear grab rail
        scope.drawRect(Color(0xFFCED4DA), Offset(cx - 32 * p, seatY - 4 * p), Size(3 * p, 8 * p))
        scope.drawRect(Color(0xFFCED4DA), Offset(cx - 32 * p, seatY - 4 * p), Size(6 * p, 2 * p))

        // Front Apron & Shield
        val frontApronX = cx + 14 * p
        val apronTopY = wheelCenterY - 28 * p
        scope.drawRect(scootPearl, Offset(frontApronX, apronTopY), Size(15 * p, 26 * p))
        scope.drawRect(scootSilver, Offset(frontApronX, apronTopY), Size(15 * p, 3 * p))               // top shadow band
        scope.drawRect(scootAccent, Offset(frontApronX + 2 * p, apronTopY + 4 * p), Size(11 * p, 2.5f * p)) // accent stripe
        scope.drawRect(scootWhite, Offset(frontApronX + 2 * p, apronTopY + 7 * p), Size(11 * p, 11 * p))

        // Front Brand Plate
        scope.drawRect(Color(0xFF1D3557), Offset(frontApronX + 3 * p, apronTopY + 10 * p), Size(9 * p, 3 * p))
        scope.drawRect(Color(0xFFFFD166), Offset(frontApronX + 4 * p, apronTopY + 11 * p), Size(7 * p, p))

        // Steering Column & Handlebars
        val handlebarY = apronTopY - 8 * p
        scope.drawRect(Color(0xFF495057), Offset(frontApronX + 6 * p, handlebarY), Size(3.5f * p, 9 * p))
        scope.drawRect(Color(0xFF212529), Offset(frontApronX + 1 * p, handlebarY), Size(14 * p, 3 * p))
        scope.drawRect(Color(0xFF000000), Offset(frontApronX, handlebarY), Size(3 * p, 3 * p))
        scope.drawRect(Color(0xFF000000), Offset(frontApronX + 12 * p, handlebarY), Size(3 * p, 3 * p))

        // Chrome Rearview Mirrors
        scope.drawRect(Color(0xFFCED4DA), Offset(frontApronX + 1 * p, handlebarY - 5 * p), Size(1.5f * p, 5 * p))
        scope.drawRect(Color(0xFFCED4DA), Offset(frontApronX + 12 * p, handlebarY - 5 * p), Size(1.5f * p, 5 * p))
        scope.drawRect(Color(0xFFE9ECEF), Offset(frontApronX - 1 * p, handlebarY - 7 * p), Size(4 * p, 3 * p))
        scope.drawRect(Color(0xFFE9ECEF), Offset(frontApronX + 11 * p, handlebarY - 7 * p), Size(4 * p, 3 * p))

        // Dual LED Headlamp & Wide Forward Beam Illuminating the Road
        scope.drawRect(Color(0xFFFFF3B0), Offset(frontApronX + 13 * p, apronTopY + 2 * p), Size(3 * p, 6 * p))
        val beamY = apronTopY + 2 * p
        scope.drawRect(Color(0x38FFF9DB), Offset(frontApronX + 16 * p, beamY), Size(50 * p, 26 * p))
        scope.drawRect(Color(0x22FFF9DB), Offset(frontApronX + 28 * p, beamY + 8 * p), Size(65 * p, 24 * p))

        // Mochi the Cat perched in the front footboard with wind-blown mini red scarf!
        val catBasketX = cx + 4 * p
        val catBasketY = floorY - 9 * p
        scope.drawRect(Color(0xFFF8F9FA), Offset(catBasketX, catBasketY + 2 * p), Size(7 * p, 7 * p))
        scope.drawRect(Color(0xFFF77F00), Offset(catBasketX + p, catBasketY + 2 * p), Size(3 * p, 3 * p))
        scope.drawRect(Color(0xFFF77F00), Offset(catBasketX + 4 * p, catBasketY), Size(2 * p, 2 * p)) // ear
        scope.drawRect(Color(0xFF2B9348), Offset(catBasketX + 5 * p, catBasketY + 3 * p), Size(1.2f * p, 1.2f * p)) // green eye
        val scarfWave = sin(timeSeconds * 12f) * 2f * p
        scope.drawRect(Color(0xFFE63946), Offset(catBasketX - 2 * p + scarfWave, catBasketY + 4 * p), Size(3 * p, 1.8f * p)) // red scarf flying back

        // 3. THE BOY SITTING IN BACK HOLDING THE GIRL TIGHT
        val boySeatX = cx - 18 * p
        val boyBottomY = seatY + 2 * p

        scope.drawRect(boyOutfit.pants, Offset(boySeatX - 6 * p, boyBottomY - 10 * p), Size(14 * p, 10 * p))
        scope.drawRect(boyOutfit.pantsFold, Offset(boySeatX - 2 * p, boyBottomY), Size(6 * p, 10 * p))
        scope.drawRect(boyOutfit.shoes, Offset(boySeatX - 1 * p, boyBottomY + 9 * p), Size(7 * p, 3 * p))

        val boyTorsoY = boyBottomY - 26 * p
        scope.drawRect(boyOutfit.sweater, Offset(boySeatX - 5 * p, boyTorsoY), Size(14 * p, 16 * p))
        scope.drawRect(boyOutfit.sweaterHighlight, Offset(boySeatX - 4 * p, boyTorsoY + 1 * p), Size(12 * p, 3 * p))

        if (boyOutfit.isHoodie) {
            // Folded hood collar fluttering behind neck
            scope.drawRect(boyOutfit.collar, Offset(boySeatX - 7 * p, boyTorsoY - 2 * p), Size(6 * p, 4 * p))
            // Drawstrings
            val cordColor = if (boyOutfitIndex == 1) boyOutfit.collar else Color.White
            scope.drawRect(cordColor, Offset(boySeatX + 3 * p, boyTorsoY + 4 * p), Size(0.9f * p, 4 * p))
        }

        // Boy Arms WRAPPED TIGHTLY AROUND GIRL'S WAIST (True romantic hug on the bike!)
        scope.drawRect(boyOutfit.sweater, Offset(boySeatX + 6 * p, boyTorsoY + 8 * p), Size(14 * p, 4.5f * p))
        scope.drawRect(PixelArtRenderer.SkinToneBoy, Offset(boySeatX + 18 * p, boyTorsoY + 8 * p), Size(4.5f * p, 4.5f * p))

        // Boy Head leaning forward against girl's back
        val boyHeadY = boyTorsoY - 14 * p
        scope.drawRect(PixelArtRenderer.SkinToneBoy, Offset(boySeatX - 2 * p, boyHeadY), Size(12 * p, 12 * p))
        val hairFlutter = sin(timeSeconds * 10f) * 1.8f * p
        scope.drawRect(Color(0xFF2A2829), Offset(boySeatX - 5 * p, boyHeadY - 3 * p), Size(15 * p, 6 * p))
        scope.drawRect(Color(0xFF2A2829), Offset(boySeatX - 8 * p + hairFlutter, boyHeadY - 1 * p), Size(5 * p, 6 * p))

        // Sweet closed smiling eyes (^), blushing cheeks
        scope.drawRect(Color(0xFF22223B), Offset(boySeatX + 4 * p, boyHeadY + 4 * p), Size(2.2f * p, p))
        scope.drawRect(Color(0xFFFFAAA6), Offset(boySeatX + 3 * p, boyHeadY + 6.5f * p), Size(3.5f * p, 2 * p))

        // Boy Accessory on bike (beanie / scarf / cap)
        when (boyAccessoryIndex) {
            1 -> {
                // Beanie
                scope.drawRect(Color(0xFF264653), Offset(boySeatX - 4 * p, boyHeadY - 4 * p), Size(14 * p, 5 * p))
                scope.drawRect(Color(0xFF1B4332), Offset(boySeatX - 3 * p, boyHeadY), Size(13 * p, 2 * p))
                scope.drawRect(Color(0xFFE9C46A), Offset(boySeatX + 2 * p, boyHeadY - 6 * p), Size(3 * p, 2.5f * p))
            }
            2 -> {
                // Scarf trailing back in wind
                val scarfBack = sin(timeSeconds * 14f) * 3f * p
                scope.drawRect(Color(0xFFE9C46A), Offset(boySeatX - 8 * p + scarfBack, boyTorsoY - 2 * p), Size(10 * p, 3.5f * p))
            }
            3 -> {
                // Baseball cap
                scope.drawRect(Color(0xFF1D3557), Offset(boySeatX - 4 * p, boyHeadY - 4 * p), Size(13 * p, 5 * p))
                scope.drawRect(Color(0xFF0F172A), Offset(boySeatX + 6 * p, boyHeadY + p), Size(6 * p, 2 * p))
            }
        }

        // Glasses always rendered on scooter in front
        if (boyWearsGlasses) {
            scope.drawRect(PixelArtRenderer.GlassesFrame, Offset(boySeatX + 3 * p, boyHeadY + 3.5f * p), Size(3.5f * p, 3.5f * p))
            scope.drawRect(PixelArtRenderer.GlassesGlint, Offset(boySeatX + 4 * p, boyHeadY + 4.5f * p), Size(1.5f * p, 1.5f * p))
            scope.drawRect(PixelArtRenderer.GlassesFrameLight, Offset(boySeatX + 2 * p, boyHeadY + 4.5f * p), Size(1.5f * p, 0.8f * p))
        }

        // 4. THE GIRL DRIVING IN FRONT (Hands on handlebars!)
        val girlSeatX = cx + 2 * p
        val girlBottomY = seatY + 2 * p

        scope.drawRect(girlDress.skirt, Offset(girlSeatX - 6 * p, girlBottomY - 10 * p), Size(14 * p, 10 * p))
        scope.drawRect(PixelArtRenderer.SkinToneGirl, Offset(girlSeatX + 2 * p, girlBottomY), Size(5 * p, 11 * p))
        scope.drawRect(Color(0xFFFFF1E6), Offset(girlSeatX + 2 * p, girlBottomY + 7 * p), Size(5 * p, 4 * p))
        scope.drawRect(PixelArtRenderer.ShoesGirl, Offset(girlSeatX + 3 * p, girlBottomY + 10 * p), Size(7 * p, 3 * p))

        val girlTorsoY = girlBottomY - 26 * p
        scope.drawRect(girlDress.sweater, Offset(girlSeatX - 4 * p, girlTorsoY), Size(13 * p, 16 * p))
        scope.drawRect(girlDress.trim, Offset(girlSeatX - 3 * p, girlTorsoY + 1 * p), Size(11 * p, 2 * p))

        if (girlDress.isHoodie || girlOutfitIndex >= 7) {
            // Folded hood collar fluttering behind girl's neck
            scope.drawRect(girlDress.skirtShadow, Offset(girlSeatX - 6 * p, girlTorsoY - 2 * p), Size(6 * p, 4 * p))
            // Drawstrings on chest
            scope.drawRect(Color.White, Offset(girlSeatX + 3 * p, girlTorsoY + 3 * p), Size(0.9f * p, 4 * p))
        }

        // Girl Arms reaching forward holding handlebars firmly
        scope.drawRect(girlDress.sweater, Offset(girlSeatX + 6 * p, girlTorsoY + 4 * p), Size(13 * p, 4 * p))
        scope.drawRect(PixelArtRenderer.SkinToneGirl, Offset(girlSeatX + 17 * p, girlTorsoY + 5 * p), Size(4 * p, 4 * p))

        val girlHeadY = girlTorsoY - 14 * p
        scope.drawRect(PixelArtRenderer.SkinToneGirl, Offset(girlSeatX + 2 * p, girlHeadY), Size(11 * p, 12 * p))

        // Long wind-blown hair flowing back horizontally with fluttering ribbon
        val girlHairWind = sin(timeSeconds * 12f) * 2.5f * p
        scope.drawRect(Color(0xFF6A381F), Offset(girlSeatX - 2 * p, girlHeadY - 3 * p), Size(15 * p, 6 * p))
        scope.drawRect(Color(0xFF6A381F), Offset(girlSeatX - 9 * p + girlHairWind, girlHeadY + 2 * p), Size(9 * p, 15 * p))
        scope.drawRect(Color(0xFF492413), Offset(girlSeatX - 13 * p + girlHairWind, girlHeadY + 6 * p), Size(6 * p, 13 * p))

        // Ribbon fluttering back in wind
        val ribbonWave = sin(timeSeconds * 14f) * 3f * p
        scope.drawRect(girlDress.ribbon, Offset(girlSeatX - 4 * p, girlHeadY - 4 * p), Size(3.5f * p, 3.5f * p))
        scope.drawRect(girlDress.ribbon, Offset(girlSeatX - 9 * p + ribbonWave, girlHeadY - 2 * p), Size(6 * p, 2 * p))

        // Girl Accessory on bike
        when (girlAccessoryIndex) {
            1 -> {
                // Beanie
                scope.drawRect(Color(0xFFE8998D), Offset(girlSeatX + p, girlHeadY - 4 * p), Size(12 * p, 5 * p))
                scope.drawRect(Color(0xFFD6587A), Offset(girlSeatX + 2 * p, girlHeadY), Size(11 * p, 2 * p))
                scope.drawRect(Color(0xFFFFF0F3), Offset(girlSeatX + 6 * p, girlHeadY - 6 * p), Size(3 * p, 2.5f * p))
            }
            2 -> {
                // Scarf fluttering back
                val scarfBack = sin(timeSeconds * 14f) * 3f * p
                scope.drawRect(Color(0xFFFFB5A7), Offset(girlSeatX - 5 * p + scarfBack, girlTorsoY - p), Size(9 * p, 3.5f * p))
            }
            3 -> {
                // Baseball cap
                scope.drawRect(Color(0xFF6B9080), Offset(girlSeatX + p, girlHeadY - 4 * p), Size(12 * p, 5 * p))
                scope.drawRect(Color(0xFF4E6E60), Offset(girlSeatX + 9 * p, girlHeadY + p), Size(6 * p, 2 * p))
            }
        }

        // Focused happy eyes looking ahead, bright smile, blushing cheeks
        scope.drawRect(Color(0xFF22223B), Offset(girlSeatX + 8 * p, girlHeadY + 4 * p), Size(2 * p, 2 * p))
        scope.drawRect(Color(0xFFFFAAA6), Offset(girlSeatX + 7 * p, girlHeadY + 7 * p), Size(3 * p, 2 * p))
        scope.drawRect(Color(0xFFC9184A), Offset(girlSeatX + 8 * p, girlHeadY + 9 * p), Size(2.5f * p, p))

        // Floating little heart while riding together
        if (sin(timeSeconds * 2.2f) > 0.60f) {
            val heartOffset = sin(timeSeconds * 4f) * 2 * p
            val hx = cx - 8 * p + heartOffset
            val hy = girlHeadY - 14 * p - (sin(timeSeconds * 3f) * 6 * p)
            scope.drawRect(Color(0xFFFF3366), Offset(hx - 2 * p, hy), Size(5 * p, 4 * p))
            scope.drawRect(Color(0xFFFF3366), Offset(hx - p, hy + 3 * p), Size(3 * p, 2 * p))
        }
    }

    fun drawPicnicBasket(scope: DrawScope, x: Float, groundY: Float, p: Float) {
        val basketW = 16 * p
        val basketH = 12 * p
        val by = groundY - basketH
        // Wicker basket body
        scope.drawRect(Color(0xFFD4A373), Offset(x, by + 3 * p), Size(basketW, basketH - 3 * p))
        scope.drawRect(Color(0xFFBC6C25), Offset(x, by + 3 * p), Size(basketW, p))
        for (i in 1..3) {
            val wx = x + i * 4 * p
            scope.drawRect(Color(0xFF9A551A), Offset(wx, by + 4 * p), Size(p, basketH - 4 * p))
        }
        scope.drawRect(Color(0xFFBC6C25), Offset(x - p, by + 1.5f * p), Size(basketW + 2 * p, 2.5f * p))
        scope.drawRect(Color(0xFF8B4513), Offset(x + basketW / 2f - 4 * p, by - 4 * p), Size(8 * p, p))
        scope.drawRect(Color(0xFF8B4513), Offset(x + basketW / 2f - 4 * p, by - 4 * p), Size(p, 5.5f * p))
        scope.drawRect(Color(0xFF8B4513), Offset(x + basketW / 2f + 3 * p, by - 4 * p), Size(p, 5.5f * p))
        // Red gingham cloth corner peeking out
        scope.drawRect(Color(0xFFE63946), Offset(x + basketW - 5 * p, by + 2 * p), Size(5 * p, 4 * p))
        scope.drawRect(Color(0xFFFFFFFF), Offset(x + basketW - 4 * p, by + 3 * p), Size(2 * p, 2 * p))
    }

    fun drawTreeSwing(scope: DrawScope, treeX: Float, groundY: Float, p: Float, timeSeconds: Float) {
        val swingSway = sin(timeSeconds * 1.6f) * 3f * p
        val ropeTopY = groundY - 82 * p
        val seatY = groundY - 18 * p
        val seatW = 14 * p
        val sx = treeX + 22 * p + swingSway

        // Two hanging jute ropes
        scope.drawRect(Color(0xFFDDA15E), Offset(sx - 5 * p, ropeTopY), Size(1.5f * p, seatY - ropeTopY))
        scope.drawRect(Color(0xFFDDA15E), Offset(sx + 5 * p, ropeTopY), Size(1.5f * p, seatY - ropeTopY))
        // Wooden seat plank
        scope.drawRect(Color(0xFF6B4226), Offset(sx - 7 * p, seatY), Size(seatW, 3 * p))
        scope.drawRect(Color(0xFF4A2810), Offset(sx - 7 * p, seatY + 2 * p), Size(seatW, p))
    }

    /**
     * Draws a soft, dappled pixel-art tree shade on the meadow grass under the canopy
     * during sunny summer daytime.
     */
    fun drawTreeShade(
        scope: DrawScope,
        baseX: Float,
        groundY: Float,
        p: Float = 3.5f,
        timeSeconds: Float = 0f
    ) {
        val sway = sin(timeSeconds * 1.5f) * 2f * p
        val shadeCx = baseX + sway * 0.4f
        val shadeCy = groundY + 4 * p

        // Tiered pixel shadow cast on meadow grass
        val outerShadeColor = Color(0x3016331C)
        val coreShadeColor = Color(0x450D2412)

        // Outer soft shade layer
        scope.drawRect(outerShadeColor, Offset(shadeCx - 46 * p, shadeCy - 4 * p), Size(92 * p, 16 * p))
        scope.drawRect(outerShadeColor, Offset(shadeCx - 38 * p, shadeCy - 8 * p), Size(76 * p, 4 * p))
        scope.drawRect(outerShadeColor, Offset(shadeCx - 38 * p, shadeCy + 12 * p), Size(76 * p, 4 * p))
        scope.drawRect(outerShadeColor, Offset(shadeCx - 52 * p, shadeCy), Size(104 * p, 8 * p))

        // Core deeper shade under dense trunk & canopy center
        scope.drawRect(coreShadeColor, Offset(shadeCx - 32 * p, shadeCy - 3 * p), Size(64 * p, 12 * p))
        scope.drawRect(coreShadeColor, Offset(shadeCx - 24 * p, shadeCy - 6 * p), Size(48 * p, 3 * p))
        scope.drawRect(coreShadeColor, Offset(shadeCx - 24 * p, shadeCy + 9 * p), Size(48 * p, 3 * p))

        // Dappled sunlight specks filtering through moving leaves
        val dappleColor = Color(0x40FFF3B0)
        val d1X = shadeCx - 24 * p + sway * 0.8f
        val d1Y = shadeCy - p
        scope.drawRect(dappleColor, Offset(d1X, d1Y), Size(3 * p, 2 * p))

        val d2X = shadeCx + 18 * p - sway * 0.6f
        val d2Y = shadeCy + 3 * p
        scope.drawRect(dappleColor, Offset(d2X, d2Y), Size(2.5f * p, 2 * p))

        val d3X = shadeCx - 8 * p + sway * 0.4f
        val d3Y = shadeCy + 6 * p
        scope.drawRect(dappleColor, Offset(d3X, d3Y), Size(2 * p, 1.5f * p))

        val d4X = shadeCx + 30 * p + sway * 0.7f
        val d4Y = shadeCy - 2 * p
        scope.drawRect(dappleColor, Offset(d4X, d4Y), Size(2 * p, 2 * p))
    }

    fun drawFairyJar(scope: DrawScope, x: Float, groundY: Float, p: Float, timeSeconds: Float) {
        val jarW = 10 * p
        val jarH = 14 * p
        val jy = groundY - jarH
        scope.drawRect(Color(0x80D8F3DC), Offset(x, jy + 2 * p), Size(jarW, jarH - 2 * p))
        scope.drawRect(Color(0xFF95D5B2), Offset(x - p, jy + 1.5f * p), Size(jarW + 2 * p, 1.5f * p))
        scope.drawRect(Color(0xFFD4A373), Offset(x + 2 * p, jy - p), Size(jarW - 4 * p, 2.5f * p))
        val f1 = (sin(timeSeconds * 3.5f) * 0.4f + 0.6f).coerceIn(0f, 1f)
        val f2 = (cos(timeSeconds * 2.8f + 1f) * 0.4f + 0.6f).coerceIn(0f, 1f)
        val f3 = (sin(timeSeconds * 4.2f + 2f) * 0.4f + 0.6f).coerceIn(0f, 1f)
        scope.drawRect(Color(0xFFFFE66D).copy(alpha = f1), Offset(x + 3 * p, jy + 4 * p), Size(2 * p, 2 * p))
        scope.drawRect(Color(0xFFFFE66D).copy(alpha = f2), Offset(x + 6 * p, jy + 7 * p), Size(2 * p, 2 * p))
        scope.drawRect(Color(0xFFFFE66D).copy(alpha = f3), Offset(x + 3.5f * p, jy + 10 * p), Size(1.5f * p, 1.5f * p))
        scope.drawRect(Color(0x25FFE66D), Offset(x - 4 * p, jy - 2 * p), Size(jarW + 8 * p, jarH + 6 * p))
    }

    fun drawFloorLamp(scope: DrawScope, x: Float, groundY: Float, p: Float, lampLit: Boolean = true) {
        val lampH = 58 * p
        val ly = groundY - lampH
        scope.drawRect(Color(0xFF495057), Offset(x - 5 * p, groundY - 2.5f * p), Size(10 * p, 2.5f * p))
        scope.drawRect(Color(0xFF6C757D), Offset(x - p, ly + 14 * p), Size(2 * p, lampH - 16 * p))
        // Shade: warm amber when lit, cool cream when off
        val shadeCol = if (lampLit) Color(0xFFFFEAA7) else Color(0xFFFEFAE0)
        scope.drawRect(shadeCol, Offset(x - 7 * p, ly + 2 * p), Size(14 * p, 12 * p))
        scope.drawRect(Color(0xFFE9EDC9), Offset(x - 5 * p, ly), Size(10 * p, 2 * p))
        scope.drawRect(Color(0xFFD4A373), Offset(x - 7 * p, ly + 13 * p), Size(14 * p, p))
        // Glow cone below the shade — bright when lit, invisible when off
        if (lampLit) {
            val shadeCenter = Offset(x, ly + 8 * p)
            scope.drawCircle(Color(0xFFFFD166).copy(alpha = 0.12f), radius = 22 * p, center = shadeCenter)
            scope.drawCircle(Color(0xFFFFF3B0).copy(alpha = 0.22f), radius = 12 * p, center = shadeCenter)

            // Warm light cone downward
            val coneTiers = 4
            for (t in 0 until coneTiers) {
                val frac = (t + 1).toFloat() / coneTiers
                val tierH = (lampH - 14 * p) / coneTiers
                val tierY = ly + 14 * p + t * tierH
                val tierW = 14 * p + frac * 42 * p
                val alpha = (0.24f * (1f - frac * 0.40f)).coerceIn(0.04f, 0.28f)
                scope.drawRect(
                    color = Color(0xFFFFEAA7).copy(alpha = alpha),
                    topLeft = Offset(x - tierW / 2f, tierY),
                    size = Size(tierW, tierH + 1.5f)
                )
            }

            // Warm oval light pool on the floor/rug
            scope.drawOval(
                color = Color(0xFFFFD166).copy(alpha = 0.24f),
                topLeft = Offset(x - 26 * p, groundY - 5 * p),
                size = Size(52 * p, 10 * p)
            )
            scope.drawOval(
                color = Color(0xFFFFF8D6).copy(alpha = 0.34f),
                topLeft = Offset(x - 14 * p, groundY - 3.5f * p),
                size = Size(28 * p, 7 * p)
            )
        } else {
            scope.drawRect(Color(0x10FFE66D), Offset(x - 14 * p, ly + 14 * p), Size(28 * p, 36 * p))
        }
    }

    fun drawBoyHoldingUmbrella(
        scope: DrawScope,
        boyX: Float,
        boyY: Float,
        girlX: Float,
        girlY: Float,
        boyFacingRight: Boolean,
        p: Float,
        timeSeconds: Float,
        isSitting: Boolean = false
    ) {
        val girlDist = abs(girlX - boyX)
        val isGirlClose = girlDist < 38f * p

        // Center the umbrella dome to shelter both comfortably, or directly over boy if separated
        val domeX = if (isGirlClose) {
            (boyX + girlX) / 2f
        } else {
            boyX + (if (boyFacingRight) 6f else -6f) * p
        }

        // Umbrella vertical coordinates
        val domeTopY = minOf(boyY, girlY) - (if (isSitting) 32f else 38f) * p + sin(timeSeconds * 2.2f) * 1.5f * p
        val domeW = if (isGirlClose) 44f * p else 32f * p
        val domeHalfW = domeW / 2f

        // The pole ALWAYS comes vertically straight down from the dome apex at domeX
        val poleX = domeX
        val poleColor = Color(0xFF2B2D42)
        val poleW = 1.8f * p
        val handY = boyY - (if (isSitting) 13.5f else 17.5f) * p

        // 1. Sleek graphite umbrella pole connecting from dome apex down to boy's hand
        scope.drawRect(
            color = poleColor,
            topLeft = Offset(poleX - poleW / 2f, domeTopY + 3.5f * p),
            size = Size(poleW, (handY - (domeTopY + 3.5f * p)).coerceAtLeast(4f * p))
        )

        // 2. Wooden J-hook handle directly below boy's hand
        val woodDark = Color(0xFF582F0E)
        val woodLight = Color(0xFF7F4F24)
        // Handle grip section
        scope.drawRect(woodDark, Offset(poleX - p, handY - 1.5f * p), Size(2 * p, 4 * p))
        // J-curve stem and hook curved away from the girl (back towards the boy)
        val hookDir = if (boyFacingRight) -1 else 1
        scope.drawRect(woodLight, Offset(poleX - p, handY + 2.5f * p), Size(2 * p, 3.5f * p))
        scope.drawRect(woodDark, Offset(poleX + hookDir * 2f * p, handY + 4.5f * p), Size(2.2f * p, 1.8f * p))
        scope.drawRect(woodDark, Offset(poleX + hookDir * 3.2f * p, handY + 3f * p), Size(1.6f * p, 2.2f * p))

        // 3. Boy's hand gripping the pole securely
        val skinColor = PixelArtRenderer.SkinToneBoy
        val skinShadow = PixelArtRenderer.SkinToneBoyShadow
        scope.drawRect(skinColor, Offset(poleX - 1.8f * p, handY - 1.5f * p), Size(3.6f * p, 3.2f * p))
        scope.drawRect(skinShadow, Offset(poleX - 0.5f * p, handY - p), Size(1.8f * p, 2.4f * p))

        // 4. Elegant pointed metal tip on top of dome
        scope.drawRect(Color(0xFF495057), Offset(domeX - p, domeTopY - 3.5f * p), Size(2 * p, 4f * p))
        scope.drawRect(Color(0xFFCED4DA), Offset(domeX - 0.5f * p, domeTopY - 3.5f * p), Size(p, 2f * p))

        // 5. Curved umbrella canopy dome (covers couple cozily) — GREEN with white polka dots
        val canopyGreen = Color(0xFF388E3C)   // vibrant forest green
        val canopyDark  = Color(0xFF1B5E20)   // deep green for apex/rim
        val canopyCream = Color(0xFFFDF0D5)   // cream ribs

        // Tier 1: Apex
        scope.drawRect(canopyDark, Offset(domeX - 4 * p, domeTopY), Size(8 * p, 1.8f * p))
        // Tier 2: Upper slope
        scope.drawRect(canopyGreen, Offset(domeX - 10 * p, domeTopY + 1.8f * p), Size(20 * p, 2.2f * p))
        // Tier 3: Mid dome
        scope.drawRect(canopyGreen, Offset(domeX - 16 * p, domeTopY + 4f * p), Size(32 * p, 2.8f * p))
        // Tier 4: Wide canopy sheltering both
        scope.drawRect(canopyGreen, Offset(domeX - domeHalfW, domeTopY + 6.8f * p), Size(domeW, 3.2f * p))
        // Tier 5: Scalloped rim shadow
        scope.drawRect(canopyDark, Offset(domeX - domeHalfW, domeTopY + 10f * p), Size(domeW, 1.8f * p))

        // Soft cream ribs radiating symmetrically
        scope.drawRect(canopyCream, Offset(domeX - 0.8f * p, domeTopY + 1.5f * p), Size(1.6f * p, 9f * p))
        if (isGirlClose) {
            scope.drawRect(canopyCream.copy(alpha = 0.85f), Offset(domeX - 11 * p, domeTopY + 3.5f * p), Size(1.4f * p, 7f * p))
            scope.drawRect(canopyCream.copy(alpha = 0.85f), Offset(domeX + 9.6f * p, domeTopY + 3.5f * p), Size(1.4f * p, 7f * p))
        }

        // White polka dots on canopy
        val dotColor = Color(0xCCFFFFFF)
        val dotR = 1.5f * p
        // Left cluster
        scope.drawCircle(dotColor, dotR, Offset(domeX - 12 * p, domeTopY + 7f * p))
        scope.drawCircle(dotColor, dotR, Offset(domeX - 6 * p, domeTopY + 5f * p))
        // Center cluster
        scope.drawCircle(dotColor, dotR * 0.9f, Offset(domeX, domeTopY + 8.5f * p))
        // Right cluster
        scope.drawCircle(dotColor, dotR, Offset(domeX + 6 * p, domeTopY + 5f * p))
        scope.drawCircle(dotColor, dotR, Offset(domeX + 12 * p, domeTopY + 7f * p))

        // 6. Rain splash droplets bouncing off umbrella dome
        val splash1 = domeX - 8 * p + sin(timeSeconds * 12f) * 4 * p
        val splash2 = domeX + 7 * p + cos(timeSeconds * 15f) * 4 * p
        scope.drawRect(Color(0xB3A8DADC), Offset(splash1, domeTopY - 2.5f * p), Size(2 * p, 2 * p))
        scope.drawRect(Color(0xB3A8DADC), Offset(splash2, domeTopY - 2.5f * p), Size(2 * p, 2 * p))
    }

    fun drawCoupleUmbrella(scope: DrawScope, cx: Float, topY: Float, p: Float, timeSeconds: Float) {
        drawBoyHoldingUmbrella(scope, cx - 8 * p, topY + 28 * p, cx + 8 * p, topY + 28 * p, true, p, timeSeconds, false)
    }

    fun drawCozyBed(
        scope: DrawScope,
        centerX: Float,
        groundY: Float,
        p: Float = 3.5f,
        timeSeconds: Float = 0f
    ) {
        val bedW = 68 * p
        val bedH = 34 * p
        val left = centerX - bedW / 2f
        val top = groundY - bedH

        // 1. Dark warm wooden headboard
        scope.drawRect(Color(0xFF582F0E), Offset(left, top), Size(bedW, 16 * p))
        scope.drawRect(Color(0xFF7F4F24), Offset(left + 2 * p, top + 2 * p), Size(bedW - 4 * p, 12 * p))
        for (i in 1..4) {
            val sx = left + i * (bedW / 5f)
            scope.drawRect(Color(0xFF582F0E), Offset(sx, top + 2 * p), Size(2 * p, 12 * p))
        }

        // 2. Plump Pillows resting against headboard
        val pillowY = top + 8 * p
        // Left pillow
        scope.drawRect(Color(0xFFEDE0D4), Offset(left + 6 * p, pillowY), Size(22 * p, 10 * p))
        scope.drawRect(Color(0xFFFFFFFF), Offset(left + 7 * p, pillowY + p), Size(20 * p, 8 * p))
        // Right pillow
        scope.drawRect(Color(0xFFEDE0D4), Offset(left + 38 * p, pillowY), Size(22 * p, 10 * p))
        scope.drawRect(Color(0xFFFFFFFF), Offset(left + 39 * p, pillowY + p), Size(20 * p, 8 * p))

        // 3. Fluffy Quilt / Duvet (Cozy Blush Pink / Rose with warm cream folded trim)
        val quiltY = top + 15 * p
        val quiltH = bedH - 15 * p
        scope.drawRect(Color(0xFFE5989B), Offset(left + 2 * p, quiltY), Size(bedW - 4 * p, quiltH))
        // Folded top sheet edge
        scope.drawRect(Color(0xFFFFF1E6), Offset(left + 2 * p, quiltY), Size(bedW - 4 * p, 4 * p))
        scope.drawRect(Color(0xFFDDBEA9), Offset(left + 2 * p, quiltY + 3.5f * p), Size(bedW - 4 * p, 1 * p))

        // Diamond quilt stitching dots
        val quiltColor2 = Color(0xFFB56576)
        for (i in 0 until 5) {
            val qx = left + 8 * p + i * 11 * p
            scope.drawRect(quiltColor2, Offset(qx, quiltY + 6 * p), Size(2 * p, 2 * p))
            scope.drawRect(quiltColor2, Offset(qx + 5.5f * p, quiltY + 11 * p), Size(2 * p, 2 * p))
        }

        // 4. Knitted cozy throw blanket at foot of bed
        scope.drawRect(Color(0xFF6B705C), Offset(left + 2 * p, top + bedH - 7 * p), Size(bedW - 4 * p, 7 * p))
        scope.drawRect(Color(0xFFA5A58D), Offset(left + 2 * p, top + bedH - 7 * p), Size(bedW - 4 * p, 1.5f * p))

        // 5. Plush Little Teddy Bear resting against the pillow!
        val bearX = left + 32 * p
        val bearY = top + 9 * p
        scope.drawRect(Color(0xFF9C6644), Offset(bearX - 4 * p, bearY), Size(8 * p, 7 * p))
        scope.drawRect(Color(0xFF7F4F24), Offset(bearX - 5 * p, bearY - 2 * p), Size(3 * p, 3 * p))
        scope.drawRect(Color(0xFF7F4F24), Offset(bearX + 2 * p, bearY - 2 * p), Size(3 * p, 3 * p))
        scope.drawRect(Color(0xFFDDBEA9), Offset(bearX - 2 * p, bearY + 2.5f * p), Size(4 * p, 3 * p))
        scope.drawRect(Color(0xFF2C1810), Offset(bearX - p, bearY + 3 * p), Size(2 * p, 1.5f * p))
        scope.drawRect(Color(0xFF2C1810), Offset(bearX - 2.5f * p, bearY + 2 * p), Size(1.2f * p, 1.2f * p))
        scope.drawRect(Color(0xFF2C1810), Offset(bearX + 1.5f * p, bearY + 2 * p), Size(1.2f * p, 1.2f * p))
        scope.drawRect(Color(0xFFC1121F), Offset(bearX - 2 * p, bearY + 6.5f * p), Size(4 * p, 1.5f * p))

        // 6. Wooden bed posts / legs
        scope.drawRect(Color(0xFF582F0E), Offset(left - p, top + bedH - 5 * p), Size(4 * p, 5 * p))
        scope.drawRect(Color(0xFF582F0E), Offset(left + bedW - 3 * p, top + bedH - 5 * p), Size(4 * p, 5 * p))
    }

    /**
     * Pixel-art wall calendar hung on the cottage living room wall.
     * cx/cy = center of the calendar.
     * Tapping this opens the Special Relationship Calendar dialog.
     */
    fun drawWallCalendar(scope: DrawScope, cx: Float, topY: Float, p: Float, timeSeconds: Float) {
        val w = 18 * p
        val h = 22 * p
        val left = cx - w / 2f
        val top = topY - h / 2f

        // Calendar body — cream/white paper
        scope.drawRect(Color(0xFFFDFBF5), Offset(left, top), Size(w, h))
        // Border
        scope.drawRect(Color(0xFFD4A373), Offset(left, top), Size(w, 1.5f * p))      // top
        scope.drawRect(Color(0xFFD4A373), Offset(left, top + h - 1.5f * p), Size(w, 1.5f * p)) // bottom
        scope.drawRect(Color(0xFFD4A373), Offset(left, top), Size(1.5f * p, h))      // left
        scope.drawRect(Color(0xFFD4A373), Offset(left + w - 1.5f * p, top), Size(1.5f * p, h)) // right

        // Header strip — rose pink
        scope.drawRect(Color(0xFFC9184A), Offset(left + 1.5f * p, top + 1.5f * p), Size(w - 3 * p, 5 * p))

        // Heart on header (tiny 3×3 pixel heart)
        val hx = cx - 1.5f * p
        val hy = top + 2.5f * p
        scope.drawRect(Color(0xFFFFFFFF), Offset(hx - p, hy), Size(p, 1.5f * p))
        scope.drawRect(Color(0xFFFFFFFF), Offset(hx, hy - 0.5f * p), Size(p, 2.5f * p))
        scope.drawRect(Color(0xFFFFFFFF), Offset(hx + p, hy), Size(p, 1.5f * p))
        scope.drawRect(Color(0xFFFFFFFF), Offset(hx - 0.5f * p, hy + 1.5f * p), Size(3 * p, p))

        // Grid lines (3 columns × 3 rows of tiny date cells)
        val gridLeft = left + 2 * p
        val gridTop = top + 8 * p
        val cellW = (w - 4 * p) / 3f
        val cellH = (h - 11 * p) / 3f
        val heartedRow = 1; val heartedCol = 1 // highlighted cell in center
        for (row in 0..2) {
            for (col in 0..2) {
                val cx2 = gridLeft + col * cellW
                val cy2 = gridTop + row * cellH
                // Highlight one cell in warm rose (the "special" day)
                if (row == heartedRow && col == heartedCol) {
                    scope.drawRect(Color(0xFFFFD6E0), Offset(cx2, cy2), Size(cellW - p, cellH - p))
                    // Small heart dot inside highlighted cell
                    scope.drawRect(Color(0xFFC9184A), Offset(cx2 + cellW * 0.3f, cy2 + cellH * 0.3f), Size(p, p))
                } else {
                    scope.drawRect(Color(0xFFEEE8D5), Offset(cx2, cy2), Size(cellW - p, cellH - p))
                }
                // Tiny dot for "date number"
                scope.drawRect(Color(0xFF888880), Offset(cx2 + cellW * 0.2f, cy2 + cellH * 0.2f), Size(0.8f * p, 0.8f * p))
            }
        }

        // Hanging string at top
        scope.drawRect(Color(0xFF6B4F35), Offset(cx - 0.7f * p, topY - h / 2f - 2.5f * p), Size(1.4f * p, 2.5f * p))

        // Subtle glow twinkle to hint interactivity (blinks every ~2 sec)
        val twinkle = (kotlin.math.sin(timeSeconds * 2.5f) * 0.5f + 0.5f).coerceIn(0f, 1f)
        if (twinkle > 0.7f) {
            scope.drawRect(Color(0x30C9184A), Offset(left - p, top - p), Size(w + 2 * p, h + 2 * p))
        }
    }

    /**
     * Cozy Cottage Double-Door Wardrobe filled with outfits and shoes for both characters.
     */
    fun drawWardrobe(
        scope: DrawScope,
        cx: Float,
        groundY: Float,
        p: Float,
        timeSeconds: Float
    ) {
        val w = 34 * p
        val h = 68 * p
        val left = cx - w / 2f
        val top = groundY - h

        // 1. Ornate Wooden Pediment / Crown Molding Header
        val woodDark = Color(0xFF43281C)
        val woodMid = Color(0xFF582F0E)
        val woodLight = Color(0xFF7F4F24)
        val woodHighlight = Color(0xFF996235)

        // Arched top header pediment
        scope.drawRect(woodDark, Offset(left + 2 * p, top), Size(w - 4 * p, 4 * p))
        scope.drawRect(woodMid, Offset(left + 4 * p, top + p), Size(w - 8 * p, 2.5f * p))
        // Decorative crest in center
        scope.drawRect(woodLight, Offset(cx - 3 * p, top - 1.5f * p), Size(6 * p, 2.5f * p))
        scope.drawRect(Color(0xFFFFD166), Offset(cx - p, top - p), Size(2 * p, 1.5f * p)) // gold crest inlay
        // Carved finials on top corners
        scope.drawRect(woodLight, Offset(left + 0.5f * p, top + p), Size(2.5f * p, 3 * p))
        scope.drawRect(woodLight, Offset(left + w - 3 * p, top + p), Size(2.5f * p, 3 * p))
        // Crown cornice strip
        scope.drawRect(woodLight, Offset(left - p, top + 4 * p), Size(w + 2 * p, 2.5f * p))
        scope.drawRect(woodHighlight, Offset(left - p, top + 4 * p), Size(w + 2 * p, 0.8f * p))

        // 2. Base Cabriole Feet & Bottom Skirt
        scope.drawRect(woodDark, Offset(left + 2 * p, groundY - 3 * p), Size(4 * p, 3 * p))
        scope.drawRect(woodDark, Offset(left + w - 6 * p, groundY - 3 * p), Size(4 * p, 3 * p))
        scope.drawRect(woodMid, Offset(left + 3 * p, groundY - 2.5f * p), Size(2.5f * p, 2.5f * p))
        scope.drawRect(woodMid, Offset(left + w - 5.5f * p, groundY - 2.5f * p), Size(2.5f * p, 2.5f * p))
        // Bottom plinth / apron
        scope.drawRect(woodLight, Offset(left - 0.5f * p, groundY - 5 * p), Size(w + p, 2.5f * p))

        // 3. Main Outer Wardrobe Frame
        scope.drawRect(woodMid, Offset(left, top + 6.5f * p), Size(w, h - 11.5f * p))
        scope.drawRect(woodLight, Offset(left + 1.5f * p, top + 6.5f * p), Size(w - 3 * p, h - 11.5f * p))

        // 4. Interior Cabinet Space (Aromatic Cedar Wood Backing)
        val cedarBg = Color(0xFFDDB892)
        val cedarShadow = Color(0xFFCCA57E)
        val innerLeft = left + 3 * p
        val innerTop = top + 7.5f * p
        val innerW = w - 6 * p
        val innerH = h - 13.5f * p
        scope.drawRect(cedarBg, Offset(innerLeft, innerTop), Size(innerW, innerH))
        // Cedar vertical panel lines
        var cedarX = innerLeft + 4 * p
        while (cedarX < innerLeft + innerW - 2 * p) {
            scope.drawRect(cedarShadow, Offset(cedarX, innerTop), Size(0.8f * p, innerH))
            cedarX += 4.5f * p
        }

        // 5. Upper Hatbox & Sweaters Shelf
        val shelf1Y = innerTop + 13 * p
        scope.drawRect(woodMid, Offset(innerLeft, shelf1Y), Size(innerW, 2 * p))
        scope.drawRect(woodHighlight, Offset(innerLeft, shelf1Y), Size(innerW, 0.6f * p))

        // On top shelf:
        // Vintage pastel hatbox on left
        scope.drawRect(Color(0xFFFFCAD4), Offset(innerLeft + 2 * p, shelf1Y - 9 * p), Size(8 * p, 9 * p))
        scope.drawRect(Color(0xFFFF758F), Offset(innerLeft + 1.5f * p, shelf1Y - 10 * p), Size(9 * p, 2 * p)) // lid
        scope.drawRect(Color(0xFFFF5D8F), Offset(innerLeft + 5 * p, shelf1Y - 9 * p), Size(2 * p, 9 * p)) // ribbon
        scope.drawRect(Color(0xFFFF5D8F), Offset(innerLeft + 4 * p, shelf1Y - 11.5f * p), Size(4 * p, 1.8f * p)) // bow

        // Stack of folded pastel knit sweaters and folded accessories in middle/right
        val stackX = innerLeft + 12 * p
        scope.drawRect(Color(0xFFC3DBD0), Offset(stackX, shelf1Y - 3 * p), Size(6 * p, 3 * p)) // mint folded knit
        scope.drawRect(Color(0xFFA7C4B5), Offset(stackX + 4.5f * p, shelf1Y - 3 * p), Size(1.5f * p, 3 * p))
        scope.drawRect(Color(0xFFE8D7F1), Offset(stackX + 0.5f * p, shelf1Y - 5.8f * p), Size(5.5f * p, 2.8f * p)) // lavender
        // Folded Accessories on right side of upper shelf:
        // Folded Beanie with pom-pom
        val beanieX = innerLeft + 19 * p
        scope.drawRect(Color(0xFF264653), Offset(beanieX, shelf1Y - 3.5f * p), Size(4 * p, 3.5f * p))
        scope.drawRect(Color(0xFF1B4332), Offset(beanieX, shelf1Y - 1.2f * p), Size(4 * p, 1.2f * p))
        scope.drawRect(Color(0xFFE9C46A), Offset(beanieX + 1.2f * p, shelf1Y - 5 * p), Size(1.6f * p, 1.5f * p)) // pom-pom
        // Folded Baseball Cap
        val capX = innerLeft + 24 * p
        scope.drawRect(Color(0xFF1D3557), Offset(capX, shelf1Y - 3 * p), Size(3.5f * p, 3 * p))
        scope.drawRect(Color(0xFF0F172A), Offset(capX - 1.5f * p, shelf1Y - 1.2f * p), Size(2f * p, 1.2f * p)) // visor brim

        // 6. Brass Clothes Hanging Rail
        val railY = shelf1Y + 3.5f * p
        val railColor = Color(0xFFFFD166)
        val railShadow = Color(0xFFC99700)
        scope.drawRect(railColor, Offset(innerLeft + p, railY), Size(innerW - 2 * p, 1.5f * p))
        scope.drawRect(railShadow, Offset(innerLeft + p, railY + 1.2f * p), Size(innerW - 2 * p, 0.5f * p))
        scope.drawRect(Color(0xFF8C6D37), Offset(innerLeft, railY - p), Size(1.5f * p, 3.5f * p)) // left bracket
        scope.drawRect(Color(0xFF8C6D37), Offset(innerLeft + innerW - 1.5f * p, railY - p), Size(1.5f * p, 3.5f * p)) // right bracket

        // 7. Hanging Garments (Split his-and-hers wardrobe rail)
        fun drawHangingDress(
            hx: Float,
            hangLen: Float,
            dressColor: Color,
            dressAccent: Color,
            isBoyItem: Boolean = false,
            isBoyHoodie: Boolean = false
        ) {
            // Brass hanger
            scope.drawRect(railColor, Offset(hx - 1.5f * p, railY + 1.2f * p), Size(3 * p, 1.5f * p))
            scope.drawRect(Color(0xFF8C6D37), Offset(hx - 0.5f * p, railY - p), Size(p, 1.2f * p)) // hook

            val dressTopY = railY + 2.5f * p
            if (isBoyItem) {
                // Boy's sweater or hoodie hanging
                scope.drawRect(dressColor, Offset(hx - 2 * p, dressTopY), Size(4 * p, hangLen))
                scope.drawRect(dressAccent, Offset(hx - 0.5f * p, dressTopY), Size(p, hangLen * 0.45f)) // collar / trim
                if (isBoyHoodie) {
                    // Kangaroo pocket pouch
                    scope.drawRect(dressAccent, Offset(hx - 1.5f * p, dressTopY + hangLen - 5 * p), Size(3 * p, 3.5f * p))
                    // Drawstrings
                    scope.drawRect(dressAccent, Offset(hx - p, dressTopY + 1.5f * p), Size(0.5f * p, 3 * p))
                    scope.drawRect(dressAccent, Offset(hx + 0.5f * p, dressTopY + 1.5f * p), Size(0.5f * p, 3 * p))
                }
            } else {
                // Girl's Dress with flowing flared skirt
                scope.drawRect(dressColor, Offset(hx - 1.8f * p, dressTopY), Size(3.6f * p, hangLen * 0.45f)) // bodice
                scope.drawRect(dressAccent, Offset(hx - p, dressTopY), Size(2 * p, p)) // neck trim
                // Flaring skirt
                val skirtTopY = dressTopY + hangLen * 0.42f
                val skirtH = hangLen * 0.58f
                scope.drawRect(dressColor, Offset(hx - 2.5f * p, skirtTopY), Size(5 * p, skirtH))
                scope.drawRect(dressAccent, Offset(hx - 2.5f * p, skirtTopY + skirtH - 1.2f * p), Size(5 * p, 1.2f * p)) // hem frill
                scope.drawRect(dressAccent.copy(alpha = 0.4f), Offset(hx + 0.5f * p, skirtTopY), Size(0.8f * p, skirtH)) // pleat fold
            }
        }

        // Hanging Rack lineup:
        // Her Side (Left): 4 dresses & hoodies
        drawHangingDress(innerLeft + 2.6f * p, 24 * p, Color(0xFFF5CAC3), Color(0xFFD6587A)) // Strawberry knit
        drawHangingDress(innerLeft + 5.8f * p, 25 * p, Color(0xFFE8D7F1), Color(0xFF9D4EDD)) // Lavender dream
        drawHangingDress(innerLeft + 9.0f * p, 26 * p, Color(0xFF74C69D), Color(0xFF2D6A4F)) // Emerald gown
        drawHangingDress(innerLeft + 12.2f * p, 23 * p, Color(0xFFF4ACB7), Color(0xFFFFCAD4)) // Blush Rose hoodie

        // Small brass divider ring between hers and his
        scope.drawRect(Color(0xFF8C6D37), Offset(innerLeft + 14.5f * p, railY - 0.5f * p), Size(0.8f * p, 2.5f * p))

        // His Side (Right): 4 outfits & hoodies
        drawHangingDress(innerLeft + 16.5f * p, 22 * p, Color(0xFF2D6A4F), Color(0xFF1B4332), isBoyItem = true) // Classic Spruce knit
        drawHangingDress(innerLeft + 19.8f * p, 22 * p, Color(0xFFF8F9FA), Color(0xFF2D6A4F), isBoyItem = true, isBoyHoodie = true) // White & Emerald hoodie
        drawHangingDress(innerLeft + 23.0f * p, 22 * p, Color(0xFF343A40), Color(0xFF495057), isBoyItem = true, isBoyHoodie = true) // Charcoal zip hoodie
        drawHangingDress(innerLeft + 26.2f * p, 19 * p, Color(0xFF1D3557), Color(0xFF457B9D), isBoyItem = true) // Blue flannel

        // 8. Lower Shelf & Shoe Cubby Unit
        val shelf2Y = top + h - 16 * p
        scope.drawRect(woodMid, Offset(innerLeft, shelf2Y), Size(innerW, 2 * p))
        scope.drawRect(woodHighlight, Offset(innerLeft, shelf2Y), Size(innerW, 0.6f * p))

        // Shoe cubby floor:
        // Girl's Rose Heels
        scope.drawRect(Color(0xFFFF758F), Offset(innerLeft + 2 * p, groundY - 7.5f * p), Size(3.5f * p, 2.5f * p))
        scope.drawRect(Color(0xFFC9184A), Offset(innerLeft + 4.5f * p, groundY - 6.5f * p), Size(p, 2 * p)) // heel stem
        // Girl's Fluffy White Bunny Slippers
        scope.drawRect(Color(0xFFFFF0F3), Offset(innerLeft + 7.5f * p, groundY - 7.5f * p), Size(4.5f * p, 2.5f * p))
        scope.drawRect(Color(0xFFFFCAD4), Offset(innerLeft + 8.5f * p, groundY - 8.5f * p), Size(1.2f * p, 1.5f * p)) // bunny ear
        scope.drawRect(Color(0xFFFFCAD4), Offset(innerLeft + 10.2f * p, groundY - 8.5f * p), Size(1.2f * p, 1.5f * p))
        // Girl's Mint Flats
        scope.drawRect(Color(0xFFA7C4B5), Offset(innerLeft + 14 * p, groundY - 7.5f * p), Size(4 * p, 2.5f * p))
        scope.drawRect(Color(0xFF6B9080), Offset(innerLeft + 15 * p, groundY - 7.5f * p), Size(2 * p, 0.8f * p)) // bow
        // Boy's Pair of Brown Boots (neatly in his corner)
        scope.drawRect(Color(0xFF4A2810), Offset(innerLeft + 21.5f * p, groundY - 8.5f * p), Size(5f * p, 3.5f * p))
        scope.drawRect(Color(0xFF2B1704), Offset(innerLeft + 21.5f * p, groundY - 6f * p), Size(5.2f * p, p))

        // 9. Open Double Doors Swung Outward
        val doorW = 5.5f * p
        val doorH = h - 16 * p
        val doorTopY = top + 7 * p
        scope.drawRect(woodDark, Offset(left - doorW + p, doorTopY), Size(doorW, doorH))
        scope.drawRect(woodMid, Offset(left - doorW + 1.5f * p, doorTopY + p), Size(doorW - p, doorH - 2 * p))
        scope.drawRect(woodLight, Offset(left - doorW + 2 * p, doorTopY + 3 * p), Size(doorW - 2 * p, doorH * 0.4f))
        scope.drawRect(woodLight, Offset(left - doorW + 2 * p, doorTopY + doorH * 0.5f), Size(doorW - 2 * p, doorH * 0.4f))
        scope.drawRect(Color(0xFFFFD166), Offset(left + 0.2f * p, doorTopY + doorH * 0.48f), Size(1.2f * p, 3 * p))

        scope.drawRect(woodDark, Offset(left + w - p, doorTopY), Size(doorW, doorH))
        scope.drawRect(woodMid, Offset(left + w - 0.5f * p, doorTopY + p), Size(doorW - p, doorH - 2 * p))
        scope.drawRect(woodLight, Offset(left + w, doorTopY + 3 * p), Size(doorW - 2 * p, doorH * 0.4f))
        scope.drawRect(woodLight, Offset(left + w, doorTopY + doorH * 0.5f), Size(doorW - 2 * p, doorH * 0.4f))
        scope.drawRect(Color(0xFFFFD166), Offset(left + w - 1.4f * p, doorTopY + doorH * 0.48f), Size(1.2f * p, 3 * p))

        // 10. Subtle golden shimmer to hint interactivity
        val shimmer = (kotlin.math.sin(timeSeconds * 2.2f) * 0.5f + 0.5f).coerceIn(0f, 1f)
        if (shimmer > 0.75f) {
            scope.drawRect(Color(0x25FFD166), Offset(left, top), Size(w, h))
        }
    }

    /**
     * Retro Kitchen Pedal Dustbin (trash can) with chrome step pedal and dome lid
     */
    fun drawPedalDustbin(
        scope: DrawScope,
        cx: Float,
        groundY: Float,
        p: Float
    ) {
        val binW = 10 * p
        val binH = 16 * p
        val binLeft = cx - binW / 2f
        val binTop = groundY - binH

        // 1. Chrome Foot Pedal at bottom center
        scope.drawRect(Color(0xFF2B2D42), Offset(cx - 5.5f * p, groundY - 1.8f * p), Size(11 * p, 1.8f * p)) // base ring
        scope.drawRect(Color(0xFFCED4DA), Offset(cx - 2.5f * p, groundY - 2.2f * p), Size(5 * p, 1.8f * p)) // pedal step
        scope.drawRect(Color(0xFF6C757D), Offset(cx - 1.5f * p, groundY - 1.2f * p), Size(3 * p, 1.2f * p)) // pedal hinge

        // 2. Retro Cream / Stainless Steel Cylindrical Body
        val bodyTop = binTop + 3.5f * p
        val bodyH = binH - 4.5f * p
        val bodyW = 9 * p
        val bodyLeft = cx - bodyW / 2f

        // Body shading from highlight to shadow
        scope.drawRect(Color(0xFFE9ECEF), Offset(bodyLeft, bodyTop), Size(bodyW, bodyH))
        scope.drawRect(Color(0xFFF8F9FA), Offset(bodyLeft + 1 * p, bodyTop), Size(2 * p, bodyH)) // metallic sheen
        scope.drawRect(Color(0xFFDEE2E6), Offset(bodyLeft + 3 * p, bodyTop), Size(3.5f * p, bodyH))
        scope.drawRect(Color(0xFFCED4DA), Offset(bodyLeft + 6.5f * p, bodyTop), Size(2.5f * p, bodyH)) // shadow side

        // Inner liner collar band
        scope.drawRect(Color(0xFF495057), Offset(bodyLeft - 0.4f * p, bodyTop), Size(bodyW + 0.8f * p, 1.2f * p))

        // Embossed sweet motif on front of dustbin (clean house / happy heart)
        scope.drawRect(Color(0xFFFF758F), Offset(cx - 1.2f * p, bodyTop + 5 * p), Size(2.4f * p, 2.2f * p))
        scope.drawRect(Color(0xFFFF758F), Offset(cx - 0.6f * p, bodyTop + 7.2f * p), Size(1.2f * p, 0.8f * p))

        // 3. Domed Pedal Lid
        val lidW = 10.5f * p
        val lidLeft = cx - lidW / 2f
        // Chrome lid rim
        scope.drawRect(Color(0xFFADB5BD), Offset(lidLeft, binTop + 2 * p), Size(lidW, 1.8f * p))
        scope.drawRect(Color(0xFFF8F9FA), Offset(lidLeft + 1.5f * p, binTop + 2 * p), Size(3 * p, 0.8f * p))
        // Domed top
        scope.drawRect(Color(0xFFDEE2E6), Offset(cx - 4 * p, binTop + 0.6f * p), Size(8 * p, 1.6f * p))
        scope.drawRect(Color(0xFFE9ECEF), Offset(cx - 2.5f * p, binTop), Size(5 * p, 1f * p))
        // Top handle loop
        scope.drawRect(Color(0xFF6C757D), Offset(cx - 1.8f * p, binTop - 1.2f * p), Size(3.6f * p, 1.4f * p))
    }

    /**
     * Potted succulent / jade plant for windowsill or counter.
     */
    fun drawWindowsillPlant(
        scope: DrawScope,
        cx: Float,
        groundY: Float,
        p: Float
    ) {
        val potW = 7 * p
        val potH = 6 * p
        val potLeft = cx - potW / 2f
        val potTop = groundY - potH
        val potBase = Color(0xFFC86D51) // warm terracotta
        val potShadow = Color(0xFFA55238)
        val plantGreen = Color(0xFF588157)
        val plantHighlight = Color(0xFFA3B18A)

        // Pot
        scope.drawRect(potBase, Offset(potLeft, potTop), Size(potW, potH))
        scope.drawRect(potShadow, Offset(potLeft + potW - 1.5f * p, potTop), Size(1.5f * p, potH))
        scope.drawRect(potBase, Offset(potLeft - 0.5f * p, potTop), Size(potW + p, 1.5f * p)) // rim
        // Soil
        scope.drawRect(Color(0xFF3A2312), Offset(potLeft + p, potTop + 0.5f * p), Size(potW - 2 * p, p))
        // Plump succulent rosettes
        scope.drawRect(plantGreen, Offset(cx - 3 * p, potTop - 3 * p), Size(6 * p, 3 * p))
        scope.drawRect(plantHighlight, Offset(cx - 2 * p, potTop - 4.5f * p), Size(4 * p, 2 * p))
        scope.drawRect(Color(0xFF344E41), Offset(cx - p, potTop - 2 * p), Size(2 * p, 2 * p))
    }

    /**
     * Hand-knitted soft throw blanket draped over the arm of the living room couch.
     */
    fun drawCouchKnitThrow(
        scope: DrawScope,
        couchLeft: Float,
        couchTop: Float,
        p: Float
    ) {
        val throwColor = Color(0xFFE8D7F1) // pastel lavender knit
        val throwPattern = Color(0xFFD0B8E3)
        val fringeColor = Color(0xFFF3EAF8)
        val throwLeft = couchLeft + 2 * p
        val throwTop = couchTop + 6 * p
        val throwW = 8 * p
        val throwH = 14 * p

        scope.drawRect(throwColor, Offset(throwLeft, throwTop), Size(throwW, throwH))
        // Textured knit rib lines
        scope.drawRect(throwPattern, Offset(throwLeft + 2 * p, throwTop), Size(1.2f * p, throwH))
        scope.drawRect(throwPattern, Offset(throwLeft + 5 * p, throwTop), Size(1.2f * p, throwH))
        // Delicate bottom fringe
        for (i in 0 until 4) {
            scope.drawRect(fringeColor, Offset(throwLeft + i * 2f * p, throwTop + throwH), Size(1.2f * p, 2f * p))
        }
    }

    /**
     * Gleaming copper tea kettle on the kitchen stove with soft rising steam wisps.
     */
    fun drawCopperTeakettle(
        scope: DrawScope,
        stoveX: Float,
        stoveY: Float,
        p: Float,
        timeSeconds: Float
    ) {
        val copperLight = Color(0xFFE07A5F)
        val copperDark = Color(0xFFB55238)
        val copperShine = Color(0xFFFFB4A2)
        val woodHandle = Color(0xFF582F0E)

        val kw = 10 * p
        val kh = 7 * p
        val kLeft = stoveX - kw / 2f
        val kTop = stoveY - kh

        // Body
        scope.drawRect(copperLight, Offset(kLeft, kTop), Size(kw, kh))
        scope.drawRect(copperDark, Offset(kLeft + kw - 2 * p, kTop), Size(2 * p, kh))
        scope.drawRect(copperShine, Offset(kLeft + 2 * p, kTop + p), Size(2 * p, kh - 2 * p))
        // Lid & brass knob
        scope.drawRect(copperLight, Offset(kLeft + 2 * p, kTop - 1.5f * p), Size(kw - 4 * p, 1.5f * p))
        scope.drawRect(Color(0xFFFFD166), Offset(stoveX - p, kTop - 2.8f * p), Size(2 * p, 1.5f * p))
        // Curved Spout
        scope.drawRect(copperLight, Offset(kLeft - 2.5f * p, kTop + p), Size(3 * p, 2.5f * p))
        scope.drawRect(copperDark, Offset(kLeft - 3f * p, kTop + 0.5f * p), Size(1.5f * p, 1.5f * p))
        // Arched wood handle overhead
        scope.drawRect(woodHandle, Offset(kLeft + p, kTop - 5 * p), Size(kw - 2 * p, 1.5f * p))
        scope.drawRect(woodHandle, Offset(kLeft + p, kTop - 5 * p), Size(1.5f * p, 4 * p))
        scope.drawRect(woodHandle, Offset(kLeft + kw - 2.5f * p, kTop - 5 * p), Size(1.5f * p, 4 * p))

        // Gentle steam wisp
        val steamSway = sin(timeSeconds * 3.5f) * 1.5f * p
        scope.drawCircle(Color.White.copy(alpha = 0.40f), 1.8f * p, Offset(kLeft - 3.5f * p + steamSway, kTop - 2.5f * p))
        scope.drawCircle(Color.White.copy(alpha = 0.25f), 2.5f * p, Offset(kLeft - 4.5f * p - steamSway * 0.8f, kTop - 6 * p))
    }

    /**
     * Broadleaf Monstera Deliciosa in a glazed ceramic floor planter for the living room corner.
     */
    fun drawCornerMonstera(
        scope: DrawScope,
        cornerX: Float,
        groundY: Float,
        p: Float
    ) {
        val potW = 14 * p
        val potH = 12 * p
        val potLeft = cornerX - potW / 2f
        val potTop = groundY - potH

        // Ceramic white planter pot
        scope.drawRect(Color(0xFFE9ECEF), Offset(potLeft, potTop), Size(potW, potH))
        scope.drawRect(Color(0xFFCED4DA), Offset(potLeft + potW - 3 * p, potTop), Size(3 * p, potH))
        scope.drawRect(Color(0xFFF8F9FA), Offset(potLeft + 2 * p, potTop), Size(3 * p, potH)) // glaze sheen
        scope.drawRect(Color(0xFF342318), Offset(potLeft + 1.5f * p, potTop + 0.5f * p), Size(potW - 3 * p, 2 * p)) // rich soil

        // Arching Monstera stems & fenestrated leaves
        val leafDark = Color(0xFF1B4332)
        val leafMid = Color(0xFF2D6A4F)
        val leafBright = Color(0xFF40916C)

        // Leaf 1: arching left
        scope.drawRect(leafMid, Offset(cornerX - 12 * p, potTop - 18 * p), Size(10 * p, 11 * p))
        scope.drawRect(leafDark, Offset(cornerX - 7 * p, potTop - 14 * p), Size(1.5f * p, 8 * p)) // midrib
        scope.drawRect(Color.Transparent, Offset(cornerX - 10 * p, potTop - 16 * p), Size(2 * p, 2 * p)) // cutout
        scope.drawRect(leafBright, Offset(cornerX - 11 * p, potTop - 17 * p), Size(3 * p, 2 * p))

        // Leaf 2: soaring center high
        scope.drawRect(leafMid, Offset(cornerX - 4 * p, potTop - 25 * p), Size(11 * p, 13 * p))
        scope.drawRect(leafDark, Offset(cornerX + p, potTop - 22 * p), Size(1.5f * p, 10 * p))
        scope.drawRect(leafBright, Offset(cornerX - 2 * p, potTop - 24 * p), Size(4 * p, 2.5f * p))

        // Leaf 3: draping right
        scope.drawRect(leafMid, Offset(cornerX + 4 * p, potTop - 16 * p), Size(9 * p, 10 * p))
        scope.drawRect(leafDark, Offset(cornerX + 6 * p, potTop - 12 * p), Size(1.5f * p, 7 * p))
        scope.drawRect(leafBright, Offset(cornerX + 8 * p, potTop - 15 * p), Size(3 * p, 2 * p))
    }

    /**
     * Draped fairy string lights across wall with glowing amber bulbs.
     */
    fun drawFairyStringLights(
        scope: DrawScope,
        leftX: Float,
        rightX: Float,
        topY: Float,
        p: Float,
        timeSeconds: Float
    ) {
        val totalW = rightX - leftX
        val swags = 4
        val swagW = totalW / swags

        for (s in 0 until swags) {
            val sx = leftX + s * swagW
            val sag = 5 * p
            // Draw scalloped wire
            val wireColor = Color(0x88495057)
            scope.drawLine(
                color = wireColor,
                start = Offset(sx, topY),
                end = Offset(sx + swagW / 2f, topY + sag),
                strokeWidth = 1.2f * p
            )
            scope.drawLine(
                color = wireColor,
                start = Offset(sx + swagW / 2f, topY + sag),
                end = Offset(sx + swagW, topY),
                strokeWidth = 1.2f * p
            )

            // Warm golden fairy bulbs hanging at lowest point and quarter points
            val bulbPulse = (sin(timeSeconds * 3f + s * 1.5f) * 0.3f + 0.7f).coerceIn(0.4f, 1f)
            val bx = sx + swagW / 2f
            val by = topY + sag + 1.5f * p
            scope.drawCircle(Color(0xFFFFD166).copy(alpha = bulbPulse * 0.40f), 5 * p, Offset(bx, by))
            scope.drawCircle(Color(0xFFFFF3B0), 2 * p, Offset(bx, by))
            scope.drawCircle(Color.White, p, Offset(bx, by - 0.5f * p))
        }
    }

    /**
     * Refrigerator door pins: Keepsake Polaroid and Love Note pinned with cute magnets.
     */
    fun drawFridgeDecorations(
        scope: DrawScope,
        fridgeLeft: Float,
        fridgeTop: Float,
        p: Float,
        hasPolaroid: Boolean,
        hasLoveNote: Boolean
    ) {
        // Polaroid on upper door
        if (hasPolaroid) {
            val polX = fridgeLeft + 3 * p
            val polY = fridgeTop + 10 * p
            val polW = 10 * p
            val polH = 12 * p
            // White polaroid photo paper
            scope.drawRect(Color(0xFFFDFBF5), Offset(polX, polY), Size(polW, polH))
            // Inner picture area (soft coral couple silhouette)
            scope.drawRect(Color(0xFFFFCAD4), Offset(polX + 1.5f * p, polY + 1.5f * p), Size(polW - 3 * p, 7 * p))
            scope.drawRect(Color(0xFF2C2A29), Offset(polX + 3 * p, polY + 3.5f * p), Size(2 * p, 3 * p))
            scope.drawRect(Color(0xFF6B4226), Offset(polX + 5.5f * p, polY + 4f * p), Size(2 * p, 3 * p))
            scope.drawRect(Color(0xFFFF0054), Offset(polX + 4.5f * p, polY + 2f * p), Size(1.5f * p, 1.5f * p)) // mini heart
            // Turquoise round magnet pin at top
            scope.drawCircle(Color(0xFF06D6A0), 1.5f * p, Offset(polX + polW / 2f, polY + 1.2f * p))
        }

        // Folded love note envelope on lower section
        if (hasLoveNote) {
            val noteX = fridgeLeft + 4 * p
            val noteY = fridgeTop + 25 * p
            val noteW = 11 * p
            val noteH = 8 * p
            // Warm cream envelope
            scope.drawRect(Color(0xFFFFF1E6), Offset(noteX, noteY), Size(noteW, noteH))
            scope.drawRect(Color(0xFFE8D6CB), Offset(noteX, noteY + noteH - p), Size(noteW, p))
            // Envelope flap V-line
            scope.drawRect(Color(0xFFE8D6CB), Offset(noteX + 2 * p, noteY + 2 * p), Size(noteW - 4 * p, 1.2f * p))
            // Sweet red heart magnet pin
            scope.drawRect(Color(0xFFE63946), Offset(noteX + noteW / 2f - 1.5f * p, noteY - p), Size(3 * p, 2.5f * p))
        }
    }

    /**
     * Seasonal tabletop artifact (Spring sakura vase, Summer iced tea, Autumn harvest pumpkin, Winter cocoa).
     */
    fun drawSeasonalTableArtifact(
        scope: DrawScope,
        cx: Float,
        groundY: Float,
        p: Float,
        artifact: com.example.data.SeasonalArtifact,
        timeSeconds: Float
    ) {
        when (artifact) {
            com.example.data.SeasonalArtifact.SPRING_BLOSSOM_VASE -> {
                // White ceramic vase with flowering cherry twigs
                val vW = 6 * p
                val vH = 9 * p
                val vLeft = cx - vW / 2f
                val vTop = groundY - vH
                scope.drawRect(Color(0xFFF8F9FA), Offset(vLeft, vTop), Size(vW, vH))
                scope.drawRect(Color(0xFFDEE2E6), Offset(vLeft + vW - 1.5f * p, vTop), Size(1.5f * p, vH))
                // Twig branches
                scope.drawRect(Color(0xFF582F0E), Offset(cx - 0.6f * p, vTop - 7 * p), Size(1.2f * p, 7 * p))
                scope.drawRect(Color(0xFF582F0E), Offset(cx - 4 * p, vTop - 5 * p), Size(4 * p, 1.2f * p))
                scope.drawRect(Color(0xFF582F0E), Offset(cx, vTop - 6 * p), Size(4 * p, 1.2f * p))
                // Pink cherry blossoms
                scope.drawCircle(Color(0xFFFFB5C2), 2.2f * p, Offset(cx - 4 * p, vTop - 5.5f * p))
                scope.drawCircle(Color(0xFFFF758F), 1.2f * p, Offset(cx - 4 * p, vTop - 5.5f * p))
                scope.drawCircle(Color(0xFFFFB5C2), 2.2f * p, Offset(cx + 4 * p, vTop - 6.5f * p))
                scope.drawCircle(Color(0xFFFF758F), 1.2f * p, Offset(cx + 4 * p, vTop - 6.5f * p))
                scope.drawCircle(Color(0xFFFFB5C2), 2.2f * p, Offset(cx, vTop - 8.5f * p))
                // Fallen petal on table
                scope.drawRect(Color(0xFFFFB5C2), Offset(cx + 5 * p, groundY - p), Size(2 * p, 1.2f * p))
            }
            com.example.data.SeasonalArtifact.SUMMER_ICED_CARAFE -> {
                // Glass carafe with lemon slice and fresh mint
                val cW = 7 * p
                val cH = 10 * p
                val cLeft = cx - cW / 2f
                val cTop = groundY - cH
                scope.drawRect(Color(0x99BDE0FE), Offset(cLeft, cTop), Size(cW, cH))
                scope.drawRect(Color(0xFFF8F9FA), Offset(cLeft + p, cTop + p), Size(1.5f * p, cH - 2 * p)) // glass sheen
                // Lemon slice inside
                scope.drawCircle(Color(0xFFFFD166), 2 * p, Offset(cx, cTop + 5 * p))
                // Fresh mint leaf sprig poking out
                scope.drawRect(Color(0xFF52B788), Offset(cx - p, cTop - 2.5f * p), Size(2.5f * p, 3 * p))
            }
            com.example.data.SeasonalArtifact.AUTUMN_HARVEST_PUMPKIN -> {
                // Cute heirloom mini pumpkin with curved wood stem
                val pw = 10 * p
                val ph = 7 * p
                val pLeft = cx - pw / 2f
                val pTop = groundY - ph
                val pumpkinOrange = Color(0xFFF77F00)
                val pumpkinShadow = Color(0xFFD62828)
                scope.drawOval(pumpkinOrange, Offset(pLeft, pTop), Size(pw, ph))
                // Pumpkin ridges
                scope.drawOval(pumpkinShadow.copy(alpha = 0.4f), Offset(cx - 2.5f * p, pTop), Size(5 * p, ph))
                // Curly green-brown stem
                scope.drawRect(Color(0xFF582F0E), Offset(cx - 0.8f * p, pTop - 2.5f * p), Size(1.6f * p, 3 * p))
                scope.drawRect(Color(0xFF55A630), Offset(cx + 0.8f * p, pTop - 2.8f * p), Size(1.5f * p, 1.5f * p)) // tendril
            }
            com.example.data.SeasonalArtifact.WINTER_WARM_COCOA -> {
                // Two warm mugs with marshmallows and rising steam
                fun drawMug(mx: Float, mugColor: Color) {
                    val mw = 6 * p
                    val mh = 7 * p
                    val mTop = groundY - mh
                    scope.drawRect(mugColor, Offset(mx - mw / 2f, mTop), Size(mw, mh))
                    // Handle
                    scope.drawRect(mugColor, Offset(mx + mw / 2f, mTop + 1.5f * p), Size(2 * p, 4 * p))
                    // Hot cocoa surface
                    scope.drawRect(Color(0xFF582F0E), Offset(mx - mw / 2f + p, mTop + 0.5f * p), Size(mw - 2 * p, 1.5f * p))
                    // Marshmallows!
                    scope.drawCircle(Color.White, 1.2f * p, Offset(mx - p, mTop + p))
                    scope.drawCircle(Color.White, 1.2f * p, Offset(mx + p, mTop + p))
                }
                drawMug(cx - 4.5f * p, Color(0xFFC1121F)) // Festive red mug
                drawMug(cx + 4.5f * p, Color(0xFF2D6A4F)) // Cozy forest green mug
                // Gentle rising steam
                val steamSway = sin(timeSeconds * 4f) * 1.5f * p
                scope.drawCircle(Color.White.copy(alpha = 0.35f), 1.5f * p, Offset(cx - 4.5f * p + steamSway, groundY - 10 * p))
                scope.drawCircle(Color.White.copy(alpha = 0.35f), 1.5f * p, Offset(cx + 4.5f * p - steamSway, groundY - 10 * p))
            }
        }
    }

    /**
     * Woven wicker adventure picnic basket with red-and-white gingham cloth peeking out.
     * Unlocked by completing 1+ Tiny Date Adventures.
     */
    fun drawAdventurePicnicBasket(scope: DrawScope, cx: Float, groundY: Float, p: Float) {
        val bw = 16 * p
        val bh = 11 * p
        val bLeft = cx - bw / 2f
        val bTop = groundY - bh

        // Ground shadow
        scope.drawOval(
            Color.Black.copy(alpha = 0.22f),
            Offset(bLeft - p, groundY - 2.5f * p),
            Size(bw + 2 * p, 4 * p)
        )

        // Basket body (woven golden wicker)
        val wickerBase = Color(0xFFC48B47)
        val wickerDark = Color(0xFF9E652A)
        val wickerLight = Color(0xFFE2B06F)
        scope.drawRect(wickerBase, Offset(bLeft, bTop + 3 * p), Size(bw, bh - 3 * p))

        // Weave cross-hatch accents
        for (i in 0..3) {
            val yOffset = bTop + (4 + i * 2) * p
            scope.drawRect(wickerDark, Offset(bLeft, yOffset), Size(bw, p))
            for (j in 0..3) {
                val xOffset = bLeft + (1 + j * 4 + (i % 2) * 2) * p
                scope.drawRect(wickerLight, Offset(xOffset, yOffset - p), Size(1.5f * p, p))
            }
        }

        // Overhanging red-and-white gingham picnic cloth
        val ginghamRed = Color(0xFFE63946)
        val ginghamLight = Color(0xFFFFA8B0)
        scope.drawRect(ginghamRed, Offset(bLeft + 2 * p, bTop + 2.5f * p), Size(5 * p, 4 * p))
        scope.drawRect(Color.White, Offset(bLeft + 3 * p, bTop + 3 * p), Size(2 * p, 2 * p))
        scope.drawRect(ginghamLight, Offset(bLeft + 2 * p, bTop + 4.5f * p), Size(2 * p, 2 * p))

        // Basket lid & rim
        scope.drawRect(wickerDark, Offset(bLeft - p, bTop + 2 * p), Size(bw + 2 * p, 2 * p))
        scope.drawRect(wickerLight, Offset(bLeft, bTop + 1.5f * p), Size(bw, p))

        // Wicker arch handle
        scope.drawRect(wickerDark, Offset(cx - 5 * p, bTop - 4 * p), Size(p, 6 * p))
        scope.drawRect(wickerDark, Offset(cx + 4 * p, bTop - 4 * p), Size(p, 6 * p))
        scope.drawRect(wickerLight, Offset(cx - 5 * p, bTop - 5 * p), Size(10 * p, 1.5f * p))
    }

    /**
     * Bedside desk notepad with pencil and folded paper corner.
     * Unlocked by completing 1+ Daily Tiny Moments.
     */
    fun drawBedsideNotepad(scope: DrawScope, cx: Float, groundY: Float, p: Float) {
        val nw = 11 * p
        val nh = 8 * p
        val nLeft = cx - nw / 2f
        val nTop = groundY - nh

        // Ground shadow
        scope.drawOval(
            Color.Black.copy(alpha = 0.18f),
            Offset(nLeft - p, groundY - 2 * p),
            Size(nw + 2 * p, 3 * p)
        )

        // Notepad paper backing
        val paperCream = Color(0xFFFFFDF5)
        val paperShadow = Color(0xFFE8E2D5)
        val lineBlue = Color(0xFFB0C4DE).copy(alpha = 0.6f)
        scope.drawRect(paperShadow, Offset(nLeft + 0.5f * p, nTop + 0.5f * p), Size(nw, nh))
        scope.drawRect(paperCream, Offset(nLeft, nTop), Size(nw, nh))

        // Lined pages
        scope.drawRect(lineBlue, Offset(nLeft + 2 * p, nTop + 2.5f * p), Size(nw - 4 * p, 0.8f * p))
        scope.drawRect(lineBlue, Offset(nLeft + 2 * p, nTop + 4.5f * p), Size(nw - 4 * p, 0.8f * p))

        // Top binder strip (warm leather / coral tape)
        scope.drawRect(Color(0xFFE07A5F), Offset(nLeft, nTop), Size(nw, 1.5f * p))

        // Little yellow wooden pencil alongside
        val pencilYellow = Color(0xFFFFB703)
        val pencilLead = Color(0xFF2B2D42)
        val px = nLeft + nw + 1.5f * p
        scope.drawRect(pencilYellow, Offset(px, nTop + p), Size(1.5f * p, 6 * p))
        scope.drawRect(Color(0xFFE9C46A), Offset(px, nTop + 7 * p), Size(1.5f * p, 1.2f * p))
        scope.drawRect(pencilLead, Offset(px + 0.2f * p, nTop + 8.2f * p), Size(1.1f * p, p))
    }

    /**
     * Handcrafted wooden mini-game board box on table / shelf.
     * Unlocked by completing 1+ Two-Person Mini-Games.
     */
    fun drawMiniGameBoard(scope: DrawScope, cx: Float, groundY: Float, p: Float) {
        val gw = 14 * p
        val gh = 6 * p
        val gLeft = cx - gw / 2f
        val gTop = groundY - gh

        // Soft drop shadow
        scope.drawOval(
            Color.Black.copy(alpha = 0.22f),
            Offset(gLeft - p, groundY - 2 * p),
            Size(gw + 2 * p, 3.5f * p)
        )

        // Wood game box (warm mahogany)
        val woodDark = Color(0xFF6B4226)
        val woodMid = Color(0xFF8B5A2B)
        val woodLight = Color(0xFFA0522D)
        val brassGold = Color(0xFFFFD166)

        scope.drawRect(woodDark, Offset(gLeft, gTop), Size(gw, gh))
        scope.drawRect(woodMid, Offset(gLeft + p, gTop + p), Size(gw - 2 * p, gh - 2 * p))

        // Checkerboard inlay on top
        val checkDark = Color(0xFF3D2314)
        val checkLight = Color(0xFFDEB887)
        for (row in 0..1) {
            for (col in 0..3) {
                val color = if ((row + col) % 2 == 0) checkLight else checkDark
                scope.drawRect(color, Offset(gLeft + 2 * p + col * 2.5f * p, gTop + 1.2f * p + row * 1.8f * p), Size(2.2f * p, 1.5f * p))
            }
        }

        // Brass corner braces and center latch
        scope.drawRect(brassGold, Offset(gLeft, gTop), Size(1.5f * p, 1.5f * p))
        scope.drawRect(brassGold, Offset(gLeft + gw - 1.5f * p, gTop), Size(1.5f * p, 1.5f * p))
        scope.drawRect(brassGold, Offset(cx - p, gTop + gh - 2 * p), Size(2 * p, 1.8f * p))
    }

    /**
     * Delicate folded origami heart on windowsill / desk.
     * Unlocked by sending or receiving a Long-Distance signal.
     */
    fun drawOrigamiHeart(scope: DrawScope, cx: Float, groundY: Float, p: Float) {
        val hw = 9 * p
        val hh = 8 * p
        val hLeft = cx - hw / 2f
        val hTop = groundY - hh

        // Contact drop shadow
        scope.drawOval(
            Color.Black.copy(alpha = 0.16f),
            Offset(hLeft, groundY - 1.5f * p),
            Size(hw, 2.5f * p)
        )

        // Crisp folded origami facet colors (Japanese washi paper)
        val facetMain = Color(0xFFFF4D6D)
        val facetLight = Color(0xFFFF758F)
        val facetDeep = Color(0xFFC9184A)
        val facetFold = Color(0xFF800F2F).copy(alpha = 0.5f)

        // Left lobe
        scope.drawRect(facetLight, Offset(hLeft + p, hTop), Size(3.5f * p, 3.5f * p))
        // Right lobe
        scope.drawRect(facetMain, Offset(hLeft + 4.5f * p, hTop), Size(3.5f * p, 3.5f * p))

        // Center fold triangle body
        scope.drawRect(facetMain, Offset(hLeft + 1.5f * p, hTop + 2.5f * p), Size(6 * p, 3 * p))
        scope.drawRect(facetDeep, Offset(hLeft + 2.5f * p, hTop + 5 * p), Size(4 * p, 2 * p))
        scope.drawRect(facetDeep, Offset(hLeft + 3.5f * p, hTop + 6.8f * p), Size(2 * p, 1.2f * p))

        // Origami crease fold line down the center
        scope.drawRect(facetFold, Offset(cx - 0.4f * p, hTop + 1.5f * p), Size(0.8f * p, 5 * p))
    }

    fun drawAromatherapyCandle(
        scope: DrawScope,
        cx: Float,
        groundY: Float,
        p: Float,
        isLit: Boolean,
        timeSeconds: Float
    ) {
        val cw = 8 * p
        val ch = 9 * p
        val left = cx - cw / 2f
        val top = groundY - ch

        // Soft drop shadow
        scope.drawOval(
            Color.Black.copy(alpha = 0.18f),
            Offset(left - p, groundY - 1.5f * p),
            Size(cw + 2 * p, 2.5f * p)
        )

        // Ceramic jar body (warm terracotta/cream)
        scope.drawRect(Color(0xFFE8DCC4), Offset(left, top + 2 * p), Size(cw, ch - 2 * p))
        scope.drawRect(Color(0xFFD4C4A8), Offset(left + cw - 1.5f * p, top + 2 * p), Size(1.5f * p, ch - 2 * p))
        scope.drawRect(Color(0xFFFAF6EE), Offset(left, top + 2 * p), Size(1.2f * p, ch - 2 * p))

        // Soy wax surface
        scope.drawOval(Color(0xFFFFF9EE), Offset(left + 0.5f * p, top + 1.2f * p), Size(cw - p, 2.2f * p))

        // Dark wick
        scope.drawRect(Color(0xFF2B2B2B), Offset(cx - 0.4f * p, top + 0.4f * p), Size(0.8f * p, 1.8f * p))

        if (isLit) {
            val flicker = kotlin.math.sin(timeSeconds * 12f) * 0.4f * p
            val flameH = 4.5f * p + flicker

            // Warm halo glow
            val glowRadius = 14f * p + flicker * 2f
            scope.drawOval(
                Color(0xFFFFD166).copy(alpha = 0.22f),
                Offset(cx - glowRadius, top - flameH * 0.5f - glowRadius),
                Size(glowRadius * 2, glowRadius * 2)
            )

            // Outer flame (warm amber)
            scope.drawOval(
                Color(0xFFFFAA00),
                Offset(cx - 1.6f * p, top - flameH),
                Size(3.2f * p, flameH)
            )
            // Inner core flame (bright white-yellow)
            scope.drawOval(
                Color(0xFFFFFDF0),
                Offset(cx - 0.9f * p, top - flameH + 1.2f * p),
                Size(1.8f * p, flameH - 1.5f * p)
            )
        }
    }

    fun drawPorchWindChimes(
        scope: DrawScope,
        cx: Float,
        topY: Float,
        p: Float,
        swayProgress: Float,
        timeSeconds: Float
    ) {
        val naturalBreeze = kotlin.math.sin(timeSeconds * 2.2f) * 1.5f
        val activeSway = if (swayProgress > 0f) kotlin.math.sin(swayProgress * 10f) * 6f else 0f
        val totalSway = (naturalBreeze + activeSway) * p

        // Top hanger hook & thread
        scope.drawRect(Color(0xFF7F5539), Offset(cx - 0.5f * p, topY), Size(1f * p, 4 * p))

        // Top wooden disk
        val diskW = 14 * p
        val diskLeft = cx - diskW / 2f
        scope.drawRect(Color(0xFF9C6644), Offset(diskLeft, topY + 4 * p), Size(diskW, 2.5f * p))
        scope.drawRect(Color(0xFFB08968), Offset(diskLeft, topY + 4 * p), Size(diskW, 0.8f * p))

        // 4 Chime pipes with varied lengths and sway
        val pipeColor = Color(0xFFC5BAAF)
        val pipeHighlight = Color(0xFFEBE6E0)
        val pipeShadow = Color(0xFF9E9285)

        val lengths = floatArrayOf(12f, 16f, 14f, 10f)
        val offsets = floatArrayOf(-4.5f, -1.5f, 1.5f, 4.5f)

        for (i in 0 until 4) {
            val px = cx + offsets[i] * p + totalSway * (0.6f + i * 0.1f)
            val py = topY + 7.5f * p
            val ph = lengths[i] * p
            scope.drawRect(pipeColor, Offset(px - p, py), Size(2 * p, ph))
            scope.drawRect(pipeHighlight, Offset(px - p, py), Size(0.6f * p, ph))
            scope.drawRect(pipeShadow, Offset(px + 0.4f * p, py), Size(0.6f * p, ph))
        }

        // Center thread, wooden clapper, and sail
        val sailY = topY + 28 * p
        val sailX = cx + totalSway * 1.4f
        scope.drawRect(Color(0xFFB08968), Offset(cx - 0.3f * p + totalSway * 0.8f, topY + 14 * p), Size(0.6f * p, 8 * p))
        // Crystal / wooden wind sail
        scope.drawRect(Color(0xFFFFB5C2), Offset(sailX - 2.5f * p, sailY), Size(5 * p, 7 * p))
        scope.drawRect(Color(0xFFFF758F), Offset(sailX - 1.5f * p, sailY + 1 * p), Size(3 * p, 5 * p))
    }

    fun drawFeatherWand(
        scope: DrawScope,
        baseX: Float,
        baseY: Float,
        p: Float,
        wiggleProgress: Float,
        timeSeconds: Float
    ) {
        val wiggleAngle = if (wiggleProgress > 0f) kotlin.math.sin(wiggleProgress * 12f) * 8f * p else kotlin.math.sin(timeSeconds * 1.8f) * 1.2f * p

        // Natural slender wooden stick (tilted ~40 degrees)
        val tipX = baseX + 14 * p + wiggleAngle
        val tipY = baseY - 22 * p

        scope.drawLine(
            Color(0xFFB08968),
            Offset(baseX, baseY),
            Offset(tipX, tipY),
            strokeWidth = 1.8f * p
        )

        // Hanging string
        val cordEnd = Offset(tipX + wiggleAngle * 1.2f, tipY + 10 * p)
        scope.drawLine(
            Color(0xFFE2D6CA),
            Offset(tipX, tipY),
            cordEnd,
            strokeWidth = 0.8f * p
        )

        // 3 playful pastel feathers (Rose, Sky, Mint)
        val feather1 = Color(0xFFFF758F)
        val feather2 = Color(0xFF48CAE4)
        val feather3 = Color(0xFF80ED99)

        scope.drawOval(feather1, Offset(cordEnd.x - 2.5f * p, cordEnd.y), Size(5 * p, 7 * p))
        scope.drawOval(feather2, Offset(cordEnd.x - 4 * p, cordEnd.y + 2 * p), Size(4 * p, 6 * p))
        scope.drawOval(feather3, Offset(cordEnd.x + 0.5f * p, cordEnd.y + 1.5f * p), Size(4 * p, 6.5f * p))
    }

    fun drawVintageTelescope(
        scope: DrawScope,
        cx: Float,
        groundY: Float,
        p: Float,
        timeSeconds: Float
    ) {
        val tripodH = 26 * p
        val topY = groundY - tripodH
        val legSpread = 13 * p

        // Contact drop shadow
        scope.drawOval(
            Color.Black.copy(alpha = 0.22f),
            Offset(cx - legSpread - 2 * p, groundY - 2 * p),
            Size(legSpread * 2 + 4 * p, 3.5f * p)
        )

        // Tripod legs (rich mahogany wood with brass tips)
        val woodLeg = Color(0xFF5E3023)
        val brass = Color(0xFFE0A96D)
        val brassHighlight = Color(0xFFF7D070)

        scope.drawLine(woodLeg, Offset(cx, topY), Offset(cx - legSpread, groundY), strokeWidth = 2.2f * p)
        scope.drawLine(woodLeg, Offset(cx, topY), Offset(cx + legSpread, groundY), strokeWidth = 2.2f * p)
        scope.drawLine(woodLeg, Offset(cx, topY), Offset(cx, groundY + p), strokeWidth = 2f * p)

        // Brass mount & pivot ring
        scope.drawOval(brass, Offset(cx - 3 * p, topY - 2 * p), Size(6 * p, 4 * p))

        // Brass telescope tube angled upward at ~32 degrees
        val barrelLen = 22 * p
        val tiltAngle = -0.48f // radians (~28 deg upwards)
        val barrelEndX = cx + kotlin.math.cos(tiltAngle) * barrelLen
        val barrelEndY = topY + kotlin.math.sin(tiltAngle) * barrelLen
        val eyepieceX = cx - kotlin.math.cos(tiltAngle) * 8 * p
        val eyepieceY = topY - kotlin.math.sin(tiltAngle) * 8 * p

        // Main barrel
        scope.drawLine(brass, Offset(eyepieceX, eyepieceY), Offset(barrelEndX, barrelEndY), strokeWidth = 4.2f * p)
        scope.drawLine(brassHighlight, Offset(eyepieceX, eyepieceY - 0.8f * p), Offset(barrelEndX, barrelEndY - 0.8f * p), strokeWidth = 1.2f * p)

        // Front lens hood
        scope.drawRect(Color(0xFFC68B59), Offset(barrelEndX - p, barrelEndY - 2.5f * p), Size(3.5f * p, 5f * p))
        // Glass lens glint
        val glintAlpha = 0.6f + kotlin.math.sin(timeSeconds * 3f) * 0.3f
        scope.drawRect(Color(0xFFBCE7FD).copy(alpha = glintAlpha), Offset(barrelEndX + 1.5f * p, barrelEndY - 2 * p), Size(1.2f * p, 4f * p))
    }
}

