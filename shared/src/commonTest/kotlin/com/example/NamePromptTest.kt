package com.example

import com.example.data.PersonalProfile
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NamePromptTest {
    @Test
    fun offersOnceForOldHimAndHerDefaults() {
        assertTrue(PersonalProfile.shouldOfferNamePrompt("Him", "Her", onboardingCompleted = true, alreadyAsked = false))
        assertTrue(PersonalProfile.shouldOfferNamePrompt("Sam", "Her", onboardingCompleted = true, alreadyAsked = false))
        assertFalse(PersonalProfile.shouldOfferNamePrompt("Him", "Her", onboardingCompleted = true, alreadyAsked = true))
    }

    @Test
    fun neverOffersForChosenOrNewDefaultNamesOrBeforeSetup() {
        assertFalse(PersonalProfile.shouldOfferNamePrompt("Sam", "Alex", onboardingCompleted = true, alreadyAsked = false))
        assertFalse(PersonalProfile.shouldOfferNamePrompt(PersonalProfile.DEFAULT_NAME_A, PersonalProfile.DEFAULT_NAME_B, onboardingCompleted = true, alreadyAsked = false))
        assertFalse(PersonalProfile.shouldOfferNamePrompt("Him", "Her", onboardingCompleted = false, alreadyAsked = false))
    }
}
