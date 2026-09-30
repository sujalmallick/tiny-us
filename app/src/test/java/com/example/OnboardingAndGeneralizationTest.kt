package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import com.example.data.ProfileManager
import com.example.data.RelationshipTimeManager
import com.example.data.SpecialCalendarManager
import com.example.data.SpecialMemoryType
import com.example.engine.AmbientAudio
import com.example.scene.SceneEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingAndGeneralizationTest {

    @Test
    fun `test onboarding initial state is false and transitions to completed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)

        // Reset to clean test state
        prefs.isOnboardingCompleted = false
        assertFalse("New installation must have onboarding incomplete", prefs.isOnboardingCompleted)

        // Complete onboarding
        prefs.boyfriendName = "Liam"
        prefs.girlfriendName = "Emma"
        prefs.anniversaryDate = "2023-05-14"
        prefs.secretCode = "FOREVER"
        prefs.secretCodeBody = "Our special private world."
        prefs.isOnboardingCompleted = true

        assertTrue("After onboarding, isOnboardingCompleted must be true", prefs.isOnboardingCompleted)
        assertEquals("Liam", prefs.boyfriendName)
        assertEquals("Emma", prefs.girlfriendName)
        assertEquals("2023-05-14", prefs.anniversaryDate)
        assertEquals("FOREVER", prefs.secretCode)
        assertEquals("Our special private world.", prefs.secretCodeBody)
    }

    @Test
    fun `test name blank fallback and trimming logic`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)

        // Empty string should fall back to profile default ("Him" / "Her")
        prefs.boyfriendName = "   "
        prefs.girlfriendName = ""
        assertEquals("Him", prefs.boyfriendName)
        assertEquals("Her", prefs.girlfriendName)

        // Length restriction
        val longName = "MaximilianBartholomew"
        val trimmed = longName.take(10)
        prefs.boyfriendName = trimmed
        assertEquals(10, prefs.boyfriendName.length)
        assertEquals("Maximilian", prefs.boyfriendName)
    }

    @Test
    fun `test date validation with today and future date`() {
        val originalDate = RelationshipTimeManager.relationshipStartDate
        try {
            val today = LocalDate.now()

            // 1. Anniversary is today -> Day 1, 0 years elapsed
            RelationshipTimeManager.relationshipStartDate = today
            assertEquals("Anniversary today must be Day 1", 1L, RelationshipTimeManager.calculateTinyUsDay(today))
            val durationToday = RelationshipTimeManager.getExactDuration()
            assertEquals("Total days must be 1", 1L, durationToday.totalDays)
            assertEquals("Years elapsed must be 0", 0, durationToday.years)

            // 2. Future anniversary -> safely clamped to Day 1
            val future = today.plusDays(30)
            RelationshipTimeManager.relationshipStartDate = future
            assertEquals("Future anniversary must clamp to Day 1", 1L, RelationshipTimeManager.calculateTinyUsDay(today))
            val durationFuture = RelationshipTimeManager.getExactDuration()
            assertEquals("Future anniversary total days must be 1", 1L, durationFuture.totalDays)
            assertEquals("Future anniversary years must be 0", 0, durationFuture.years)

            // 3. Tree moss calculation with today / future -> Stage 0 (no moss)
            val audio = AmbientAudio().apply { isEnabled = false }
            val engine = SceneEngine(audio = audio, onOpenLoveNotes = {}, onOpenMemories = {})
            assertEquals("Tree moss for today's anniversary must be Stage 0", 0, engine.computeTreeMossGrowthStage(today))
        } finally {
            RelationshipTimeManager.relationshipStartDate = originalDate
        }
    }

    @Test
    fun `test scene engine initials propagation from onboarding names`() {
        val audio = AmbientAudio().apply { isEnabled = false }
        val engine = SceneEngine(audio = audio, onOpenLoveNotes = {}, onOpenMemories = {})

        engine.updateNames("Lucas", "Sophia")
        assertEquals("Lucas", engine.boy.name)
        assertEquals("Sophia", engine.girl.name)
        assertEquals('L', engine.boyfriendInitial)
        assertEquals('S', engine.girlfriendInitial)
    }

    @Test
    fun `test special calendar manager dynamically reflects birthdays and anniversary`() {
        val originalStart = RelationshipTimeManager.relationshipStartDate
        val originalCustom = SpecialCalendarManager.customMemories
        val originalBoyBday = SpecialCalendarManager.boyBirthday
        val originalGirlBday = SpecialCalendarManager.girlBirthday
        try {
            val testAnniv = LocalDate.of(2022, 6, 15)
            RelationshipTimeManager.relationshipStartDate = testAnniv
            SpecialCalendarManager.customMemories = null
            SpecialCalendarManager.boyName = "Noah"
            SpecialCalendarManager.girlName = "Mia"
            SpecialCalendarManager.boyBirthday = LocalDate.of(2000, 3, 20)
            SpecialCalendarManager.girlBirthday = LocalDate.of(2001, 8, 10)

            val memories = SpecialCalendarManager.fixedMemories
            assertEquals(3, memories.size)

            val beginning = memories.first { it.id == "our_beginning" }
            assertEquals("Our Beginning", beginning.title)
            assertEquals(testAnniv, beginning.date)
            assertEquals(SpecialMemoryType.RELATIONSHIP, beginning.type)

            val boyBday = memories.first { it.id == "boy_birthday" }
            assertEquals("Noah's Birthday", boyBday.title)
            assertEquals(LocalDate.of(2000, 3, 20), boyBday.date)
            assertEquals(SpecialMemoryType.BIRTHDAY, boyBday.type)
            assertTrue(boyBday.annualRecurring)

            val girlBday = memories.first { it.id == "girl_birthday" }
            assertEquals("Mia's Birthday", girlBday.title)
            assertEquals(LocalDate.of(2001, 8, 10), girlBday.date)
            assertEquals(SpecialMemoryType.BIRTHDAY, girlBday.type)
            assertTrue(girlBday.annualRecurring)
        } finally {
            RelationshipTimeManager.relationshipStartDate = originalStart
            SpecialCalendarManager.customMemories = originalCustom
            SpecialCalendarManager.boyBirthday = originalBoyBday
            SpecialCalendarManager.girlBirthday = originalGirlBday
        }
    }

    @Test
    fun `test personal profile safeguard prevents auto-loading generic JSON files`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val targetFile = File(context.filesDir, "personal_profile.json")

        // 1. JSON without isPersonalProfile flag or schemaVersion should be rejected
        targetFile.writeText("""
            {
              "boyName": "Intruder",
              "girlName": "Unknown"
            }
        """.trimIndent())

        val rejected = ProfileManager.loadFromLocalFile(context)
        assertFalse("JSON missing isPersonalProfile and schemaVersion must be rejected by safeguard", rejected)

        // 2. Valid JSON with explicit isPersonalProfile flag and schemaVersion is accepted
        targetFile.writeText("""
            {
              "isPersonalProfile": true,
              "schemaVersion": 1,
              "boyName": "Partner1",
              "girlName": "Partner2",
              "anniversaryDate": "2023-01-01"
            }
        """.trimIndent())

        val accepted = ProfileManager.loadFromLocalFile(context)
        assertTrue("Valid personal profile with safeguard tokens must be accepted", accepted)
        assertEquals("Partner1", ProfileManager.getProfile().boyName)
        assertEquals("Partner2", ProfileManager.getProfile().girlName)
        assertEquals(LocalDate.of(2023, 1, 1), ProfileManager.getProfile().anniversaryDate)

        // Clean up test file
        targetFile.delete()
    }
}
