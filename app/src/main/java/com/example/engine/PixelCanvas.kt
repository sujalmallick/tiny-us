package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

enum class Direction {
    LEFT, RIGHT
}

enum class CharacterEmotion {
    IDLE,
    HAPPY,
    SHY,
    SLEEPY,
    CURIOUS,
    LOVING,
    SURPRISED,
    PLAYFUL
}

enum class CharacterPose {
    IDLE,
    IDLE_BLINK,
    WALK_1,
    WALK_2,
    WALK_3,
    WALK_4,
    SIT,
    SIT_SNUGGLE,
    SLEEP,
    SLEEP_YAWN,
    GIVE_FLOWER,
    RECEIVE_FLOWER,
    WAVE,
    HUG,
    HOLD_HANDS,
    HEAD_PAT,
    HEAD_PAT_RECEIVE,
    COOK,
    EAT_SNEAK,
    EAT_MOMO,
    FEED_MOMO,
    SURPRISED,
    JOY_JUMP,
    KISS
}

enum class EmoteType {
    NONE,
    HEART,
    SWEAT,
    EXCLAMATION,
    BLUSH,
    MUSIC_NOTE,
    QUESTION,
    DOTS,
    SPARKLE,
    SLEEP_Z,
    KISS
}

/** High-level behaviour state — wrapper layer only; CharacterPose values are never renamed. */
enum class CharacterState {
    IDLE, LOOKING, WALKING, SITTING, INTERACTING, SLEEPING, SURPRISED, HAPPY, SLEEPY
}

/**
 * Shared motion tweening & easing utility for smooth character transitions.
 * Applies cubic ease-in-out curve with physical anticipation and step generation.
 */
object CharacterMotionTween {
    /**
     * Shared reference walking speed in world units per second.
     * Derived from the reference cooking intro walk: 0.27 distance / 2.4s = 0.1125 units/second.
     */
    const val SHARED_WALKING_SPEED = 0.1125f

    fun easeInOutCubic(t: Float): Float {
        val clamped = t.coerceIn(0f, 1f)
        return if (clamped < 0.5f) {
            4f * clamped * clamped * clamped
        } else {
            1f - (-2f * clamped + 2f).let { it * it * it } / 2f
        }
    }
}

data class PixelCharacter(
    val isGirl: Boolean,
    var name: String,
    var worldX: Float,
    var worldY: Float,
    var pose: CharacterPose = CharacterPose.IDLE,
    var direction: Direction = Direction.RIGHT,
    var emotion: CharacterEmotion = CharacterEmotion.IDLE,
    var emote: EmoteType = EmoteType.NONE,
    var emoteTimer: Float = 0f,
    var bounceOffset: Float = 0f,
    var walkFrame: Int = 0,
    var blinkTimer: Float = 0f,
    var isBlinking: Boolean = false,
    var breathingOffset: Float = 0f,
    var idleSwayOffset: Float = 0f,
    var reactionTimer: Float = 0f,
    var nextBlinkTime: Float = 3.0f,
    var hasItem: String? = null,
    var wearsEarphone: Boolean = false,
    var outfitIndex: Int = 0,
    var accessoryIndex: Int = 0,
    var characterState: CharacterState = CharacterState.IDLE,
    var wearsGlasses: Boolean = false,
    var isSpeaking: Boolean = false,

    // Motion tweening state (smooth transitions)
    var targetWorldX: Float = worldX,
    var targetWorldY: Float = worldY,
    var startWorldX: Float = worldX,
    var startWorldY: Float = worldY,
    var posTransitionProgress: Float = 1f,
    var posTransitionDuration: Float = 0f,
    var isTransitioningPosition: Boolean = false,

    var targetPose: CharacterPose = pose,
    var previousPose: CharacterPose = pose,
    var poseTransitionProgress: Float = 1f,
    var poseTransitionDuration: Float = 0f,
    var isTransitioningPose: Boolean = false
) {
    /**
     * Smoothly interpolates character to a new world position.
     * Duration is strictly derived from distance / sharedWalkingSpeed to ensure uniform velocity across all scenes.
     * Uses cubic ease-in-out curve and animates walk stepping if distance >= 0.03.
     */
    val isMovingOrTransitioning: Boolean
        get() = isTransitioningPosition || isTransitioningPose

    fun moveTo(
        newX: Float,
        newY: Float = worldY,
        speed: Float = CharacterMotionTween.SHARED_WALKING_SPEED,
        arrivePose: CharacterPose? = null
    ) {
        if (arrivePose != null) {
            targetPose = arrivePose
        }
        isTransitioningPose = false
        val dist = kotlin.math.hypot(newX - worldX, newY - worldY)
        if (dist < 0.002f) {
            worldX = newX
            worldY = newY
            targetWorldX = newX
            targetWorldY = newY
            isTransitioningPosition = false
            posTransitionProgress = 1f
            if (arrivePose != null) {
                pose = arrivePose
            }
            return
        }
        startWorldX = worldX
        startWorldY = worldY
        targetWorldX = newX
        targetWorldY = newY
        posTransitionProgress = 0f
        posTransitionDuration = (dist / speed).coerceAtLeast(0.08f)
        isTransitioningPosition = true
        if (newX < worldX - 0.005f) {
            direction = Direction.LEFT
        } else if (newX > worldX + 0.005f) {
            direction = Direction.RIGHT
        }
    }

    /**
     * Smoothly transitions to a new gesture/pose, inserting a brief neutral anticipation frame.
     */
    fun transitionPoseTo(newPose: CharacterPose, duration: Float = 0.14f) {
        if (pose == newPose) {
            targetPose = newPose
            isTransitioningPose = false
            poseTransitionProgress = 1f
            return
        }
        previousPose = pose
        targetPose = newPose
        poseTransitionProgress = 0f
        poseTransitionDuration = duration.coerceAtLeast(0.08f)
        isTransitioningPose = true
    }

    /**
     * Resets character immediately without transition (used during scene load).
     */
    fun resetTo(x: Float, y: Float, newPose: CharacterPose = CharacterPose.IDLE, newDir: Direction = Direction.RIGHT) {
        worldX = x
        worldY = y
        targetWorldX = x
        targetWorldY = y
        startWorldX = x
        startWorldY = y
        posTransitionProgress = 1f
        posTransitionDuration = 0f
        isTransitioningPosition = false
        pose = newPose
        targetPose = newPose
        previousPose = newPose
        poseTransitionProgress = 1f
        poseTransitionDuration = 0f
        isTransitioningPose = false
        direction = newDir
        bounceOffset = 0f
        walkFrame = 0
    }

    /**
     * Advances motion interpolation each frame using actual deltaSeconds.
     */
    fun updateMotion(dt: Float) {
        // 1. Position tweening
        if (isTransitioningPosition) {
            posTransitionProgress = (posTransitionProgress + dt / posTransitionDuration).coerceAtMost(1f)
            val eased = CharacterMotionTween.easeInOutCubic(posTransitionProgress)
            worldX = startWorldX + (targetWorldX - startWorldX) * eased
            worldY = startWorldY + (targetWorldY - startWorldY) * eased

            val totalDist = kotlin.math.abs(targetWorldX - startWorldX)
            if (totalDist >= 0.03f) {
                // Cycle walk frames during movement (6.5 steps per second of physical time)
                val step = ((posTransitionProgress * posTransitionDuration * 6.5f).toInt() % 4)
                walkFrame = step
                pose = when (step) {
                    0 -> CharacterPose.WALK_1
                    1 -> CharacterPose.WALK_2
                    2 -> CharacterPose.WALK_3
                    else -> CharacterPose.WALK_4
                }
                bounceOffset = if (step % 2 == 0) 1.5f else 0f
            }

            if (posTransitionProgress >= 1f) {
                worldX = targetWorldX
                worldY = targetWorldY
                isTransitioningPosition = false
                pose = targetPose
                bounceOffset = 0f
            }
        }

        // 2. Pose anticipation / settling transition
        if (isTransitioningPose && !isTransitioningPosition) {
            poseTransitionProgress = (poseTransitionProgress + dt / poseTransitionDuration).coerceAtMost(1f)
            if (poseTransitionProgress < 0.55f) {
                // Neutral anticipation dip: arms pass through neutral resting pose
                val dipProgress = poseTransitionProgress / 0.55f
                val dip = kotlin.math.sin(dipProgress * kotlin.math.PI.toFloat()) * 1.5f
                bounceOffset = dip
                if (pose != CharacterPose.IDLE && targetPose != CharacterPose.IDLE && reactionTimer <= 0) {
                    pose = CharacterPose.IDLE
                }
            } else {
                pose = targetPose
                bounceOffset = 0f
            }
            if (poseTransitionProgress >= 1f) {
                pose = targetPose
                isTransitioningPose = false
                bounceOffset = 0f
            }
        }
    }
}

object PixelArtRenderer {

    /** Uniform 10% character size increase applied to every scene automatically. */
    private const val CHARACTER_SCALE_FACTOR = 1.10f


    data class GirlDressPalette(
        val sweater: Color,
        val trim: Color,
        val skirt: Color,
        val skirtShadow: Color,
        val ribbon: Color,
        val isHoodie: Boolean = false
    )

    val GirlDressPalettes = listOf(
        // 0: Classic Blush Strawberry Cozy Knit
        GirlDressPalette(Color(0xFFF5CAC3), Color(0xFFF78CA0), Color(0xFFD6587A), Color(0xFFB53D60), Color(0xFFFF5D8F)),
        // 1: Lavender Dream Floral Wrap Dress
        GirlDressPalette(Color(0xFFE8D7F1), Color(0xFFC77DFF), Color(0xFF9D4EDD), Color(0xFF7B2CBF), Color(0xFFC77DFF)),
        // 2: Emerald Velvet Romance Gown
        GirlDressPalette(Color(0xFF74C69D), Color(0xFF52B788), Color(0xFF2D6A4F), Color(0xFF1B4332), Color(0xFF95D5B2)),
        // 3: Lemon Sunshine Picnic Dress
        GirlDressPalette(Color(0xFFFFF3B0), Color(0xFFFFD166), Color(0xFFE9C46A), Color(0xFFD4A373), Color(0xFFF4A261)),
        // 4: Midnight Starlight Evening Gown
        GirlDressPalette(Color(0xFF4A6FA5), Color(0xFF98C1D9), Color(0xFF1E3A8A), Color(0xFF0F172A), Color(0xFFE0FBFC)),
        // 5: Mint Macaron Chiffon Tea Dress
        GirlDressPalette(Color(0xFFC3DBD0), Color(0xFFA7C4B5), Color(0xFF6B9080), Color(0xFF4E6E60), Color(0xFFA7C4B5)),
        // 6: Cozy Oversized Flannel
        GirlDressPalette(Color(0xFF457B9D), Color(0xFF1D3557), Color(0xFF2B334B), Color(0xFF1B263B), Color(0xFFE63946)),
        // 7: Blush Rose Cropped Hoodie & Pleated Skirt
        GirlDressPalette(Color(0xFFF4ACB7), Color(0xFFFFCAD4), Color(0xFFD6587A), Color(0xFFB53D60), Color(0xFFFF758F), isHoodie = true),
        // 8: Sage & Cream Colorblock Hoodie & Linen Skirt
        GirlDressPalette(Color(0xFF84A98C), Color(0xFFF8F9FA), Color(0xFF52796F), Color(0xFF354F52), Color(0xFFCAD2C5), isHoodie = true),
        // 9: Lavender Cloud Oversized Hoodie & Denim Skirt
        GirlDressPalette(Color(0xFFD8BBFF), Color(0xFFE8D7F1), Color(0xFF3D5A80), Color(0xFF293241), Color(0xFFC77DFF), isHoodie = true),
        // 10: Buttercream Star Shimmer Hoodie & Skirt
        GirlDressPalette(Color(0xFFFFF1C5), Color(0xFFFFD166), Color(0xFFE09F3E), Color(0xFF9E2A2B), Color(0xFFFFE3A8), isHoodie = true)
    )

    fun getGirlDressPalette(index: Int): GirlDressPalette {
        return GirlDressPalettes.getOrElse(index) { GirlDressPalettes[0] }
    }

    data class BoyOutfitPalette(
        val sweater: Color,
        val sweaterHighlight: Color,
        val collar: Color,
        val pants: Color,
        val pantsFold: Color,
        val shoes: Color = Color(0xFF495057),
        val shoesSole: Color = Color(0xFFECEFE6),
        val isHoodie: Boolean = false
    )

    val BoyOutfitPalettes = listOf(
        // 0: Classic Spruce Knit & Navy Pants
        BoyOutfitPalette(
            sweater = Color(0xFF2D6A4F),
            sweaterHighlight = Color(0xFF40916C),
            collar = Color(0xFF1B4332),
            pants = Color(0xFF2B334B),
            pantsFold = Color(0xFF1F2436),
            isHoodie = false
        ),
        // 1: White & Emerald Varsity Hoodie & Khaki Chinos
        BoyOutfitPalette(
            sweater = Color(0xFFF8F9FA),
            sweaterHighlight = Color(0xFFFFFFFF),
            collar = Color(0xFF2D6A4F),
            pants = Color(0xFFB08968),
            pantsFold = Color(0xFF7F5539),
            isHoodie = true
        ),
        // 2: Charcoal Streetwear Zip Hoodie & Dark Indigo Denim
        BoyOutfitPalette(
            sweater = Color(0xFF343A40),
            sweaterHighlight = Color(0xFF495057),
            collar = Color(0xFF212529),
            pants = Color(0xFF1D2D44),
            pantsFold = Color(0xFF0D1B2A),
            isHoodie = true
        ),
        // 3: Oatmeal Cloud Oversized Hoodie & Slate Cargo Pants
        BoyOutfitPalette(
            sweater = Color(0xFFEDE0D4),
            sweaterHighlight = Color(0xFFF7F1EB),
            collar = Color(0xFFDDB892),
            pants = Color(0xFF4A5568),
            pantsFold = Color(0xFF2D3748),
            isHoodie = true
        ),
        // 4: Midnight Starlight Graphic Hoodie & Black Denim
        BoyOutfitPalette(
            sweater = Color(0xFF1E293B),
            sweaterHighlight = Color(0xFF334155),
            collar = Color(0xFF64748B),
            pants = Color(0xFF18181B),
            pantsFold = Color(0xFF09090B),
            isHoodie = true
        )
    )

    fun getBoyOutfitPalette(index: Int): BoyOutfitPalette {
        return BoyOutfitPalettes.getOrElse(index) { BoyOutfitPalettes[0] }
    }

    // Warm, cozy retro pixel palette
    val SkinToneBoy = Color(0xFFFFDFBA)
    val SkinToneBoyShadow = Color(0xFFF5CB9F)
    val SkinToneGirl = Color(0xFFD49B7A)        // Warmer, light tan/brown shade
    val SkinToneGirlShadow = Color(0xFFB87D5C)  // Rich warm tan/brown shadow
    val BlushCoral = Color(0xFFFFAAA6)
    val BlushRose = Color(0xFFFF758F)
    val EyeDark = Color(0xFF22223B)
    val EyeSparkle = Color(0xFFFFFFFF)

    // Signature square-frame glasses
    val GlassesFrame = Color(0xFF1E222F)        // Dark charcoal/black square frame
    val GlassesFrameLight = Color(0xFF3D4457)   // Subtle rim highlight / bridge
    val GlassesGlint = Color(0x40FFFFFF)        // Soft lens glint

    // Boy hair & clothes
    val HairBoy = Color(0xFF2A2829)
    val HairBoyHighlight = Color(0xFF484240)
    val HairBoyShadow = Color(0xFF1B191A)
    val SweaterBoy = Color(0xFF2D6A4F)
    val SweaterBoyHighlight = Color(0xFF40916C)
    val SweaterBoyCollar = Color(0xFF1B4332)
    val PantsBoy = Color(0xFF2B334B)
    val PantsBoyFold = Color(0xFF1F2436)
    val ShoesBoy = Color(0xFF495057)
    val ShoesBoySole = Color(0xFFECEFE6)

    // Girl hair & clothes
    val HairGirl = Color(0xFF6A381F)
    val HairGirlHighlight = Color(0xFF93532C)
    val HairGirlShadow = Color(0xFF492413)
    val RibbonGirl = Color(0xFFFF5D8F)
    val RibbonCenter = Color(0xFFFFD166)
    val SweaterGirl = Color(0xFFF5CAC3)
    val SweaterGirlTrim = Color(0xFFF78CA0)
    val SweaterGirlShadow = Color(0xFFE0A49A)
    val SkirtGirl = Color(0xFFD6587A)
    val SkirtGirlShadow = Color(0xFFB53D60)
    val SocksGirl = Color(0xFFFFF1E6)
    val ShoesGirl = Color(0xFF503629)

    // Props & accents
    val FlowerRed = Color(0xFFE63946)
    val FlowerPink = Color(0xFFFF758F)
    val FlowerStem = Color(0xFF40916C)
    val FlowerCenter = Color(0xFFFFD166)
    val CookieBrown = Color(0xFFD4A373)
    val CookieChoc = Color(0xFF6B4226)
    val SpoonWood = Color(0xFFB07D62)

    // Winter warm clothes palette (used in SNOW mode)
    // Boy Winter Outfit
    val WinterBeanieBoy = Color(0xFF1E3F30)         // Dark spruce green knitted beanie
    val WinterBeanieBrimBoy = Color(0xFF2D6A4F)     // Folded knit brim
    val WinterPomPomBoy = Color(0xFFE9C46A)         // Warm mustard pom-pom on top
    val WinterScarfBoy = Color(0xFFE9C46A)          // Warm chunky mustard knitted scarf
    val WinterScarfBoyDark = Color(0xFFD4A373)      // Scarf shadow / fringe
    val WinterCoatBoy = Color(0xFF1B4332)           // Heavy dark evergreen winter wool coat
    val WinterCoatBoyTrim = Color(0xFF40916C)       // Coat seam / zipper
    val WinterGlovesBoy = Color(0xFF264653)         // Charcoal/slate warm winter gloves
    val WinterPantsBoy = Color(0xFF1F2436)          // Thermal dark navy winter trousers
    val WinterBootsBoy = Color(0xFF3D261D)          // Sturdy brown winter leather boots
    val WinterBootsBoySole = Color(0xFF252422)      // Thick snow-tread sole

    // Girl Winter Outfit
    val WinterEarmuffsGirl = Color(0xFFFF5D8F)       // Cozy rose earmuff band
    val WinterEarmuffsFluff = Color(0xFFFFF0F3)      // Fluffy plush white earmuff puffs
    val WinterScarfGirl = Color(0xFFFFCAD4)          // Soft pastel rose knit scarf
    val WinterScarfGirlDark = Color(0xFFF78CA0)      // Rosy scarf accent / fringe
    val WinterCoatGirl = Color(0xFFE07A5F)           // Warm terracotta / strawberry cream wool coat
    val WinterCoatGirlFur = Color(0xFFFFF0F3)        // Soft white sherpa/fleece trim along collar & hem
    val WinterMittensGirl = Color(0xFFFFF0F3)        // Cute fluffy cream mittens
    val WinterTightsGirl = Color(0xFF2B2D42)         // Warm charcoal/plum opaque thermal winter tights
    val WinterBootsGirl = Color(0xFF5E4134)          // Cozy cocoa winter snow boots
    val WinterBootsGirlFur = Color(0xFFFFF0F3)       // Fluffy fleece lining along top of boots

    /**
     * Draw character at canvas coordinate (centerX, bottomY) with given pixel scale.
     */
    fun drawCharacter(
        drawScope: DrawScope,
        char: PixelCharacter,
        centerX: Float,
        bottomY: Float,
        pixelSize: Float = 3.5f,
        isHoldingUmbrella: Boolean = false,
        isSnow: Boolean = false,
        isSpeaking: Boolean = false
    ) {
        // Feature 1: uniform +10% scale applied here once, covering all scenes and all poses
        val p = pixelSize * CHARACTER_SCALE_FACTOR
        val flip = char.direction == Direction.LEFT
        val w = 18
        val h = 26
        val startX = centerX - (w * p) / 2f + char.idleSwayOffset
        val startY = bottomY - (h * p) - char.bounceOffset

        // Grounding contact drop-shadow beneath character's feet
        val shadowW = when (char.pose) {
            CharacterPose.SLEEP, CharacterPose.SLEEP_YAWN -> 19f * p
            CharacterPose.SIT, CharacterPose.SIT_SNUGGLE -> 17f * p
            CharacterPose.HUG, CharacterPose.KISS -> 16f * p
            else -> 14f * p
        }
        val shadowH = 4.2f * p
        val shadowAlpha = when (char.pose) {
            CharacterPose.SLEEP, CharacterPose.SLEEP_YAWN -> 0.18f
            else -> 0.25f
        }
        drawScope.drawOval(
            color = Color(0xFF151820).copy(alpha = shadowAlpha),
            topLeft = Offset(centerX - shadowW / 2f + char.idleSwayOffset * 0.5f, bottomY - shadowH * 0.55f),
            size = Size(shadowW, shadowH)
        )

        when (char.pose) {
            CharacterPose.SIT, CharacterPose.SIT_SNUGGLE -> {
                drawSittingCharacter(drawScope, char, startX, startY + 5 * p, p, flip, isHoldingUmbrella, isSnow)
            }
            CharacterPose.SLEEP, CharacterPose.SLEEP_YAWN -> {
                drawSleepingCharacter(drawScope, char, startX, startY + 6 * p, p, flip, isSnow)
            }
            CharacterPose.HUG -> {
                drawHuggingCharacter(drawScope, char, startX, startY, p, flip, isSnow)
            }
            CharacterPose.KISS -> {
                drawKissingCharacter(drawScope, char, startX, startY, p, flip, isSnow)
            }
            else -> {
                drawStandingCharacter(drawScope, char, startX, startY, p, flip, isHoldingUmbrella, isSnow)
            }
        }

        val poseOffsetY = when (char.pose) {
            CharacterPose.SIT, CharacterPose.SIT_SNUGGLE -> 5 * p
            CharacterPose.SLEEP, CharacterPose.SLEEP_YAWN -> 6 * p
            else -> 0f
        }

        // Layer accessories on top of character (unless in snow mode where winter gear is worn)
        if (!isSnow && char.accessoryIndex > 0) {
            drawAccessory(drawScope, char, startX, startY + poseOffsetY, p, flip)
        }

        // Draw signature square-frame glasses - ALWAYS in front of accessories and face!
        if (char.wearsGlasses) {
            drawGlasses(drawScope, char, startX, startY + poseOffsetY, p, flip)
        }

        // Draw cute retro earphone bud on character if active
        if (char.wearsEarphone) {
            drawEarphoneOnHead(drawScope, char, startX, startY + poseOffsetY, p, flip)
        }

        // Draw emote bubble above head if active (suppressed when speaking so it doesn't overlap the speech bubble)
        if (!char.isSpeaking && !isSpeaking && char.emote != EmoteType.NONE && char.emoteTimer > 0) {
            val bubbleY = if (isHoldingUmbrella) {
                startY - 24 * p
            } else {
                startY - 14 * p
            }
            drawEmoteBubble(drawScope, char.emote, centerX, bubbleY, p)
        }
    }

    private fun drawStandingCharacter(
        scope: DrawScope,
        char: PixelCharacter,
        startX: Float,
        startY: Float,
        p: Float,
        flip: Boolean,
        isHoldingUmbrella: Boolean = false,
        isSnow: Boolean = false
    ) {
        val isGirl = char.isGirl
        val girlDress = getGirlDressPalette(char.outfitIndex)
        val boyOutfit = getBoyOutfitPalette(char.outfitIndex)
        val hairColor = if (isGirl) HairGirl else HairBoy
        val hairHigh = if (isGirl) HairGirlHighlight else HairBoyHighlight
        val hairShadow = if (isGirl) HairGirlShadow else HairBoyShadow
        val sweaterColor = if (isSnow) {
            if (isGirl) WinterCoatGirl else WinterCoatBoy
        } else {
            if (isGirl) girlDress.sweater else boyOutfit.sweater
        }
        val sweaterHighlight = if (isSnow) {
            if (isGirl) WinterCoatGirlFur else WinterCoatBoyTrim
        } else {
            if (isGirl) girlDress.trim else boyOutfit.sweaterHighlight
        }
        val skinColor = if (isGirl) SkinToneGirl else SkinToneBoy
        val skinShadow = if (isGirl) SkinToneGirlShadow else SkinToneBoyShadow
        val blushColor = if (isSnow) BlushRose else (if (char.emotion == CharacterEmotion.LOVING || char.emotion == CharacterEmotion.SHY) BlushRose else BlushCoral)
        val handColor = if (isSnow) {
            if (isGirl) WinterMittensGirl else WinterGlovesBoy
        } else {
            skinColor
        }

        fun px(gridX: Int, gridY: Int, color: Color) {
            val actualX = if (flip) (17 - gridX) else gridX
            val breathY = if (gridY <= 17) -char.breathingOffset else 0f
            scope.drawRect(
                color = color,
                topLeft = Offset(startX + actualX * p, startY + gridY * p + breathY),
                size = Size(p, p)
            )
        }

        fun drawPixelRect(gx: Int, gy: Int, gw: Int, gh: Int, color: Color, breathY: Float) {
            val actualLeftX = if (flip) (17 - (gx + gw - 1)) else gx
            scope.drawRect(
                color = color,
                topLeft = Offset(startX + actualLeftX * p, startY + gy * p + breathY),
                size = Size(gw * p, gh * p)
            )
        }

        fun fillRect(gx: Int, gy: Int, gw: Int, gh: Int, color: Color) {
            if (gw <= 0 || gh <= 0) return
            val breath = -char.breathingOffset
            if (gy + gh <= 18) {
                drawPixelRect(gx, gy, gw, gh, color, breath)
            } else if (gy > 17) {
                drawPixelRect(gx, gy, gw, gh, color, 0f)
            } else {
                val topH = 18 - gy
                val bottomH = gh - topH
                drawPixelRect(gx, gy, gw, topH, color, breath)
                drawPixelRect(gx, 18, gw, bottomH, color, 0f)
            }
        }

        // --- 1. Head & Hair Base ---
        if (isSnow && !isGirl) {
            // Boy Winter Beanie with pom-pom on top
            fillRect(8, 0, 2, 2, WinterPomPomBoy) // pom-pom
            fillRect(6, 2, 7, 3, WinterBeanieBoy) // beanie dome
            fillRect(5, 4, 9, 2, WinterBeanieBrimBoy) // folded brim
            px(7, 3, WinterCoatBoyTrim)
            px(8, 3, WinterCoatBoyTrim)
            // Bangs peeking out beneath brim
            px(6, 6, hairColor)
            px(8, 6, hairColor)
            px(10, 6, hairColor)
            fillRect(4, 5, 2, 3, hairColor)
            fillRect(12, 5, 2, 3, hairColor)
        } else {
            fillRect(6, 2, 7, 2, hairColor)
            fillRect(5, 4, 9, 3, hairColor)

            if (isGirl) {
                // Cute wavy side locks framing face
                fillRect(4, 5, 2, 10, hairColor)
                fillRect(12, 5, 2, 10, hairColor)
                fillRect(3, 8, 2, 6, hairShadow)
                fillRect(13, 9, 2, 6, hairShadow)

                // Flowing hair strands down the back
                fillRect(3, 13, 2, 4, hairColor)
                fillRect(13, 14, 2, 3, hairColor)

                // Hair highlights
                fillRect(7, 3, 3, 1, hairHigh)
                fillRect(8, 4, 3, 1, hairHigh)

                // Signature Flower Ribbon Clip
                fillRect(11, 4, 2, 2, girlDress.ribbon)
                px(11, 3, girlDress.ribbon)
                px(13, 4, girlDress.ribbon)
                px(12, 4, RibbonCenter) // golden center
            } else {
                // Boy styled tousled bangs
                fillRect(4, 4, 2, 4, hairColor)
                fillRect(12, 4, 2, 3, hairColor)
                fillRect(6, 1, 5, 1, hairHigh)
                fillRect(7, 2, 4, 1, hairHigh)
                px(5, 3, hairHigh)
                px(11, 3, hairShadow)
                // Bangs hanging over forehead
                px(6, 6, hairColor)
                px(8, 6, hairColor)
                px(10, 6, hairColor)
            }
        }

        if (isSnow && isGirl) {
            // Plush winter earmuffs on girl
            fillRect(6, 2, 6, 1, WinterEarmuffsGirl) // top band
            fillRect(3, 6, 2, 4, WinterEarmuffsFluff) // left ear fluff
            fillRect(13, 6, 2, 4, WinterEarmuffsFluff) // right ear fluff
            px(3, 7, Color.White)
            px(14, 7, Color.White)
        }

        // --- 2. Face Skin ---
        fillRect(6, 7, 7, 4, skinColor)
        fillRect(7, 11, 5, 1, skinColor)
        // Ear
        px(5, 8, skinShadow)

        // --- 3. Eyes with Pixel Sparkles ---
        val isClosedEyes = char.isBlinking ||
                char.pose == CharacterPose.IDLE_BLINK ||
                char.pose == CharacterPose.HUG ||
                char.pose == CharacterPose.HEAD_PAT_RECEIVE ||
                char.emotion == CharacterEmotion.LOVING ||
                char.pose == CharacterPose.JOY_JUMP

        if (isClosedEyes) {
            // Sweet curved happy eyes ^_^
            px(7, 8, EyeDark)
            px(8, 7, EyeDark)
            px(9, 8, EyeDark)
            px(10, 8, EyeDark)
            px(11, 7, EyeDark)
            px(12, 8, EyeDark)
        } else if (char.pose == CharacterPose.SURPRISED || char.emotion == CharacterEmotion.SURPRISED) {
            // Wide surprised eyes with catchlights
            fillRect(7, 7, 2, 3, Color.White)
            fillRect(10, 7, 2, 3, Color.White)
            fillRect(8, 8, 1, 2, EyeDark)
            fillRect(11, 8, 1, 2, EyeDark)
            px(7, 7, EyeSparkle)
            px(10, 7, EyeSparkle)
        } else if (char.emotion == CharacterEmotion.SLEEPY) {
            // Drooping relaxed eyes
            px(7, 8, EyeDark)
            px(8, 8, EyeDark)
            px(10, 8, EyeDark)
            px(11, 8, EyeDark)
            px(7, 9, skinShadow)
            px(10, 9, skinShadow)
        } else {
            // Standard expressive eyes with warm sparkle
            fillRect(7, 8, 2, 2, EyeDark)
            fillRect(10, 8, 2, 2, EyeDark)
            // Sparkle dot in upper-left
            px(7, 8, EyeSparkle)
            px(10, 8, EyeSparkle)
        }

        // --- 4. Rosy Cheek Blush ---
        val blushSize = if (char.emotion == CharacterEmotion.LOVING || char.emotion == CharacterEmotion.SHY) 2 else 1
        fillRect(6, 9, blushSize, 1, blushColor)
        fillRect(11, 9, blushSize, 1, blushColor)

        // --- 5. Mouth ---
        if (char.pose == CharacterPose.SURPRISED || char.emotion == CharacterEmotion.SURPRISED) {
            fillRect(8, 10, 2, 1, EyeDark) // tiny 'o'
        } else if (char.pose == CharacterPose.EAT_SNEAK) {
            px(9, 10, EyeDark)
            px(10, 10, CookieBrown)
        } else {
            px(9, 10, blushColor) // tiny smile
            px(8, 10, skinColor)
        }

        // --- 6. Sweater / Winter Coat / Torso ---
        fillRect(6, 12, 7, 6, sweaterColor)
        // Collar detail
        fillRect(7, 12, 4, 1, if (isGirl) girlDress.trim else boyOutfit.collar)
        // Side shadows
        fillRect(5, 13, 1, 4, sweaterColor)
        fillRect(12, 13, 1, 4, sweaterColor)

        // Hoodie features: cozy rolled hood collar around neck, dropped shoulders, front kangaroo pouch, and drawstrings
        if (!isSnow) {
            val isHoodieOutfit = (!isGirl && boyOutfit.isHoodie) || (isGirl && (girlDress.isHoodie || char.outfitIndex >= 7))
            if (isHoodieOutfit) {
                val hoodColor = if (isGirl) girlDress.sweater else boyOutfit.sweater
                val hoodAccent = if (isGirl) girlDress.trim else boyOutfit.collar
                val hoodShadow = if (isGirl) girlDress.skirtShadow else boyOutfit.pantsFold
                val pouchHighlight = if (isGirl) girlDress.trim else boyOutfit.sweaterHighlight
                val pouchShadow = if (isGirl) girlDress.skirtShadow else boyOutfit.collar
                val cordColor = if (isGirl) {
                    Color.White.copy(alpha = 0.95f)
                } else {
                    if (char.outfitIndex == 1) boyOutfit.collar else Color.White.copy(alpha = 0.95f)
                }
                val cordTipColor = if (isGirl) girlDress.ribbon else (if (char.outfitIndex == 1) boyOutfit.pants else Color(0xFFFFD166))

                // 1. Cozy rolled hood fabric draped around neck and over shoulders
                px(5, 11, hoodShadow)
                px(6, 11, hoodColor)
                px(11, 11, hoodColor)
                px(12, 11, hoodShadow)

                // Dropped-shoulder hood roll seam
                px(4, 12, hoodShadow)
                fillRect(5, 12, 8, 1, hoodColor)
                fillRect(6, 12, 6, 1, hoodAccent)
                px(13, 12, hoodShadow)

                // 2. Front kangaroo pouch pocket across lower belly
                fillRect(6, 15, 6, 2, hoodColor)
                fillRect(7, 15, 4, 1, pouchHighlight)
                px(6, 15, pouchShadow)
                px(11, 15, pouchShadow)
                // Ribbed waist hem
                fillRect(6, 17, 7, 1, hoodAccent)

                // 3. Thick hoodie drawstrings dangling with knotted tips
                px(8, 13, cordColor)
                px(8, 14, cordColor)
                px(8, 15, cordTipColor)
                px(10, 13, cordColor)
                px(10, 14, cordColor)
                px(10, 15, cordTipColor)
            }
        }

        if (isSnow) {
            if (isGirl) {
                // Fluffy sherpa fleece trim along coat collar and hem
                fillRect(6, 12, 7, 1, WinterCoatGirlFur)
                fillRect(5, 17, 9, 1, WinterCoatGirlFur)
                // Soft pastel rose knit scarf around neck
                fillRect(6, 11, 7, 2, WinterScarfGirl)
                px(6, 12, WinterScarfGirlDark)
                px(12, 12, WinterScarfGirlDark)
                // Scarf tail dangling down over coat
                fillRect(10, 13, 2, 3, WinterScarfGirl)
                px(10, 15, WinterScarfGirlDark)
                px(11, 15, WinterScarfGirlDark)
            } else {
                // Warm chunky mustard knit scarf around boy's neck
                fillRect(6, 11, 7, 2, WinterScarfBoy)
                px(6, 12, WinterScarfBoyDark)
                px(12, 12, WinterScarfBoyDark)
                // Scarf tail dangling down over coat
                fillRect(7, 13, 2, 3, WinterScarfBoy)
                px(7, 15, WinterScarfBoyDark)
                px(8, 15, WinterScarfBoyDark)
                // Coat buttons down center
                px(9, 14, WinterPomPomBoy)
                px(9, 16, WinterPomPomBoy)
            }
        } else {
            // Knit texture / highlight
            px(7, 14, sweaterHighlight)
            px(10, 15, sweaterHighlight)
        }

        // --- 7. Arms & Actions ---
        if (isHoldingUmbrella && !isGirl) {
            // Boy raised arm reaching up and outward towards umbrella handle centered between couple
            fillRect(12, 12, 2, 3, sweaterColor)
            fillRect(13, 11, 2, 2, sweaterColor)
            fillRect(15, 10, 2, 2, sweaterColor)
            fillRect(16, 9, 2, 2, handColor)
            px(17, 9, skinShadow)
            // Left arm resting comfortably at side
            fillRect(4, 13, 2, 4, sweaterColor)
            fillRect(4, 17, 1, 1, handColor)
        } else when (char.pose) {
            CharacterPose.WAVE -> {
                // Waving hand high
                fillRect(13, 9, 2, 3, sweaterColor)
                fillRect(14, 7, 2, 2, handColor)
                px(15, 6, handColor)
                // Other arm resting
                fillRect(4, 13, 2, 4, sweaterColor)
                fillRect(4, 17, 1, 1, handColor)
            }
            CharacterPose.GIVE_FLOWER -> {
                // Arm extended forward holding flower
                fillRect(12, 14, 3, 2, sweaterColor)
                fillRect(15, 14, 1, 2, handColor)
                // Rose
                fillRect(16, 12, 2, 2, FlowerRed)
                px(17, 11, FlowerPink)
                px(16, 14, FlowerStem)
                px(16, 15, FlowerStem)
                px(17, 14, Color(0xFF55A630)) // leaf
                // Left arm resting
                fillRect(4, 13, 2, 4, sweaterColor)
            }
            CharacterPose.RECEIVE_FLOWER -> {
                // Hands held together at heart holding blossom
                fillRect(8, 14, 3, 2, handColor)
                fillRect(8, 12, 3, 2, FlowerRed)
                px(9, 11, FlowerPink)
                px(9, 13, FlowerStem)
                fillRect(5, 13, 2, 3, sweaterColor)
                fillRect(11, 13, 2, 3, sweaterColor)
            }
            CharacterPose.HEAD_PAT -> {
                // Boy raising arm and extending hand forward over her head
                fillRect(12, 11, 4, 2, sweaterColor)
                fillRect(16, 10, 2, 2, handColor)
                fillRect(4, 13, 2, 4, sweaterColor)
            }
            CharacterPose.HEAD_PAT_RECEIVE -> {
                // Girl leaning in with hands tucked
                fillRect(5, 14, 2, 3, sweaterColor)
                fillRect(11, 14, 2, 3, sweaterColor)
                fillRect(8, 15, 2, 1, handColor)
            }
            CharacterPose.HOLD_HANDS -> {
                // Arm reaching toward center
                fillRect(12, 14, 3, 2, sweaterColor)
                fillRect(15, 15, 2, 2, handColor)
                fillRect(4, 13, 2, 4, sweaterColor)
                fillRect(4, 17, 1, 1, handColor)
            }
            CharacterPose.COOK -> {
                // Holding wooden spoon over stove
                fillRect(12, 14, 3, 2, sweaterColor)
                fillRect(15, 14, 1, 1, handColor)
                fillRect(15, 11, 1, 5, SpoonWood)
                fillRect(4, 13, 2, 4, sweaterColor)
            }
            CharacterPose.EAT_SNEAK -> {
                // Hand to mouth with chocolate cookie
                fillRect(11, 12, 2, 2, handColor)
                fillRect(10, 11, 2, 2, CookieBrown)
                px(11, 11, CookieChoc)
                fillRect(4, 13, 2, 4, sweaterColor)
            }
            CharacterPose.EAT_MOMO -> {
                // Hand holding a delicious steamed momo dumpling with dipping red chutney!
                fillRect(11, 12, 2, 2, handColor)
                fillRect(10, 10, 3, 3, Color(0xFFFFFDF0))
                px(11, 9, Color(0xFFFAF0CA)) // pleated top pinch
                px(12, 11, Color(0xFFD90429)) // red chutney dab!
                fillRect(4, 13, 2, 4, sweaterColor)
            }
            CharacterPose.FEED_MOMO -> {
                // Arm extended forward holding momo on skewer / toothpick
                fillRect(12, 14, 3, 2, sweaterColor)
                fillRect(15, 14, 1, 2, handColor)
                // Momo dumpling on toothpick
                px(15, 13, Color(0xFFDDA15E)) // skewer
                fillRect(16, 11, 3, 3, Color(0xFFFFFDF0))
                px(17, 10, Color(0xFFFAF0CA)) // pleat top
                px(18, 12, Color(0xFFD90429)) // spicy red chili chutney
                fillRect(4, 13, 2, 4, sweaterColor)
            }
            CharacterPose.JOY_JUMP -> {
                // Both arms raised up in celebration
                fillRect(3, 11, 2, 3, sweaterColor)
                fillRect(3, 9, 2, 2, handColor)
                fillRect(13, 11, 2, 3, sweaterColor)
                fillRect(13, 9, 2, 2, handColor)
            }
            else -> {
                // Natural relaxed arms at sides
                fillRect(4, 13, 2, 4, sweaterColor)
                fillRect(4, 17, 1, 1, handColor)
                fillRect(12, 13, 2, 4, sweaterColor)
                fillRect(13, 17, 1, 1, handColor)
            }
        }

        // --- 8. Lower Body (Skirt/Pants & Legs & Boots) ---
        if (isGirl) {
            if (isSnow) {
                // Winter warm coat skirt, thermal tights, and fur-lined snow boots
                fillRect(5, 18, 8, 3, WinterCoatGirl)
                px(7, 18, WinterCoatGirlFur)
                px(9, 18, WinterCoatGirlFur)
                px(11, 18, WinterCoatGirlFur)

                // Thick thermal winter tights
                val walkCycle = char.walkFrame % 4
                when (char.pose) {
                    CharacterPose.WALK_1, CharacterPose.WALK_2, CharacterPose.WALK_3, CharacterPose.WALK_4 -> {
                        if (walkCycle == 0 || walkCycle == 1) {
                            fillRect(5, 21, 2, 3, WinterTightsGirl)
                            fillRect(10, 20, 2, 4, WinterTightsGirl)
                        } else {
                            fillRect(6, 20, 2, 4, WinterTightsGirl)
                            fillRect(10, 21, 2, 3, WinterTightsGirl)
                        }
                    }
                    CharacterPose.JOY_JUMP -> {
                        fillRect(6, 21, 2, 2, WinterTightsGirl)
                        fillRect(10, 21, 2, 2, WinterTightsGirl)
                    }
                    else -> {
                        fillRect(6, 21, 2, 3, WinterTightsGirl)
                        fillRect(10, 21, 2, 3, WinterTightsGirl)
                    }
                }

                // Winter boots with fluffy fleece cuff
                when (char.pose) {
                    CharacterPose.WALK_1, CharacterPose.WALK_2 -> {
                        fillRect(4, 23, 3, 1, WinterBootsGirlFur)
                        fillRect(4, 24, 3, 2, WinterBootsGirl)
                        fillRect(10, 22, 3, 1, WinterBootsGirlFur)
                        fillRect(10, 23, 3, 2, WinterBootsGirl)
                    }
                    CharacterPose.WALK_3, CharacterPose.WALK_4 -> {
                        fillRect(6, 22, 3, 1, WinterBootsGirlFur)
                        fillRect(6, 23, 3, 2, WinterBootsGirl)
                        fillRect(10, 23, 3, 1, WinterBootsGirlFur)
                        fillRect(10, 24, 3, 2, WinterBootsGirl)
                    }
                    CharacterPose.JOY_JUMP -> {
                        fillRect(5, 22, 3, 1, WinterBootsGirlFur)
                        fillRect(5, 23, 3, 2, WinterBootsGirl)
                        fillRect(10, 22, 3, 1, WinterBootsGirlFur)
                        fillRect(10, 23, 3, 2, WinterBootsGirl)
                    }
                    else -> {
                        fillRect(5, 23, 3, 1, WinterBootsGirlFur)
                        fillRect(5, 24, 3, 2, WinterBootsGirl)
                        fillRect(10, 23, 3, 1, WinterBootsGirlFur)
                        fillRect(10, 24, 3, 2, WinterBootsGirl)
                    }
                }
            } else {
                // Pleated Skirt
                fillRect(5, 18, 8, 3, girlDress.skirt)
                px(7, 18, girlDress.skirtShadow)
                px(9, 18, girlDress.skirtShadow)
                px(11, 18, girlDress.skirtShadow)

                // Cozy socks & legs
                val walkCycle = char.walkFrame % 4
                when (char.pose) {
                    CharacterPose.WALK_1, CharacterPose.WALK_2, CharacterPose.WALK_3, CharacterPose.WALK_4 -> {
                        if (walkCycle == 0 || walkCycle == 1) {
                            fillRect(5, 21, 2, 3, SocksGirl)
                            fillRect(10, 20, 2, 4, SocksGirl)
                        } else {
                            fillRect(6, 20, 2, 4, SocksGirl)
                            fillRect(10, 21, 2, 3, SocksGirl)
                        }
                    }
                    CharacterPose.JOY_JUMP -> {
                        fillRect(6, 21, 2, 2, SocksGirl)
                        fillRect(10, 21, 2, 2, SocksGirl)
                    }
                    else -> {
                        fillRect(6, 21, 2, 3, SocksGirl)
                        fillRect(10, 21, 2, 3, SocksGirl)
                    }
                }

                // Shoes
                when (char.pose) {
                    CharacterPose.WALK_1, CharacterPose.WALK_2 -> {
                        fillRect(4, 24, 3, 2, ShoesGirl)
                        fillRect(10, 23, 3, 2, ShoesGirl)
                    }
                    CharacterPose.WALK_3, CharacterPose.WALK_4 -> {
                        fillRect(6, 23, 3, 2, ShoesGirl)
                        fillRect(10, 24, 3, 2, ShoesGirl)
                    }
                    CharacterPose.JOY_JUMP -> {
                        fillRect(5, 23, 3, 2, ShoesGirl)
                        fillRect(10, 23, 3, 2, ShoesGirl)
                    }
                    else -> {
                        fillRect(5, 24, 3, 2, ShoesGirl)
                        fillRect(10, 24, 3, 2, ShoesGirl)
                    }
                }
            }
        } else {
            // Boy Pants & Legs
            val boyPants = if (isSnow) WinterPantsBoy else boyOutfit.pants
            val boyPantsFold = if (isSnow) WinterPantsBoy else boyOutfit.pantsFold
            val boyShoes = if (isSnow) WinterBootsBoy else boyOutfit.shoes
            val boyShoesSole = if (isSnow) WinterBootsBoySole else boyOutfit.shoesSole

            fillRect(6, 18, 7, 2, boyPants)
            px(9, 18, boyPantsFold)

            val walkCycle = char.walkFrame % 4
            when (char.pose) {
                CharacterPose.WALK_1, CharacterPose.WALK_2 -> {
                    fillRect(5, 20, 2, 4, boyPants)
                    fillRect(10, 19, 2, 4, boyPants)
                    fillRect(4, 24, 3, 1, boyShoes)
                    fillRect(4, 25, 4, 1, boyShoesSole)
                    fillRect(10, 23, 3, 1, boyShoes)
                    fillRect(10, 24, 4, 1, boyShoesSole)
                }
                CharacterPose.WALK_3, CharacterPose.WALK_4 -> {
                    fillRect(6, 19, 2, 4, boyPants)
                    fillRect(10, 20, 2, 4, boyPants)
                    fillRect(6, 23, 3, 1, boyShoes)
                    fillRect(6, 24, 4, 1, boyShoesSole)
                    fillRect(10, 24, 3, 1, boyShoes)
                    fillRect(10, 25, 4, 1, boyShoesSole)
                }
                CharacterPose.JOY_JUMP -> {
                    fillRect(6, 20, 2, 3, boyPants)
                    fillRect(10, 20, 2, 3, boyPants)
                    fillRect(5, 23, 3, 1, boyShoes)
                    fillRect(5, 24, 4, 1, boyShoesSole)
                    fillRect(10, 23, 3, 1, boyShoes)
                    fillRect(10, 24, 4, 1, boyShoesSole)
                }
                else -> {
                    fillRect(6, 20, 2, 4, boyPants)
                    fillRect(10, 20, 2, 4, boyPants)
                    fillRect(5, 24, 3, 1, boyShoes)
                    fillRect(5, 25, 4, 1, boyShoesSole)
                    fillRect(10, 24, 3, 1, boyShoes)
                    fillRect(10, 25, 4, 1, boyShoesSole)
                }
            }
        }
    }

    private fun drawSittingCharacter(
        scope: DrawScope,
        char: PixelCharacter,
        startX: Float,
        startY: Float,
        p: Float,
        flip: Boolean,
        isHoldingUmbrella: Boolean = false,
        isSnow: Boolean = false
    ) {
        val isGirl = char.isGirl
        val girlDress = getGirlDressPalette(char.outfitIndex)
        val boyOutfit = getBoyOutfitPalette(char.outfitIndex)
        val hairColor = if (isGirl) HairGirl else HairBoy
        val hairHigh = if (isGirl) HairGirlHighlight else HairBoyHighlight
        val sweaterColor = if (isSnow) {
            if (isGirl) WinterCoatGirl else WinterCoatBoy
        } else {
            if (isGirl) girlDress.sweater else boyOutfit.sweater
        }
        val pantsColor = if (isSnow) {
            if (isGirl) WinterTightsGirl else WinterPantsBoy
        } else {
            if (isGirl) girlDress.skirt else boyOutfit.pants
        }
        val shoesColor = if (isSnow) {
            if (isGirl) WinterBootsGirl else WinterBootsBoy
        } else {
            if (isGirl) ShoesGirl else boyOutfit.shoes
        }
        val skinColor = if (isGirl) SkinToneGirl else SkinToneBoy
        val skinShadow = if (isGirl) SkinToneGirlShadow else SkinToneBoyShadow
        val blushColor = if (isSnow) BlushRose else (if (char.emotion == CharacterEmotion.LOVING) BlushRose else BlushCoral)
        val handColor = if (isSnow) {
            if (isGirl) WinterMittensGirl else WinterGlovesBoy
        } else {
            skinColor
        }

        fun px(gridX: Int, gridY: Int, color: Color) {
            val actualX = if (flip) (17 - gridX) else gridX
            val breathY = if (gridY <= 16) -char.breathingOffset else 0f
            scope.drawRect(
                color = color,
                topLeft = Offset(startX + actualX * p, startY + gridY * p + breathY),
                size = Size(p, p)
            )
        }

        fun drawPixelRect(gx: Int, gy: Int, gw: Int, gh: Int, color: Color, breathY: Float) {
            val actualLeftX = if (flip) (17 - (gx + gw - 1)) else gx
            scope.drawRect(
                color = color,
                topLeft = Offset(startX + actualLeftX * p, startY + gy * p + breathY),
                size = Size(gw * p, gh * p)
            )
        }

        fun fillRect(gx: Int, gy: Int, gw: Int, gh: Int, color: Color) {
            if (gw <= 0 || gh <= 0) return
            val breath = -char.breathingOffset
            if (gy + gh <= 17) {
                drawPixelRect(gx, gy, gw, gh, color, breath)
            } else if (gy > 16) {
                drawPixelRect(gx, gy, gw, gh, color, 0f)
            } else {
                val topH = 17 - gy
                val bottomH = gh - topH
                drawPixelRect(gx, gy, gw, topH, color, breath)
                drawPixelRect(gx, 17, gw, bottomH, color, 0f)
            }
        }

        // Head and hair
        if (isSnow && !isGirl) {
            // Boy Beanie with pom-pom
            fillRect(8, 1, 2, 2, WinterPomPomBoy)
            fillRect(6, 3, 7, 3, WinterBeanieBoy)
            fillRect(5, 5, 9, 2, WinterBeanieBrimBoy)
            px(5, 6, hairColor)
            px(12, 6, hairColor)
        } else {
            fillRect(6, 3, 7, 2, hairColor)
            fillRect(5, 5, 9, 3, hairColor)
            if (isGirl) {
                fillRect(4, 6, 2, 9, hairColor)
                fillRect(12, 6, 2, 9, hairColor)
                fillRect(11, 4, 2, 2, girlDress.ribbon)
                px(12, 4, RibbonCenter)
                px(7, 4, hairHigh)
            } else {
                px(5, 4, hairColor)
                px(12, 4, hairColor)
                fillRect(6, 2, 5, 1, hairHigh)
            }
        }

        if (isSnow && isGirl) {
            // Girl Earmuffs
            fillRect(6, 3, 6, 1, WinterEarmuffsGirl)
            fillRect(3, 7, 2, 4, WinterEarmuffsFluff)
            fillRect(13, 7, 2, 4, WinterEarmuffsFluff)
            px(3, 8, Color.White)
            px(14, 8, Color.White)
        }

        // Face
        fillRect(6, 8, 7, 4, skinColor)
        if (char.isBlinking || char.pose == CharacterPose.SIT_SNUGGLE || char.emotion == CharacterEmotion.LOVING) {
            px(7, 9, EyeDark)
            px(8, 8, EyeDark)
            px(10, 8, EyeDark)
            px(11, 9, EyeDark)
        } else {
            fillRect(7, 9, 2, 2, EyeDark)
            fillRect(10, 9, 2, 2, EyeDark)
            px(7, 9, EyeSparkle)
            px(10, 9, EyeSparkle)
        }
        fillRect(5, 10, 2, 1, blushColor)
        fillRect(11, 10, 2, 1, blushColor)
        px(9, 11, blushColor)

        // Torso & sitting posture
        fillRect(6, 13, 7, 4, sweaterColor)
        fillRect(5, 14, 2, 3, sweaterColor)
        fillRect(11, 14, 2, 3, sweaterColor)

        // Hoodie details when sitting: rolled hood drape, drawstrings, and kangaroo pouch
        if (!isSnow) {
            val isHoodieOutfit = (!isGirl && boyOutfit.isHoodie) || (isGirl && (girlDress.isHoodie || char.outfitIndex >= 7))
            if (isHoodieOutfit) {
                val hoodColor = if (isGirl) girlDress.sweater else boyOutfit.sweater
                val hoodAccent = if (isGirl) girlDress.trim else boyOutfit.collar
                val hoodShadow = if (isGirl) girlDress.skirtShadow else boyOutfit.pantsFold
                val pouchHighlight = if (isGirl) girlDress.trim else boyOutfit.sweaterHighlight
                val pouchShadow = if (isGirl) girlDress.skirtShadow else boyOutfit.collar
                val cordColor = if (isGirl) {
                    Color.White.copy(alpha = 0.95f)
                } else {
                    if (char.outfitIndex == 1) boyOutfit.collar else Color.White.copy(alpha = 0.95f)
                }
                val cordTipColor = if (isGirl) girlDress.ribbon else (if (char.outfitIndex == 1) boyOutfit.pants else Color(0xFFFFD166))

                // Draped hood collar around neck and shoulders
                px(5, 12, hoodShadow)
                px(6, 12, hoodColor)
                fillRect(7, 12, 4, 1, hoodAccent)
                px(11, 12, hoodColor)
                px(12, 12, hoodShadow)

                // Drawstrings
                px(8, 13, cordColor)
                px(8, 14, cordTipColor)
                px(10, 13, cordColor)
                px(10, 14, cordTipColor)

                // Kangaroo pouch pocket
                fillRect(6, 15, 6, 2, hoodColor)
                fillRect(7, 15, 4, 1, pouchHighlight)
                px(6, 15, pouchShadow)
                px(11, 15, pouchShadow)
            }
        }

        if (isSnow) {
            if (isGirl) {
                // Scarf and fleece trim
                fillRect(6, 12, 7, 2, WinterScarfGirl)
                px(6, 13, WinterScarfGirlDark)
                px(12, 13, WinterScarfGirlDark)
                fillRect(10, 14, 2, 3, WinterScarfGirl)
                fillRect(5, 16, 9, 1, WinterCoatGirlFur)
            } else {
                fillRect(6, 12, 7, 2, WinterScarfBoy)
                px(6, 13, WinterScarfBoyDark)
                px(12, 13, WinterScarfBoyDark)
                fillRect(7, 14, 2, 3, WinterScarfBoy)
                px(9, 14, WinterPomPomBoy)
            }
        }

        if (isHoldingUmbrella && !isGirl) {
            // Boy sitting with raised arm reaching up and outward towards umbrella handle
            fillRect(12, 12, 2, 3, sweaterColor)
            fillRect(13, 11, 2, 2, sweaterColor)
            fillRect(15, 10, 2, 2, handColor)
            px(16, 10, skinShadow)
            // Left arm resting on lap
            fillRect(5, 14, 2, 3, sweaterColor)
            fillRect(6, 16, 2, 1, handColor)
        } else {
            // Hands resting on lap or holding hand
            fillRect(8, 16, 3, 1, handColor)
        }

        // Sitting folded legs
        fillRect(5, 17, 9, 3, pantsColor)
        if (isSnow && isGirl) {
            fillRect(5, 18, 8, 1, WinterBootsGirlFur)
        }
        fillRect(5, 19, 8, 2, shoesColor)
    }

    private fun drawSleepingCharacter(
        scope: DrawScope,
        char: PixelCharacter,
        startX: Float,
        startY: Float,
        p: Float,
        flip: Boolean,
        isSnow: Boolean = false
    ) {
        val isGirl = char.isGirl
        val girlDress = getGirlDressPalette(char.outfitIndex)
        val boyOutfit = getBoyOutfitPalette(char.outfitIndex)
        val hairColor = if (isGirl) HairGirl else HairBoy
        val sweaterColor = if (isSnow) {
            if (isGirl) WinterCoatGirl else WinterCoatBoy
        } else {
            if (isGirl) girlDress.sweater else boyOutfit.sweater
        }
        val pantsColor = if (isSnow) {
            if (isGirl) WinterTightsGirl else WinterPantsBoy
        } else {
            if (isGirl) girlDress.skirt else boyOutfit.pants
        }
        val shoesColor = if (isSnow) {
            if (isGirl) WinterBootsGirl else WinterBootsBoy
        } else {
            if (isGirl) ShoesGirl else boyOutfit.shoes
        }
        val skinColor = if (isGirl) SkinToneGirl else SkinToneBoy
        val blushColor = BlushCoral

        fun px(gridX: Int, gridY: Int, color: Color) {
            val actualX = if (flip) (17 - gridX) else gridX
            val breathY = if (gridY <= 15) -char.breathingOffset * 0.75f else 0f
            scope.drawRect(
                color = color,
                topLeft = Offset(startX + actualX * p, startY + gridY * p + breathY),
                size = Size(p, p)
            )
        }

        fun drawPixelRect(gx: Int, gy: Int, gw: Int, gh: Int, color: Color, breathY: Float) {
            val actualLeftX = if (flip) (17 - (gx + gw - 1)) else gx
            scope.drawRect(
                color = color,
                topLeft = Offset(startX + actualLeftX * p, startY + gy * p + breathY),
                size = Size(gw * p, gh * p)
            )
        }

        fun fillRect(gx: Int, gy: Int, gw: Int, gh: Int, color: Color) {
            if (gw <= 0 || gh <= 0) return
            val breath = -char.breathingOffset * 0.75f
            if (gy + gh <= 16) {
                drawPixelRect(gx, gy, gw, gh, color, breath)
            } else if (gy > 15) {
                drawPixelRect(gx, gy, gw, gh, color, 0f)
            } else {
                val topH = 16 - gy
                val bottomH = gh - topH
                drawPixelRect(gx, gy, gw, topH, color, breath)
                drawPixelRect(gx, 16, gw, bottomH, color, 0f)
            }
        }

        // Tilted cozy sleeping head
        fillRect(7, 4, 7, 4, hairColor)
        if (isGirl) {
            fillRect(5, 6, 3, 8, hairColor)
            fillRect(12, 5, 2, 2, RibbonGirl)
        }
        fillRect(8, 8, 6, 4, skinColor)

        // Sleeping closed eye lines: - -
        fillRect(9, 9, 2, 1, EyeDark)
        fillRect(12, 9, 2, 1, EyeDark)
        px(8, 10, blushColor)
        px(13, 10, blushColor)

        // Curled body
        fillRect(6, 12, 8, 5, sweaterColor)
        fillRect(5, 16, 9, 3, pantsColor)
        fillRect(5, 18, 6, 2, shoesColor)
    }

    private fun drawHuggingCharacter(
        scope: DrawScope,
        char: PixelCharacter,
        startX: Float,
        startY: Float,
        p: Float,
        flip: Boolean,
        isSnow: Boolean = false
    ) {
        val isGirl = char.isGirl
        val girlDress = getGirlDressPalette(char.outfitIndex)
        val boyOutfit = getBoyOutfitPalette(char.outfitIndex)
        val hairColor = if (isGirl) HairGirl else HairBoy
        val hairHigh = if (isGirl) HairGirlHighlight else HairBoyHighlight
        val sweaterColor = if (isSnow) {
            if (isGirl) WinterCoatGirl else WinterCoatBoy
        } else {
            if (isGirl) girlDress.sweater else boyOutfit.sweater
        }
        val pantsColor = if (isSnow) {
            if (isGirl) WinterTightsGirl else WinterPantsBoy
        } else {
            if (isGirl) girlDress.skirt else boyOutfit.pants
        }
        val shoesColor = if (isSnow) {
            if (isGirl) WinterBootsGirl else WinterBootsBoy
        } else {
            if (isGirl) ShoesGirl else boyOutfit.shoes
        }
        val skinColor = if (isGirl) SkinToneGirl else SkinToneBoy
        val handColor = if (isSnow) {
            if (isGirl) WinterMittensGirl else WinterGlovesBoy
        } else {
            skinColor
        }

        fun px(gridX: Int, gridY: Int, color: Color) {
            val actualX = if (flip) (17 - gridX) else gridX
            val breathY = if (gridY <= 17) -char.breathingOffset * 0.8f else 0f
            scope.drawRect(
                color = color,
                topLeft = Offset(startX + actualX * p, startY + gridY * p + breathY),
                size = Size(p, p)
            )
        }

        fun drawPixelRect(gx: Int, gy: Int, gw: Int, gh: Int, color: Color, breathY: Float) {
            val actualLeftX = if (flip) (17 - (gx + gw - 1)) else gx
            scope.drawRect(
                color = color,
                topLeft = Offset(startX + actualLeftX * p, startY + gy * p + breathY),
                size = Size(gw * p, gh * p)
            )
        }

        fun fillRect(gx: Int, gy: Int, gw: Int, gh: Int, color: Color) {
            if (gw <= 0 || gh <= 0) return
            val breath = -char.breathingOffset * 0.8f
            if (gy + gh <= 18) {
                drawPixelRect(gx, gy, gw, gh, color, breath)
            } else if (gy > 17) {
                drawPixelRect(gx, gy, gw, gh, color, 0f)
            } else {
                val topH = 18 - gy
                val bottomH = gh - topH
                drawPixelRect(gx, gy, gw, topH, color, breath)
                drawPixelRect(gx, 18, gw, bottomH, color, 0f)
            }
        }

        // Embracing pose leaning forward
        if (isSnow && !isGirl) {
            fillRect(8, 0, 2, 2, WinterPomPomBoy)
            fillRect(6, 2, 7, 3, WinterBeanieBoy)
            fillRect(5, 4, 8, 2, WinterBeanieBrimBoy)
        } else {
            fillRect(7, 2, 7, 3, hairColor)
            fillRect(6, 4, 8, 3, hairColor)
            if (isGirl) {
                fillRect(5, 5, 2, 10, hairColor)
                fillRect(12, 4, 2, 2, RibbonGirl)
                px(13, 4, RibbonCenter)
            } else {
                fillRect(7, 1, 5, 1, hairHigh)
            }
        }

        if (isSnow && isGirl) {
            fillRect(6, 2, 6, 1, WinterEarmuffsGirl)
            fillRect(3, 6, 2, 4, WinterEarmuffsFluff)
            fillRect(13, 6, 2, 4, WinterEarmuffsFluff)
        }

        // Peaceful closed blissful eyes
        fillRect(7, 7, 7, 4, skinColor)
        px(8, 8, EyeDark)
        px(9, 7, EyeDark)
        px(11, 7, EyeDark)
        px(12, 8, EyeDark)
        // Deep rosy blush
        fillRect(7, 9, 2, 1, BlushRose)
        fillRect(12, 9, 2, 1, BlushRose)

        // Arms wrapped tightly around partner
        fillRect(6, 12, 8, 6, sweaterColor)
        if (isSnow) {
            if (isGirl) {
                fillRect(6, 11, 7, 2, WinterScarfGirl)
            } else {
                fillRect(6, 11, 7, 2, WinterScarfBoy)
            }
        } else {
            val isHoodieOutfit = (!isGirl && boyOutfit.isHoodie) || (isGirl && (girlDress.isHoodie || char.outfitIndex >= 7))
            if (isHoodieOutfit) {
                val hoodAccent = if (isGirl) girlDress.trim else boyOutfit.collar
                val cordColor = if (isGirl) Color.White.copy(alpha = 0.95f) else (if (char.outfitIndex == 1) boyOutfit.collar else Color.White.copy(alpha = 0.95f))
                px(5, 11, hoodAccent)
                px(6, 11, hoodAccent)
                px(11, 11, hoodAccent)
                px(8, 13, cordColor)
                px(8, 14, cordColor)
            }
        }
        fillRect(12, 13, 4, 3, sweaterColor)
        fillRect(15, 14, 2, 2, handColor) // hands embracing back

        fillRect(6, 18, 7, 3, pantsColor)
        fillRect(5, 21, 3, 4, pantsColor)
        fillRect(9, 21, 3, 4, pantsColor)
        fillRect(4, 24, 4, 2, shoesColor)
        fillRect(9, 24, 4, 2, shoesColor)
    }

    private fun drawKissingCharacter(
        scope: DrawScope,
        char: PixelCharacter,
        startX: Float,
        startY: Float,
        p: Float,
        flip: Boolean,
        isSnow: Boolean = false
    ) {
        val isGirl = char.isGirl
        val girlDress = getGirlDressPalette(char.outfitIndex)
        val boyOutfit = getBoyOutfitPalette(char.outfitIndex)
        val hairColor = if (isGirl) HairGirl else HairBoy
        val hairHigh = if (isGirl) HairGirlHighlight else HairBoyHighlight
        val hairShadow = if (isGirl) HairGirlShadow else HairBoyShadow
        val sweaterColor = if (isSnow) {
            if (isGirl) WinterCoatGirl else WinterCoatBoy
        } else {
            if (isGirl) girlDress.sweater else boyOutfit.sweater
        }
        val pantsColor = if (isSnow) {
            if (isGirl) WinterTightsGirl else WinterPantsBoy
        } else {
            if (isGirl) girlDress.skirt else boyOutfit.pants
        }
        val shoesColor = if (isSnow) {
            if (isGirl) WinterBootsGirl else WinterBootsBoy
        } else {
            if (isGirl) ShoesGirl else boyOutfit.shoes
        }
        val skinColor = if (isGirl) SkinToneGirl else SkinToneBoy
        val handColor = if (isSnow) {
            if (isGirl) WinterMittensGirl else WinterGlovesBoy
        } else {
            skinColor
        }
        val lipKissColor = Color(0xFFFF3366)

        fun px(gridX: Int, gridY: Int, color: Color) {
            val actualX = if (flip) (17 - gridX) else gridX
            val breathY = if (gridY <= 17) -char.breathingOffset * 0.8f else 0f
            scope.drawRect(
                color = color,
                topLeft = Offset(startX + actualX * p, startY + gridY * p + breathY),
                size = Size(p, p)
            )
        }

        fun drawPixelRect(gx: Int, gy: Int, gw: Int, gh: Int, color: Color, breathY: Float) {
            val actualLeftX = if (flip) (17 - (gx + gw - 1)) else gx
            scope.drawRect(
                color = color,
                topLeft = Offset(startX + actualLeftX * p, startY + gy * p + breathY),
                size = Size(gw * p, gh * p)
            )
        }

        fun fillRect(gx: Int, gy: Int, gw: Int, gh: Int, color: Color) {
            if (gw <= 0 || gh <= 0) return
            val breath = -char.breathingOffset * 0.8f
            if (gy + gh <= 18) {
                drawPixelRect(gx, gy, gw, gh, color, breath)
            } else if (gy > 17) {
                drawPixelRect(gx, gy, gw, gh, color, 0f)
            } else {
                val topH = 18 - gy
                val bottomH = gh - topH
                drawPixelRect(gx, gy, gw, topH, color, breath)
                drawPixelRect(gx, 18, gw, bottomH, color, 0f)
            }
        }

        // Leaning head forward for tender kiss
        if (isSnow && !isGirl) {
            fillRect(9, 0, 2, 2, WinterPomPomBoy)
            fillRect(7, 2, 8, 3, WinterBeanieBoy)
            fillRect(6, 4, 9, 2, WinterBeanieBrimBoy)
        } else {
            fillRect(7, 2, 8, 3, hairColor)
            fillRect(6, 4, 9, 3, hairColor)
            if (isGirl) {
                fillRect(5, 5, 2, 11, hairColor)
                fillRect(4, 8, 2, 6, hairShadow)
                fillRect(13, 4, 2, 2, RibbonGirl)
                px(14, 4, RibbonCenter)
            } else {
                fillRect(8, 1, 5, 1, hairHigh)
                fillRect(9, 2, 4, 1, hairHigh)
            }
        }

        if (isSnow && isGirl) {
            fillRect(7, 2, 6, 1, WinterEarmuffsGirl)
            fillRect(4, 6, 2, 4, WinterEarmuffsFluff)
            fillRect(14, 6, 2, 4, WinterEarmuffsFluff)
        }

        // Face skin leaning forward
        fillRect(7, 7, 8, 4, skinColor)
        // Peaceful blissful closed eye lines: sweet curved ^ ^
        px(8, 8, EyeDark)
        px(9, 7, EyeDark)
        px(11, 7, EyeDark)
        px(12, 8, EyeDark)

        // Rosy blush glowing on cheek
        fillRect(8, 9, 2, 1, BlushRose)
        fillRect(12, 9, 2, 1, BlushRose)

        // Sweet puckered kiss lips extending forward right into partner!
        fillRect(14, 10, 2, 1, lipKissColor)
        px(15, 9, Color(0xFFFF758F))

        // Torso leaning forward
        fillRect(6, 12, 8, 6, sweaterColor)
        if (isSnow) {
            if (isGirl) {
                fillRect(6, 11, 7, 2, WinterScarfGirl)
            } else {
                fillRect(6, 11, 7, 2, WinterScarfBoy)
            }
        } else {
            val isHoodieOutfit = (!isGirl && boyOutfit.isHoodie) || (isGirl && (girlDress.isHoodie || char.outfitIndex >= 7))
            if (isHoodieOutfit) {
                val hoodAccent = if (isGirl) girlDress.trim else boyOutfit.collar
                val cordColor = if (isGirl) Color.White.copy(alpha = 0.95f) else (if (char.outfitIndex == 1) boyOutfit.collar else Color.White.copy(alpha = 0.95f))
                px(5, 11, hoodAccent)
                px(6, 11, hoodAccent)
                px(11, 11, hoodAccent)
                px(8, 13, cordColor)
                px(8, 14, cordColor)
            }
        }
        // Arm reaching forward and tenderly holding partner's cheek / shoulder
        fillRect(12, 13, 5, 2, sweaterColor)
        fillRect(16, 12, 2, 2, handColor) // hand cupping cheek/shoulder
        fillRect(5, 13, 2, 4, sweaterColor)

        // Lower body
        fillRect(6, 18, 7, 3, pantsColor)
        fillRect(5, 21, 3, 4, pantsColor)
        fillRect(9, 21, 3, 4, pantsColor)
        fillRect(4, 24, 4, 2, shoesColor)
        fillRect(9, 24, 4, 2, shoesColor)
    }

    /**
     * Accessories layered on top of character outfits (beanie, scarf, cap).
     * Rendered before drawGlasses so square glasses always stay in front.
     */
    fun drawAccessory(
        scope: DrawScope,
        char: PixelCharacter,
        startX: Float,
        startY: Float,
        p: Float,
        flip: Boolean
    ) {
        val breath = -char.breathingOffset

        fun px(gridX: Int, gridY: Int, color: Color) {
            val actualX = if (flip) (17 - gridX) else gridX
            val breathY = if (gridY <= 17) breath else 0f
            scope.drawRect(
                color = color,
                topLeft = Offset(startX + actualX * p, startY + gridY * p + breathY),
                size = Size(p, p)
            )
        }

        fun drawRect(gx: Int, gy: Int, gw: Int, gh: Int, color: Color) {
            val actualLeftX = if (flip) (17 - (gx + gw - 1)) else gx
            val breathY = if (gy <= 17) breath else 0f
            scope.drawRect(
                color = color,
                topLeft = Offset(startX + actualLeftX * p, startY + gy * p + breathY),
                size = Size(gw * p, gh * p)
            )
        }

        when (char.accessoryIndex) {
            1 -> {
                // Beanie (Cozy Ribbed Beanie): sits on crown gy 0..5
                val beanieColor = if (char.isGirl) Color(0xFFE8998D) else Color(0xFF264653)
                val beanieBrim = if (char.isGirl) Color(0xFFD6587A) else Color(0xFF1B4332)
                val beaniePom = if (char.isGirl) Color(0xFFFFF0F3) else Color(0xFFE9C46A)

                // Pom-pom on top
                drawRect(8, 0, 2, 2, beaniePom)
                // Beanie crown
                drawRect(6, 2, 7, 3, beanieColor)
                px(7, 3, beanieBrim)
                px(9, 3, beanieBrim)
                px(11, 3, beanieBrim)
                // Folded ribbed brim sitting right above brow/eyes (gy 4..5)
                drawRect(5, 4, 9, 2, beanieBrim)
            }
            2 -> {
                // Scarf (Warm Wool Fringe Scarf): wraps around neck gy 11..14
                val scarfColor = if (char.isGirl) Color(0xFFFFB5A7) else Color(0xFFE9C46A)
                val scarfDark = if (char.isGirl) Color(0xFFF78CA0) else Color(0xFFD4A373)
                val scarfFringe = if (char.isGirl) Color(0xFFFFF0F3) else Color(0xFF7F4F24)

                // Wrap around neck
                drawRect(6, 11, 7, 2, scarfColor)
                px(6, 12, scarfDark)
                px(12, 12, scarfDark)
                // Dangling fringe tail
                drawRect(10, 13, 2, 3, scarfColor)
                px(10, 15, scarfFringe)
                px(11, 15, scarfFringe)
            }
            3 -> {
                // Baseball Cap (Casual Streetwear Cap): sits on head gy 1..5, visor/brim forward at gy 5
                val capColor = if (char.isGirl) Color(0xFF6B9080) else Color(0xFF1D3557)
                val capBrimColor = if (char.isGirl) Color(0xFF4E6E60) else Color(0xFF0F172A)
                val capButton = if (char.isGirl) Color(0xFFFFF0F3) else Color(0xFFE63946)

                // Top button
                px(9, 1, capButton)
                // Cap crown
                drawRect(6, 2, 7, 3, capColor)
                // Curved forward visor / brim extending forward
                drawRect(6, 5, 8, 1, capColor)
                drawRect(11, 5, 4, 1, capBrimColor)
            }
        }
    }

    /**
     * Signature square-frame glasses accessory.
     * Renders consistently across standing, sitting, sleeping, hugging, and kissing poses.
     */
    fun drawGlasses(
        scope: DrawScope,
        char: PixelCharacter,
        startX: Float,
        startY: Float,
        p: Float,
        flip: Boolean
    ) {
        val breath = -char.breathingOffset

        fun px(gridX: Int, gridY: Int, color: Color) {
            val actualX = if (flip) (17 - gridX) else gridX
            val breathY = if (gridY <= 17) breath else 0f
            scope.drawRect(
                color = color,
                topLeft = Offset(startX + actualX * p, startY + gridY * p + breathY),
                size = Size(p, p)
            )
        }

        fun drawRect(gx: Int, gy: Int, gw: Int, gh: Int, color: Color) {
            val actualLeftX = if (flip) (17 - (gx + gw - 1)) else gx
            val breathY = if (gy <= 17) breath else 0f
            scope.drawRect(
                color = color,
                topLeft = Offset(startX + actualLeftX * p, startY + gy * p + breathY),
                size = Size(gw * p, gh * p)
            )
        }

        when (char.pose) {
            CharacterPose.SIT, CharacterPose.SIT_SNUGGLE -> {
                // Sitting head: eyes at (7..8, 9) and (10..11, 9)
                // Left square frame around left eye
                drawRect(6, 8, 4, 1, GlassesFrame)
                drawRect(6, 11, 4, 1, GlassesFrame)
                drawRect(6, 8, 1, 4, GlassesFrame)
                drawRect(9, 8, 1, 4, GlassesFrame)
                // Right square frame around right eye
                drawRect(10, 8, 4, 1, GlassesFrame)
                drawRect(10, 11, 4, 1, GlassesFrame)
                drawRect(10, 8, 1, 4, GlassesFrame)
                drawRect(13, 8, 1, 4, GlassesFrame)
                // Nose bridge
                px(9, 9, GlassesFrameLight)
                px(10, 9, GlassesFrameLight)
                // Temple arm back to ear
                px(5, 9, GlassesFrame)
                // Soft subtle glints inside lens
                px(7, 9, GlassesGlint)
                px(11, 9, GlassesGlint)
            }
            CharacterPose.SLEEP, CharacterPose.SLEEP_YAWN -> {
                // Tilted sleeping head: eyes at (9..10, 9) and (12..13, 9)
                // Left square frame
                drawRect(8, 8, 4, 1, GlassesFrame)
                drawRect(8, 10, 4, 1, GlassesFrame)
                drawRect(8, 8, 1, 3, GlassesFrame)
                drawRect(11, 8, 1, 3, GlassesFrame)
                // Right square frame
                drawRect(11, 8, 4, 1, GlassesFrame)
                drawRect(11, 10, 4, 1, GlassesFrame)
                drawRect(14, 8, 1, 3, GlassesFrame)
                // Nose bridge
                px(11, 9, GlassesFrameLight)
                // Temple arm
                px(7, 9, GlassesFrame)
            }
            CharacterPose.HUG, CharacterPose.KISS -> {
                // Leaning forward: eyes at (8..9, 7..8) and (11..12, 7..8)
                // Left square frame
                drawRect(7, 7, 4, 1, GlassesFrame)
                drawRect(7, 9, 4, 1, GlassesFrame)
                drawRect(7, 7, 1, 3, GlassesFrame)
                drawRect(10, 7, 1, 3, GlassesFrame)
                // Right square frame
                drawRect(10, 7, 4, 1, GlassesFrame)
                drawRect(10, 9, 4, 1, GlassesFrame)
                drawRect(13, 7, 1, 3, GlassesFrame)
                // Nose bridge
                px(10, 7, GlassesFrameLight)
                // Temple arm
                px(6, 8, GlassesFrame)
                // Soft glint
                px(8, 8, GlassesGlint)
                px(11, 8, GlassesGlint)
            }
            else -> {
                // Standing / default poses: eyes at (7..8, 8) and (10..11, 8)
                // Left square frame around left eye
                drawRect(6, 7, 4, 1, GlassesFrame)
                drawRect(6, 10, 4, 1, GlassesFrame)
                drawRect(6, 7, 1, 4, GlassesFrame)
                drawRect(9, 7, 1, 4, GlassesFrame)
                // Right square frame around right eye
                drawRect(10, 7, 4, 1, GlassesFrame)
                drawRect(10, 10, 4, 1, GlassesFrame)
                drawRect(10, 7, 1, 4, GlassesFrame)
                drawRect(13, 7, 1, 4, GlassesFrame)
                // Nose bridge
                px(9, 8, GlassesFrameLight)
                px(10, 8, GlassesFrameLight)
                // Temple arm back to ear
                px(5, 8, GlassesFrame)
                // Soft subtle glints inside lens
                px(7, 8, GlassesGlint)
                px(10, 8, GlassesGlint)
            }
        }
    }

    fun drawEarphoneOnHead(
        scope: DrawScope,
        char: PixelCharacter,
        startX: Float,
        startY: Float,
        p: Float,
        flip: Boolean
    ) {
        val isGirl = char.isGirl
        // Ear position on head:
        // Boy facing right (!flip): inner ear on right side (gridX = 11)
        // Girl facing left (flip): inner ear on left side (17 - 11 = 6)
        val earX = if (!isGirl) {
            if (!flip) 11 else 6
        } else {
            if (flip) 6 else 11
        }
        val earY = 8
        val actualX = startX + earX * p
        val actualY = startY + earY * p

        val earbudColor = Color.White
        val accentColor = if (isGirl) Color(0xFFFF6B8B) else Color(0xFF64B5F6)
        val shineColor = Color(0xFFFFF0F5)

        // Cute rounded 3x3 pixel earbud casing
        scope.drawRect(earbudColor, Offset(actualX, actualY), Size(2.8f * p, 2.8f * p))
        // Inner accent dot (soft pink for girl, soft sky blue for boy)
        scope.drawRect(accentColor, Offset(actualX + 0.6f * p, actualY + 0.6f * p), Size(1.6f * p, 1.6f * p))
        // Shiny catchlight
        scope.drawRect(shineColor, Offset(actualX + 0.3f * p, actualY + 0.3f * p), Size(0.8f * p, 0.8f * p))

        // Cute downward wire stem (1px wide, angled softly toward center)
        val stemOffsetX = if (!isGirl) 0.5f * p else 1.3f * p
        scope.drawRect(earbudColor, Offset(actualX + stemOffsetX, actualY + 2.8f * p), Size(1.2f * p, 2.5f * p))
    }

    fun getEarphoneAttachmentOffset(
        char: PixelCharacter,
        centerX: Float,
        bottomY: Float,
        p: Float
    ): Offset {
        val charWidth = 18 * p
        val charHeight = 26 * p
        val startX = centerX - charWidth / 2f + char.idleSwayOffset
        val poseOffsetY = when (char.pose) {
            CharacterPose.SIT, CharacterPose.SIT_SNUGGLE -> 5 * p
            CharacterPose.SLEEP, CharacterPose.SLEEP_YAWN -> 6 * p
            else -> 0f
        }
        val startY = bottomY - charHeight - char.bounceOffset - char.breathingOffset + poseOffsetY
        val flip = (char.direction == Direction.LEFT)
        val isGirl = char.isGirl

        val earX = if (!isGirl) {
            if (!flip) 11 else 6
        } else {
            if (flip) 6 else 11
        }
        val stemOffsetX = if (!isGirl) 1.1f * p else 1.9f * p
        val actualX = startX + earX * p + stemOffsetX
        val actualY = startY + 13.3f * p

        return Offset(actualX, actualY)
    }

    fun drawEarphoneCord(
        scope: DrawScope,
        startOffset: Offset,
        endOffset: Offset,
        p: Float,
        timeSeconds: Float
    ) {
        val bx = startOffset.x
        val by = startOffset.y
        val gx = endOffset.x
        val gy = endOffset.y

        val midX = (bx + gx) / 2f
        val dist = kotlin.math.abs(gx - bx)
        val dip = (dist * 0.22f).coerceIn(4f * p, 10f * p)
        val sway = kotlin.math.sin(timeSeconds * 3.5f) * (0.8f * p)
        val dropY = kotlin.math.max(by, gy) + dip + sway

        // Draw delicate smooth catenary curve wire using segmented lines
        val steps = 20
        var prevX = bx
        var prevY = by
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val cx = (1 - t) * (1 - t) * bx + 2 * (1 - t) * t * midX + t * t * gx
            val cy = (1 - t) * (1 - t) * by + 2 * (1 - t) * t * dropY + t * t * gy
            scope.drawLine(
                color = Color(0xFFFFF0F5),
                start = Offset(prevX, prevY),
                end = Offset(cx, cy),
                strokeWidth = 1.4f * p
            )
            prevX = cx
            prevY = cy
        }

        // Precious 5x5 pixel heart clip right in the center!
        val heartCenterX = midX
        val heartCenterY = (by + gy) / 2f + dip * 0.75f + sway * 0.75f
        val heartColor = Color(0xFFFF3366)
        val shineColor = Color(0xFFFFFFFF)

        fun hp(relX: Int, relY: Int, col: Color) {
            scope.drawRect(col, Offset(heartCenterX + relX * p, heartCenterY + relY * p), Size(p, p))
        }

        // Row -2: . # . # .
        hp(-1, -2, heartColor)
        hp(1, -2, heartColor)

        // Row -1: # # # # #
        hp(-2, -1, heartColor)
        hp(-1, -1, shineColor) // cute shine dot
        hp(0, -1, heartColor)
        hp(1, -1, heartColor)
        hp(2, -1, heartColor)

        // Row 0:  # # # # #
        hp(-2, 0, heartColor)
        hp(-1, 0, heartColor)
        hp(0, 0, heartColor)
        hp(1, 0, heartColor)
        hp(2, 0, heartColor)

        // Row 1:  . # # # .
        hp(-1, 1, heartColor)
        hp(0, 1, heartColor)
        hp(1, 1, heartColor)

        // Row 2:  . . # . .
        hp(0, 2, heartColor)
    }

    // Overload for legacy callers
    fun drawEarphoneCord(
        scope: DrawScope,
        boyHeadX: Float,
        boyHeadY: Float,
        girlHeadX: Float,
        girlHeadY: Float,
        p: Float,
        timeSeconds: Float
    ) {
        drawEarphoneCord(
            scope = scope,
            startOffset = Offset(boyHeadX, boyHeadY),
            endOffset = Offset(girlHeadX, girlHeadY),
            p = p,
            timeSeconds = timeSeconds
        )
    }

    private fun drawEmoteBubble(
        scope: DrawScope,
        emote: EmoteType,
        cx: Float,
        topY: Float,
        p: Float
    ) {
        val bw = 14 * p
        val bh = 11 * p
        val bx = cx - bw / 2f
        val by = topY

        // Retro pixel bubble outline
        scope.drawRect(Color(0xFF2B2D42), Offset(bx - p, by - p), Size(bw + 2 * p, bh + 2 * p))
        scope.drawRect(Color.White, Offset(bx, by), Size(bw, bh))
        // Bubble tail pointing downward to character
        scope.drawRect(Color(0xFF2B2D42), Offset(cx - 2 * p, by + bh), Size(4 * p, 2 * p))
        scope.drawRect(Color.White, Offset(cx - p, by + bh - p), Size(2 * p, 2 * p))

        // Emote icon inside
        when (emote) {
            EmoteType.HEART, EmoteType.KISS -> {
                val red = Color(0xFFFF3366)
                scope.drawRect(red, Offset(cx - 4 * p, by + 2 * p), Size(3 * p, 2 * p))
                scope.drawRect(red, Offset(cx + 1 * p, by + 2 * p), Size(3 * p, 2 * p))
                scope.drawRect(red, Offset(cx - 5 * p, by + 4 * p), Size(10 * p, 2 * p))
                scope.drawRect(red, Offset(cx - 4 * p, by + 6 * p), Size(8 * p, 2 * p))
                scope.drawRect(red, Offset(cx - 2 * p, by + 8 * p), Size(4 * p, 1.5f * p))
                scope.drawRect(red, Offset(cx - 1 * p, by + 9.5f * p), Size(2 * p, 1 * p))
            }
            EmoteType.EXCLAMATION -> {
                val orange = Color(0xFFFF5722)
                scope.drawRect(orange, Offset(cx - 1.5f * p, by + 2 * p), Size(3 * p, 5 * p))
                scope.drawRect(orange, Offset(cx - 1.5f * p, by + 8 * p), Size(3 * p, 2 * p))
            }
            EmoteType.SWEAT -> {
                val blue = Color(0xFF48CAE4)
                scope.drawRect(blue, Offset(cx - 1 * p, by + 3 * p), Size(2 * p, 2 * p))
                scope.drawRect(blue, Offset(cx - 2 * p, by + 5 * p), Size(4 * p, 4 * p))
            }
            EmoteType.BLUSH -> {
                val pink = Color(0xFFFF758F)
                scope.drawRect(pink, Offset(cx - 4 * p, by + 4 * p), Size(3 * p, 3 * p))
                scope.drawRect(pink, Offset(cx + 1 * p, by + 4 * p), Size(3 * p, 3 * p))
            }
            EmoteType.MUSIC_NOTE -> {
                val note = Color(0xFF4361EE)
                scope.drawRect(note, Offset(cx - 3 * p, by + 6 * p), Size(3 * p, 2 * p))
                scope.drawRect(note, Offset(cx + 1 * p, by + 4 * p), Size(3 * p, 2 * p))
                scope.drawRect(note, Offset(cx - p, by + 3 * p), Size(1.5f * p, 4 * p))
                scope.drawRect(note, Offset(cx + 3 * p, by + 2 * p), Size(1.5f * p, 3 * p))
                scope.drawRect(note, Offset(cx - p, by + 2 * p), Size(5 * p, 1.5f * p))
            }
            EmoteType.QUESTION -> {
                val dark = Color(0xFF2B2D42)
                scope.drawRect(dark, Offset(cx - 3 * p, by + 2 * p), Size(6 * p, 2 * p))
                scope.drawRect(dark, Offset(cx + 2 * p, by + 4 * p), Size(2 * p, 2 * p))
                scope.drawRect(dark, Offset(cx - 1 * p, by + 5 * p), Size(3 * p, 2 * p))
                scope.drawRect(dark, Offset(cx - 1 * p, by + 8 * p), Size(2 * p, 2 * p))
            }
            EmoteType.DOTS -> {
                val dark = Color(0xFF4A4E69)
                scope.drawRect(dark, Offset(cx - 4 * p, by + 5 * p), Size(2 * p, 2 * p))
                scope.drawRect(dark, Offset(cx - 1 * p, by + 5 * p), Size(2 * p, 2 * p))
                scope.drawRect(dark, Offset(cx + 2 * p, by + 5 * p), Size(2 * p, 2 * p))
            }
            EmoteType.SPARKLE -> {
                val gold = Color(0xFFFFD166)
                scope.drawRect(gold, Offset(cx - 0.5f * p, by + 2 * p), Size(1.5f * p, 7 * p))
                scope.drawRect(gold, Offset(cx - 3 * p, by + 4.5f * p), Size(7 * p, 1.5f * p))
            }
            EmoteType.SLEEP_Z -> {
                val blue = Color(0xFF90E0EF)
                scope.drawRect(blue, Offset(cx - 3 * p, by + 3 * p), Size(6 * p, 1.5f * p))
                scope.drawRect(blue, Offset(cx - p, by + 4.5f * p), Size(2 * p, 2 * p))
                scope.drawRect(blue, Offset(cx - 3 * p, by + 6.5f * p), Size(6 * p, 1.5f * p))
            }
            EmoteType.NONE -> {}
        }
    }
}
