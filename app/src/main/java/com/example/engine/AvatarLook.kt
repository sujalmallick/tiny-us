package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.data.AvatarAppearance
import com.example.data.AvatarPalette

/**
 * [AvatarAppearance] resolved to Compose colours once, so renderers read plain fields every frame
 * instead of allocating colours or looking up palettes.
 */
class AvatarLook private constructor(val appearance: AvatarAppearance) {
    private val skinSwatch = AvatarPalette.skinTones[appearance.skinTone]
    private val hairSwatch = AvatarPalette.hairColors[appearance.hairColor]

    val skin = Color(skinSwatch.base)
    val skinShadow = Color(skinSwatch.shadow)
    val hair = Color(hairSwatch.base)
    val hairHighlight = Color(hairSwatch.highlight)
    val hairShadow = Color(hairSwatch.shadow)
    val longHair: Boolean get() = appearance.longHair
    val wearsDress: Boolean get() = appearance.wearsDress

    companion object {
        val DEFAULT_A = AvatarLook(AvatarAppearance.DEFAULT_A)
        val DEFAULT_B = AvatarLook(AvatarAppearance.DEFAULT_B)

        fun defaultFor(isSlotB: Boolean): AvatarLook = if (isSlotB) DEFAULT_B else DEFAULT_A

        fun of(appearance: AvatarAppearance): AvatarLook = when (val a = appearance.normalized()) {
            AvatarAppearance.DEFAULT_A -> DEFAULT_A
            AvatarAppearance.DEFAULT_B -> DEFAULT_B
            else -> AvatarLook(a)
        }
    }
}
