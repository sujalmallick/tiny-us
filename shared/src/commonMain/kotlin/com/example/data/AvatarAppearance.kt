package com.example.data

/**
 * How one partner's pixel character looks. Deliberately independent of which character slot
 * (`boy`/`girl` internally) the person occupies, so any couple can look like themselves.
 *
 * Colours are stored as palette indices so saves stay tiny and both platforms share one palette.
 */
data class AvatarAppearance(
    val skinTone: Int,
    val hairColor: Int,
    val longHair: Boolean,
    val wearsDress: Boolean
) {
    /** Clamps indices so an old or hand-edited save can never crash rendering. */
    fun normalized(): AvatarAppearance = copy(
        skinTone = skinTone.coerceIn(0, AvatarPalette.skinTones.lastIndex),
        hairColor = hairColor.coerceIn(0, AvatarPalette.hairColors.lastIndex)
    )

    companion object {
        /** Slot A (internally "boy") — matches the look shipped before the customizer existed. */
        val DEFAULT_A = AvatarAppearance(skinTone = 1, hairColor = 0, longHair = false, wearsDress = false)

        /** Slot B (internally "girl") — matches the look shipped before the customizer existed. */
        val DEFAULT_B = AvatarAppearance(skinTone = 3, hairColor = 1, longHair = true, wearsDress = true)

        fun defaultFor(isSlotB: Boolean): AvatarAppearance = if (isSlotB) DEFAULT_B else DEFAULT_A
    }
}

/** A named colour with a darker shade, as 0xAARRGGBB. */
data class AvatarSwatch(val name: String, val base: Long, val shadow: Long, val highlight: Long = base)

object AvatarPalette {
    /** Ordered light to deep. Indices 1 and 3 are the original two tones and must not move. */
    val skinTones: List<AvatarSwatch> = listOf(
        AvatarSwatch("Porcelain", 0xFFFFE8D6, 0xFFF2CDB4),
        AvatarSwatch("Peach", 0xFFFFDFBA, 0xFFF5CB9F),
        AvatarSwatch("Honey", 0xFFF1C27D, 0xFFDDA766),
        AvatarSwatch("Tan", 0xFFD49B7A, 0xFFB87D5C),
        AvatarSwatch("Caramel", 0xFFB9784F, 0xFF9A5E3B),
        AvatarSwatch("Cocoa", 0xFF8D5524, 0xFF704219),
        AvatarSwatch("Espresso", 0xFF5C3A21, 0xFF472B17),
        AvatarSwatch("Deep Umber", 0xFF3F2716, 0xFF2E1C10)
    )

    /** Indices 0 and 1 are the original two hair colours and must not move. */
    val hairColors: List<AvatarSwatch> = listOf(
        AvatarSwatch("Soft Black", 0xFF2A2829, 0xFF1B191A, 0xFF484240),
        AvatarSwatch("Chestnut", 0xFF6A381F, 0xFF492413, 0xFF93532C),
        AvatarSwatch("Jet", 0xFF141418, 0xFF0A0A0C, 0xFF2E2E3A),
        AvatarSwatch("Honey Blonde", 0xFFE0B868, 0xFFB88F45, 0xFFF2D492),
        AvatarSwatch("Copper", 0xFFB5532A, 0xFF8A3B1B, 0xFFD4733F),
        AvatarSwatch("Silver", 0xFFB8B8C0, 0xFF8E8E99, 0xFFDCDCE4),
        AvatarSwatch("Rose", 0xFFE88AA8, 0xFFC2647F, 0xFFF5B3C8),
        AvatarSwatch("Ocean", 0xFF4A6FA5, 0xFF34507A, 0xFF6E92C4)
    )
}
