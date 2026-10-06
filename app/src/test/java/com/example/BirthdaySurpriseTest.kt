package com.example

import com.example.data.BirthdayStore
import com.example.data.CoupleDates
import com.example.data.InMemoryKeyValueStorage
import com.example.data.LetterKind
import com.example.data.Partner
import com.example.engine.AmbientAudio
import com.example.engine.HeldItem
import com.example.progress.ProgressEvent
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.SurpriseStep
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Plan 09, A: the birthday surprise, beat by beat, in the real engine. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BirthdaySurpriseTest {

    private lateinit var engine: SceneEngine
    private lateinit var store: BirthdayStore
    private val events = mutableListOf<ProgressEvent>()
    private val cw = 1080f
    private val ch = 2400f
    private val today: LocalDate = CoupleDates.today()

    @Before
    fun setup() {
        engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {})
        engine.weatherDriftEnabled = false
        engine.onProgress = { events += it }
        store = BirthdayStore(InMemoryKeyValueStorage())
        engine.loadScene(SceneType.FLOWER)
    }

    private fun run(seconds: Float) {
        var t = 0f
        while (t < seconds) {
            engine.update(0.05f, cw, ch)
            t += 0.05f
        }
    }

    private fun girlBirthdayToday(year: Int = 1996) =
        store.setBirthday(Partner.GIRL, LocalDate(year, today.month.number, today.day).toString())

    private fun start() {
        engine.birthdayStore = store
        run(1.5f)
        assertEquals(SurpriseStep.DARK, engine.birthdaySurprise.step)
    }

    @Test
    fun `the whole surprise, with the wish and the sealed letter`() {
        girlBirthdayToday()
        val letter = store.seal(Partner.GIRL, LetterKind.BIRTHDAY, "Happy birthday, love.", "Bean", opensOn = today)
        engine.birthdayOverlayShowing = true
        start()
        assertEquals(SceneType.SLEEP, engine.currentScene)
        assertEquals(1f, engine.birthdaySurprise.darkness, 0f)
        assertEquals(listOf(Partner.GIRL), engine.birthdaySurprise.birthdayOf)

        // The light comes on: the boy is there with the cake, the girl gets a hat.
        assertTrue(engine.onBirthdayTap(cw, ch))
        run(1f)
        assertEquals(SurpriseStep.SURPRISE, engine.birthdaySurprise.step)
        assertEquals(HeldItem.CAKE, engine.boy.heldItem)
        assertEquals(0b111, engine.boy.heldItemState)
        assertTrue(engine.girl.wearsPartyHat)
        assertTrue(engine.mochiPartyCollar)

        run(3.6f)
        assertEquals(SurpriseStep.CANDLES, engine.birthdaySurprise.step)
        repeat(3) { engine.onBirthdayTap(cw, ch) }
        assertEquals(0, engine.boy.heldItemState)
        run(1.2f)
        assertEquals(SurpriseStep.WISH, engine.birthdaySurprise.step)

        engine.submitBirthdayWish("a little garden of our own")
        assertEquals(SurpriseStep.GIFT, engine.birthdaySurprise.step)
        engine.onBirthdayTap(cw, ch)
        assertEquals(SurpriseStep.LETTER, engine.birthdaySurprise.step)
        assertEquals(letter.id, engine.birthdaySurprise.currentLetter?.id)

        engine.closeBirthdayLetter()
        assertEquals(SurpriseStep.DONE, engine.birthdaySurprise.step)
        assertFalse(engine.birthdaySurprise.isRunning)
        assertTrue(store.letters().single().isOpened)
        val record = store.records().single()
        assertEquals("a little garden of our own", record.wish)
        assertEquals(letter.id, record.letterId)
        assertEquals(today.year - 1996, record.age)
        assertEquals(today, store.partyHeldOn(Partner.GIRL))
        assertTrue(events.any { it is ProgressEvent.BirthdayCelebrated && !it.forBoy })

        // The party carries on: hat, banner, a line on the first tap.
        run(1f)
        assertTrue(engine.girl.wearsPartyHat)
        assertTrue(engine.birthdaySurprise.partyOn(today))
        engine.onTouchGirl(cw, ch)
        assertEquals("It’s my birthday today!", engine.girlSpeechText)
    }

    @Test
    fun `without the overlay the wish and letter wait, and the letter stays sealed`() {
        girlBirthdayToday(year = 1904)
        store.seal(Partner.GIRL, LetterKind.BIRTHDAY, "Open me today", "Bean", opensOn = today)
        start()
        engine.onBirthdayTap(cw, ch)
        run(4.6f)
        repeat(3) { engine.onBirthdayTap(cw, ch) }
        run(1.2f)
        assertEquals(SurpriseStep.GIFT, engine.birthdaySurprise.step)
        engine.onBirthdayTap(cw, ch)
        assertEquals(SurpriseStep.DONE, engine.birthdaySurprise.step)
        assertFalse(store.letters().single().isOpened)
        assertNull("No age without a year", store.records().single().age)
    }

    @Test
    fun `held once a year, and taps outside the surprise are not taken`() {
        girlBirthdayToday()
        start()
        engine.onBirthdayTap(cw, ch)
        run(4.6f)
        repeat(3) { engine.onBirthdayTap(cw, ch) }
        run(1.2f)
        engine.onBirthdayTap(cw, ch)
        assertFalse(engine.onBirthdayTap(cw, ch))

        // A fresh engine on the same day (the app reopened): no second surprise, the party is on.
        val again = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {})
        again.birthdayStore = store
        again.loadScene(SceneType.FLOWER)
        repeat(40) { again.update(0.05f, cw, ch) }
        assertNull(again.birthdaySurprise.step)
        assertTrue(again.birthdaySurprise.partyOn(today))
        assertTrue(again.girl.wearsPartyHat)
    }

    @Test
    fun `both birthdays on the same day share one party`() {
        val date = LocalDate(1995, today.month.number, today.day).toString()
        store.setBirthday(Partner.BOY, date)
        store.setBirthday(Partner.GIRL, date)
        start()
        assertEquals(listOf(Partner.BOY, Partner.GIRL), engine.birthdaySurprise.birthdayOf)
        engine.onBirthdayTap(cw, ch)
        run(1f)
        assertEquals(HeldItem.CAKE, engine.girl.heldItem)
        assertTrue(engine.boy.wearsPartyHat && engine.girl.wearsPartyHat)
    }

    @Test
    fun `no birthday, no surprise`() {
        engine.birthdayStore = store
        run(3f)
        assertNull(engine.birthdaySurprise.step)
        assertFalse(engine.onBirthdayTap(cw, ch))
    }
}
