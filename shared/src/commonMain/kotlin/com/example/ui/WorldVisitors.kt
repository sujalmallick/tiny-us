package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.data.AvatarAppearance
import com.example.data.Painting
import com.example.engine.AvatarLook
import com.example.engine.CharacterPose
import com.example.engine.Direction
import com.example.engine.HeldItem
import com.example.engine.PixelArtRenderer
import com.example.engine.PixelCharacter
import com.example.engine.WorldViewport
import com.example.scene.OldCoupleVisit
import com.example.scene.PainterVisit
import com.example.scene.SceneEngine
import kotlin.math.sin

/*
 * Plan 11: the visitors in the world. The street painter at an easel; Billionaire and The Great on
 * their bench on the pier; and the painting itself, drawn from the moment it was finished.
 */

private val PAINTER_LOOK = AvatarLook.of(AvatarAppearance(skinTone = 3, hairColor = 4, longHair = false, wearsDress = false))
/**
 * Billionaire and The Great have the main girl's skin tone (the user's wish), silver hair, and their
 * own faces and clothes drawn over the usual sprite, so they never look like the two of them.
 */
private fun oldLook(girl: AvatarLook, her: Boolean): AvatarLook =
    if (her) AvatarLook.of(girl.appearance.copy(hairColor = 5, longHair = true, wearsDress = true))
    // Billionaire is fair-skinned (the user's wish)
    else AvatarLook.of(AvatarAppearance(skinTone = 0, hairColor = 5, longHair = false, wearsDress = false))

private fun person(look: AvatarLook, girl: Boolean, pose: CharacterPose, facing: Direction, outfit: Int) =
    PixelCharacter(isGirl = girl, name = "", worldX = 0f, worldY = 0f, pose = pose, direction = facing).apply {
        this.look = look
        outfitIndex = outfit
    }

fun drawVisitors(scope: DrawScope, engine: SceneEngine, cw: Float, ch: Float, p: Float) {
    val charP = WorldViewport.characterPixelScale(cw, engine.usesLowResRenderer)
    if (engine.painter.active) drawPainterVisit(scope, engine, cw, ch, p, charP)
    if (engine.oldCouple.active || engine.oldCouple.noteWaiting) drawOldCouple(scope, engine, cw, ch, p, charP)
}

private fun drawPainterVisit(scope: DrawScope, engine: SceneEngine, cw: Float, ch: Float, p: Float, charP: Float) {
    val v = engine.painter
    val ground = ch * v.y
    if (v.easelUp) {
        val ex = cw * v.easelX
        // The easel: three legs and a canvas
        val wood = Color(0xFF8B5A2B)
        scope.drawRect(wood, Offset(ex - 6f * p, ground - 22f * p), Size(p, 22f * p))
        scope.drawRect(wood, Offset(ex + 5f * p, ground - 22f * p), Size(p, 22f * p))
        scope.drawRect(wood, Offset(ex - 0.5f * p, ground - 26f * p), Size(p, 18f * p))
        val cl = ex - 9f * p
        val ct = ground - 30f * p
        val cwid = 18f * p
        val chgt = 13f * p
        if (v.showing) {
            engine.visitorStore?.paintings()?.lastOrNull()?.let {
                drawPainting(scope, it, cl, ct, cwid, chgt, engine.boy.look, engine.girl.look)
            }
        } else {
            scope.drawRect(Color(0xFF6B4226), Offset(cl - p, ct - p), Size(cwid + 2f * p, chgt + 2f * p))
            scope.drawRect(Color(0xFFFFF8E7), Offset(cl, ct), Size(cwid, chgt))
            // Dabs of paint appear as it goes
            val dabs = (v.progress * 26).toInt()
            val colors = listOf(Color(0xFF8ECAE6), Color(0xFF6BBF59), Color(0xFFE88AA8), Color(0xFFFFD166), Color(0xFF6A381F))
            for (i in 0 until dabs) {
                val dx = ((i * 37) % 16) + 1f
                val dy = ((i * 23) % 11) + 1f
                scope.drawRect(colors[i % colors.size], Offset(cl + dx * p, ct + dy * p), Size(p, p))
            }
        }
    }
    // The painter: a beret, a smock, looking past the canvas at them
    val walking = !v.easelUp
    val step = ((v.age * 6f).toInt() % 4)
    val pose = if (walking) listOf(CharacterPose.WALK_1, CharacterPose.WALK_2, CharacterPose.WALK_3, CharacterPose.WALK_4)[step] else CharacterPose.IDLE
    val painter = person(PAINTER_LOOK, false, pose, if (v.facingLeft) Direction.LEFT else Direction.RIGHT, 5)
    val px = cw * v.x
    PixelArtRenderer.drawCharacter(scope, painter, px, ground, pixelSize = charP)
    val c = charP * 1.1f
    val top = ground - 26f * c
    // A beret, worn at a tilt
    val back = if (v.facingLeft) 1f else -1f
    scope.drawRect(Color(0xFFC1121F), Offset(px - 4f * c + back * c, top + 1.2f * c), Size(8f * c, 1.8f * c))
    scope.drawRect(Color(0xFF9B0E19), Offset(px - 4f * c + back * c, top + 2.6f * c), Size(8f * c, 0.5f * c))
    scope.drawRect(Color(0xFFC1121F), Offset(px - 0.5f * c + back * 2f * c, top + 0.4f * c), Size(c, 0.8f * c))
    if (v.easelUp && !v.showing) {
        // A brush, dabbing
        val dab = sin(v.age * 6f) * c
        val bx = px - 9f * c
        scope.drawRect(Color(0xFF6B4226), Offset(bx, top + 13f * c + dab), Size(3f * c, 0.8f * c))
        scope.drawRect(Color(0xFFE88AA8), Offset(bx - c, top + 13f * c + dab), Size(c, 0.8f * c))
    }
}

private fun drawOldCouple(scope: DrawScope, engine: SceneEngine, cw: Float, ch: Float, p: Float, charP: Float) {
    val v = engine.oldCouple
    val bx = cw * OldCoupleVisit.BENCH_X
    val ground = ch * OldCoupleVisit.BENCH_Y
    val c = charP * 1.1f
    // Their bench
    val wood = Color(0xFF6B4226)
    scope.drawRect(wood, Offset(bx - 16f * c, ground - 8f * c), Size(32f * c, 2f * c))
    scope.drawRect(Color(0xFF5C3A24), Offset(bx - 16f * c, ground - 15f * c), Size(32f * c, 2f * c))
    scope.drawRect(Color(0xFF4A2F1C), Offset(bx - 14f * c, ground - 6f * c), Size(1.5f * c, 6f * c))
    scope.drawRect(Color(0xFF4A2F1C), Offset(bx + 12.5f * c, ground - 6f * c), Size(1.5f * c, 6f * c))
    if (v.noteWaiting && (!v.active || v.phase == OldCoupleVisit.Phase.LEAVING)) {
        // A note under a pebble, where they sat
        scope.drawRect(Color(0xFFFFF8E7), Offset(bx - 3f * c, ground - 10f * c), Size(6f * c, 2f * c))
        scope.drawRect(Color(0xFFE88AA8), Offset(bx - 0.5f * c, ground - 9.5f * c), Size(c, c))
        scope.drawRect(Color(0xFF8D99AE), Offset(bx + 1f * c, ground - 11f * c), Size(2.5f * c, 1.5f * c))
    }
    if (!v.active) return
    val sitting = v.phase == OldCoupleVisit.Phase.SITTING
    val mood = v.mood
    val gap = when (mood) {
        OldCoupleVisit.Mood.HUG -> 4.5f * c
        OldCoupleVisit.Mood.SNUGGLE, OldCoupleVisit.Mood.HOLD_HANDS -> 6f * c
        else -> 8f * c
    }
    val manX: Float
    val womanX: Float
    val manPose: CharacterPose
    val womanPose: CharacterPose
    if (sitting) {
        manX = bx - gap
        womanX = bx + gap
        // They stay on their bench: holding hands is two hands joined between them
        manPose = if (mood == OldCoupleVisit.Mood.HUG) CharacterPose.HUG else CharacterPose.SIT
        womanPose = when (mood) {
            OldCoupleVisit.Mood.HUG -> CharacterPose.HUG
            OldCoupleVisit.Mood.SNUGGLE -> CharacterPose.SIT_SNUGGLE
            else -> CharacterPose.SIT
        }
    } else {
        // Strolling arm in arm, slowly
        manX = cw * v.x - 5f * c
        womanX = cw * v.x + 5f * c
        val step = ((v.age * 3f).toInt() % 4)
        val walk = listOf(CharacterPose.WALK_1, CharacterPose.WALK_2, CharacterPose.WALK_3, CharacterPose.WALK_4)[step]
        manPose = walk
        womanPose = walk
    }
    val leaving = v.phase == OldCoupleVisit.Phase.LEAVING
    val man = person(oldLook(engine.girl.look, her = false), false, manPose, if (leaving) Direction.LEFT else Direction.RIGHT, 6)
    val womanFacing = if (leaving || sitting) Direction.LEFT else Direction.RIGHT
    val woman = person(oldLook(engine.girl.look, her = true), true, womanPose, womanFacing, 4)
    if (sitting && mood == OldCoupleVisit.Mood.SNACK) {
        // Sharing a paper cone of chips, one each
        man.hold(HeldItem.MUG, 10f); man.heldItemAge = v.age
        woman.hold(HeldItem.MUG, 10f); woman.heldItemAge = v.age + 0.7f
    }
    PixelArtRenderer.drawCharacter(scope, man, manX, ground, pixelSize = charP)
    PixelArtRenderer.drawCharacter(scope, woman, womanX, ground, pixelSize = charP)
    if (manPose != CharacterPose.HUG) {
        drawOldOutfit(scope, man, manX, ground, c, her = false)
        drawOldFace(scope, man, manX, ground, c, her = false)
    }
    if (womanPose != CharacterPose.HUG) {
        drawOldOutfit(scope, woman, womanX, ground, c, her = true)
        drawOldFace(scope, woman, womanX, ground, c, her = true)
    }
    // His flat cap (a joke on his name), her big round glasses and her shawl
    val manSits = manPose == CharacterPose.SIT
    val capTop = ground - 26f * c + (if (manSits) 6f * c else 0f)
    val facingRight = man.direction == Direction.RIGHT
    scope.drawRect(Color(0xFF6F5E4B), Offset(manX - 4.5f * c, capTop + 1.5f * c), Size(9f * c, 2f * c))
    scope.drawRect(Color(0xFF8A7760), Offset(manX - 3.5f * c, capTop + 1.5f * c), Size(4f * c, 0.7f * c))
    scope.drawRect(Color(0xFF4A3F33), Offset(if (facingRight) manX + 3.5f * c else manX - 7.5f * c, capTop + 3f * c), Size(4f * c, c))
    val herSits = womanPose == CharacterPose.SIT || womanPose == CharacterPose.SIT_SNUGGLE
    if (sitting && mood == OldCoupleVisit.Mood.HOLD_HANDS) {
        // Hands joined on the bench between them
        scope.drawRect(Color(0xFFFFDFBA), Offset(bx - 2f * c, ground - 9f * c), Size(4f * c, 1.6f * c))
        scope.drawRect(Color(0xFFF1C27D), Offset(bx - 0.5f * c, ground - 9f * c), Size(c, 1.6f * c))
    }
    val shawlTop = ground - 14f * c + (if (herSits) 6f * c else 0f)
    scope.drawRect(Color(0xFFF3E9D2), Offset(womanX - 5f * c, shawlTop), Size(10f * c, 2.5f * c))
    scope.drawRect(Color(0xFFDCCBA8), Offset(womanX - 4f * c, shawlTop + 2.5f * c), Size(2f * c, c))
    scope.drawRect(Color(0xFFDCCBA8), Offset(womanX + 2f * c, shawlTop + 2.5f * c), Size(2f * c, c))
}

/**
 * A painting of the two of them (plan 11): the light and weather of that moment, the ground of that
 * place, the two of them as they were (in their outfits and looks), and Mochi if she was there.
 */
fun drawPainting(
    scope: DrawScope,
    painting: Painting,
    left: Float,
    top: Float,
    w: Float,
    h: Float,
    boyLook: AvatarLook,
    girlLook: AvatarLook
) {
    val f = (w / 40f).coerceAtLeast(1f)
    // The frame
    scope.drawRect(Color(0xFF8B5A2B), Offset(left - f * 1.5f, top - f * 1.5f), Size(w + 3f * f, h + 3f * f))
    scope.drawRect(Color(0xFFE9C46A), Offset(left - f * 0.5f, top - f * 0.5f), Size(w + f, h + f))
    // Sky, by the light
    val (skyTop, skyLow) = when (painting.phase) {
        "MORNING" -> Color(0xFFBDE0FE) to Color(0xFFFFD6A5)
        "SUNSET" -> Color(0xFFFF9F68) to Color(0xFFFFD6A5)
        "NIGHT" -> Color(0xFF1D3557) to Color(0xFF457B9D)
        else -> Color(0xFF8ECAE6) to Color(0xFFCAF0F8)
    }
    val horizon = top + h * 0.62f
    val bands = 6
    for (i in 0 until bands) {
        val t = i / (bands - 1f)
        val c = Color(
            red = skyTop.red + (skyLow.red - skyTop.red) * t,
            green = skyTop.green + (skyLow.green - skyTop.green) * t,
            blue = skyTop.blue + (skyLow.blue - skyTop.blue) * t
        )
        scope.drawRect(c, Offset(left, top + (horizon - top) * i / bands), Size(w, (horizon - top) / bands + 1f))
    }
    if (painting.phase == "NIGHT") {
        for (i in 0 until 7) scope.drawRect(Color(0xFFFFF3B0), Offset(left + ((i * 53) % 37) / 37f * w, top + ((i * 29) % 13) / 13f * (horizon - top) * 0.7f), Size(f * 0.6f, f * 0.6f))
    } else {
        scope.drawCircle(if (painting.phase == "SUNSET") Color(0xFFFFB703) else Color(0xFFFFE066), f * 2.5f, Offset(left + w * 0.8f, top + h * 0.18f))
    }
    // The ground of that place
    val ground = when (painting.scene) {
        "WALK", "MOMO_STALL" -> Color(0xFF9A9AA0)
        "SEASIDE_PIER" -> Color(0xFFB07D52)
        "COOKING", "SLEEP", "RAINY_CAFE", "COZY_LOFT", "SUNROOM" -> Color(0xFFD9A06B)
        else -> Color(0xFF6BBF59)
    }
    scope.drawRect(ground, Offset(left, horizon), Size(w, top + h - horizon))
    if (painting.scene == "FLOWER" || painting.scene == "UNDER_TREE" || painting.scene == "LOOKING") {
        for (i in 0 until 9) scope.drawRect(listOf(Color(0xFFFF8FAB), Color(0xFFFFD166), Color(0xFFFFFFFF))[i % 3], Offset(left + ((i * 41) % 39) / 39f * w, horizon + ((i * 17) % 7 + 1) / 9f * (top + h - horizon)), Size(f * 0.7f, f * 0.7f))
    }
    // Weather
    when (painting.weather) {
        "RAIN" -> for (i in 0 until 16) scope.drawRect(Color(0x99FFFFFF), Offset(left + ((i * 31) % 40) / 40f * w, top + ((i * 13) % 15) / 15f * h * 0.9f), Size(f * 0.3f, f * 1.6f))
        "SNOW" -> for (i in 0 until 16) scope.drawRect(Color(0xEEFFFFFF), Offset(left + ((i * 31) % 40) / 40f * w, top + ((i * 13) % 15) / 15f * h * 0.9f), Size(f * 0.6f, f * 0.6f))
        "SAKURA" -> for (i in 0 until 12) scope.drawRect(Color(0xFFFFB5C2), Offset(left + ((i * 31) % 40) / 40f * w, top + ((i * 13) % 15) / 15f * h * 0.9f), Size(f * 0.7f, f * 0.5f))
        "AUTUMN" -> for (i in 0 until 12) scope.drawRect(Color(0xFFE76F51), Offset(left + ((i * 31) % 40) / 40f * w, top + ((i * 13) % 15) / 15f * h * 0.9f), Size(f * 0.7f, f * 0.6f))
    }
    // The two of them, as they were
    val cp = h * 0.62f / 26f / 1.1f
    val feet = top + h * 0.94f
    val (boyPose, girlPose) = when (painting.together) {
        "HOLD_HANDS" -> CharacterPose.HOLD_HANDS to CharacterPose.HOLD_HANDS
        "HUG" -> CharacterPose.HUG to CharacterPose.HUG
        "SNUGGLE" -> CharacterPose.SIT to CharacterPose.SIT_SNUGGLE
        else -> CharacterPose.IDLE to CharacterPose.IDLE
    }
    val close = if (painting.together == "APART") 9f else 6f
    val boy = person(boyLook, false, boyPose, Direction.RIGHT, painting.boyOutfit)
    val girl = person(girlLook, true, girlPose, Direction.LEFT, painting.girlOutfit)
    PixelArtRenderer.drawCharacter(scope, boy, left + w * 0.45f - close * cp, feet, pixelSize = cp)
    PixelArtRenderer.drawCharacter(scope, girl, left + w * 0.45f + close * cp, feet, pixelSize = cp)
    if (painting.mochi) {
        com.example.engine.WorldSprites.drawCat(scope, left + w * 0.78f, feet, cp * 0.9f, catState = com.example.scene.CatState.SITTING_PURR, isSleeping = false)
    }
    // A painter's touch: soft dabs of light across it
    for (i in 0 until 10) {
        val dx = ((painting.epochDay + i * 37) % 40) / 40f * w
        val dy = ((painting.epochDay + i * 23) % 30) / 30f * h
        scope.drawRect(Color(0x22FFFFFF), Offset(left + dx, top + dy), Size(f * 2f, f))
    }
}

/**
 * Their own clothes (plan 11), painted over the sprite so they never look like the two of them:
 * his navy three-piece suit with a red bow tie and a gold watch chain; her long plum dress with
 * pearls and a flower brooch.
 */
private fun drawOldOutfit(scope: DrawScope, who: PixelCharacter, cx: Float, ground: Float, c: Float, her: Boolean) {
    val sitting = who.pose == CharacterPose.SIT || who.pose == CharacterPose.SIT_SNUGGLE
    val top = ground - 26f * c + (if (sitting) 5f * c else 0f)
    val flip = who.direction == Direction.LEFT
    val left = cx - 9f * c
    fun px(x: Int, y: Int, w: Int, h: Int, col: Color) {
        val gx = if (flip) 17 - (x + w - 1) else x
        scope.drawRect(col, Offset(left + gx * c, top + y * c), Size(w * c, h * c))
    }
    // Torso and arms, sitting or standing, on the sprite's grid
    val torsoTop = if (sitting) 13 else 12
    val torsoRows = if (sitting) 4 else 6
    if (!her) {
        val navy = Color(0xFF1F2A44)
        val navyLight = Color(0xFF2E3D5C)
        val trousers = Color(0xFF2B2B33)
        px(6, torsoTop, 7, torsoRows, navy)
        if (sitting) { px(5, 14, 2, 3, navy); px(11, 14, 2, 3, navy) } else { px(4, 13, 2, 4, navy); px(12, 13, 2, 4, navy) }
        // White shirt in a V, red bow tie, a pocket square, the watch chain
        px(8, torsoTop, 3, 2, Color(0xFFF8F9FA))
        px(9, torsoTop + 2, 1, 1, Color(0xFFF8F9FA))
        px(8, torsoTop, 1, 1, Color(0xFFC1121F)); px(10, torsoTop, 1, 1, Color(0xFFC1121F)); px(9, torsoTop, 1, 1, Color(0xFF9B0E19))
        px(11, torsoTop + 1, 1, 1, Color(0xFFE88AA8))
        px(7, torsoTop + 3, 3, 1, Color(0xFFE9C46A))
        px(6, torsoTop, 1, torsoRows, navyLight)
        if (sitting) px(5, 17, 9, 2, trousers) else px(6, 18, 7, 5, trousers)
    } else {
        val plum = Color(0xFF6D2E46)
        val plumLight = Color(0xFF8E3B5C)
        px(6, torsoTop, 7, torsoRows, plum)
        if (sitting) { px(5, 14, 2, 3, plum); px(11, 14, 2, 3, plum) } else { px(4, 13, 2, 4, plum); px(12, 13, 2, 4, plum) }
        // A long dress down to her shoes
        if (sitting) px(5, 17, 9, 2, plum) else px(5, 18, 9, 6, plum)
        px(6, torsoTop + 1, 1, torsoRows - 1, plumLight)
        // Pearls and a little flower brooch
        for (i in 0 until 4) px(7 + i, torsoTop, 1, 1, if (i % 2 == 0) Color(0xFFFFFFFF) else Color(0xFFE9ECEF))
        px(11, torsoTop + 1, 1, 1, Color(0xFFFFD166))
    }
}

/**
 * Their own faces and clothes (plan 11), over the usual sprite on its 18 x 26 grid: his crinkly
 * smiling eyes, bushy white brows and moustache; her grey bun, soft eyes, rosy cheeks and a touch
 * of lipstick.
 */
private fun drawOldFace(scope: DrawScope, who: PixelCharacter, cx: Float, ground: Float, c: Float, her: Boolean) {
    val sitting = who.pose == CharacterPose.SIT || who.pose == CharacterPose.SIT_SNUGGLE
    // The sitting sprite is drawn 5 cells lower, and its face a row lower again
    val top = ground - 26f * c + (if (sitting) 5f * c else 0f)
    val r = if (sitting) 1 else 0
    val flip = who.direction == Direction.LEFT
    val left = cx - 9f * c
    fun px(x: Int, y: Int, w: Int, h: Int, col: Color) {
        val gx = if (flip) 17 - (x + w - 1) else x
        scope.drawRect(col, Offset(left + gx * c, top + y * c), Size(w * c, h * c))
    }
    val skin = who.look.skin
    val ink = Color(0xFF3B2A24)
    val white = Color(0xFFF1F1F4)
    // Clear the big young eyes
    px(7, 7 + r, 5, 3, skin)
    if (!her) {
        // Bushy white brows, crinkly closed smiling eyes, laugh lines, a white moustache
        px(7, 7 + r, 2, 1, white); px(10, 7 + r, 2, 1, white)
        px(7, 9 + r, 2, 1, ink); px(10, 9 + r, 2, 1, ink)
        px(6, 9 + r, 1, 1, who.look.skinShadow); px(12, 9 + r, 1, 1, who.look.skinShadow)
        px(8, 10 + r, 3, 1, white)
    } else {
        // A grey bun, soft smiling eyes, rosy cheeks, a little lipstick
        px(7, -1 + r, 4, 2, Color(0xFFB8B8C0)); px(8, -2 + r, 2, 1, Color(0xFFDCDCE4))
        // Smiling eyes, two little arcs
        px(7, 9 + r, 1, 1, ink); px(8, 8 + r, 1, 1, ink)
        px(10, 8 + r, 1, 1, ink); px(11, 9 + r, 1, 1, ink)
        px(6, 10 + r, 1, 1, Color(0xFFFF8FA3)); px(12, 10 + r, 1, 1, Color(0xFFFF8FA3))
        px(9, 10 + r, 1, 1, Color(0xFFC1121F))
    }
}
