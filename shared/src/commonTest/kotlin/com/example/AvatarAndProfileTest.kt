package com.example

import com.example.data.AvatarAppearance
import com.example.data.AvatarPalette
import com.example.data.PersonalProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AvatarAndProfileTest {
    @Test
    fun defaultLooksKeepTheOriginalColours() {
        // Existing users must see exactly the characters they had before the customizer.
        assertEquals(0xFFFFDFBA, AvatarPalette.skinTones[AvatarAppearance.DEFAULT_A.skinTone].base)
        assertEquals(0xFFD49B7A, AvatarPalette.skinTones[AvatarAppearance.DEFAULT_B.skinTone].base)
        assertEquals(0xFF2A2829, AvatarPalette.hairColors[AvatarAppearance.DEFAULT_A.hairColor].base)
        assertEquals(0xFF6A381F, AvatarPalette.hairColors[AvatarAppearance.DEFAULT_B.hairColor].base)
        assertFalse(AvatarAppearance.DEFAULT_A.longHair)
        assertTrue(AvatarAppearance.DEFAULT_B.wearsDress)
    }

    @Test
    fun normalizedClampsOutOfRangeIndices() {
        val broken = AvatarAppearance(skinTone = 99, hairColor = -4, longHair = true, wearsDress = false)
        val fixed = broken.normalized()
        assertEquals(AvatarPalette.skinTones.lastIndex, fixed.skinTone)
        assertEquals(0, fixed.hairColor)
        assertTrue(fixed.longHair)
    }

    @Test
    fun slotDefaultsAreIndependentOfStyle() {
        assertEquals(AvatarAppearance.DEFAULT_B, AvatarAppearance.defaultFor(isSlotB = true))
        assertEquals(AvatarAppearance.DEFAULT_A, AvatarAppearance.defaultFor(isSlotB = false))
    }

    @Test
    fun placeholderNamesIncludeLegacyDefaults() {
        assertTrue(PersonalProfile.isPlaceholderName(""))
        assertTrue(PersonalProfile.isPlaceholderName("Him"))
        assertTrue(PersonalProfile.isPlaceholderName("Her"))
        assertTrue(PersonalProfile.isPlaceholderName(PersonalProfile.DEFAULT_NAME_A))
        assertFalse(PersonalProfile.isPlaceholderName("Alex"))
        assertEquals(PersonalProfile.DEFAULT_NAME_A, PersonalProfile().boyName)
    }
}
