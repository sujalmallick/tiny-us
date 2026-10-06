package com.example

import com.example.data.Festival
import com.example.data.FestivalPicks
import com.example.data.FestivalStore
import com.example.data.Festivals
import com.example.data.InMemoryKeyValueStorage
import com.example.engine.AmbientAudio
import com.example.engine.GameText
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Plan 09, D in the world: crowns, lanterns and gifts under the tree. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FestivalEngineTest {
    private lateinit var engine: SceneEngine
    private lateinit var store: FestivalStore
    private val year = com.example.data.CoupleDates.today().year

    @Before
    fun setup() {
        runBlocking { GameText.load() }
        engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {})
        engine.weatherDriftEnabled = false
        store = FestivalStore(InMemoryKeyValueStorage())
    }

    @After
    fun clear() { Festivals.override = null }

    private fun run(seconds: Float) {
        var t = 0f
        while (t < seconds) { engine.update(0.05f, 1080f, 2400f); t += 0.05f }
    }

    private fun on(f: Festival) {
        Festivals.override = f
        engine.festivalStore = store
        engine.loadScene(f.scene)
        run(1f)
    }

    @Test
    fun `a festival can be joined until it's celebrated`() {
        on(Festival.BLOSSOM_PICNIC)
        assertEquals(Festival.BLOSSOM_PICNIC, engine.festivalToJoin)
        store.markCelebrated(Festival.BLOSSOM_PICNIC, year)
        engine.refreshFestival()
        assertNull(engine.festivalToJoin)
    }

    @Test
    fun `the picnic's crowns are worn for the day, each picked by the other`() {
        on(Festival.BLOSSOM_PICNIC)
        engine.celebratePicnic(boyPickedForGirl = 1, girlPickedForBoy = 2)
        assertEquals(2, engine.boy.crownFlower)
        assertEquals(1, engine.girl.crownFlower)
        // After a restart the same day, the saved picks put the crowns back on.
        store.savePicks(Festival.BLOSSOM_PICNIC, year, FestivalPicks(boy = "1", girl = "2", revealed = true))
        engine.boy.crownFlower = -1
        engine.refreshFestival()
        assertEquals(2, engine.boy.crownFlower)
    }

    @Test
    fun `lanterns rise and drift away`() {
        on(Festival.LANTERN_NIGHT)
        engine.releaseLanterns()
        run(1f)
        assertTrue(engine.lanternRise > 0.9f)
        run(SceneEngine.LANTERN_SECONDS)
        assertEquals(0f, engine.lanternRise, 0f)
    }

    @Test
    fun `gifts wait under the tree until opened, then Mochi takes a ribbon`() {
        on(Festival.GIFT_EXCHANGE)
        store.savePicks(Festival.GIFT_EXCHANGE, year, FestivalPicks(boy = "card", girl = "discovery:SEASHELL"))
        engine.refreshFestival()
        assertTrue(engine.giftBoxesWaiting)
        assertEquals(Festival.GIFT_EXCHANGE, engine.festivalToJoin)
        engine.openFestivalGifts()
        assertFalse(engine.giftBoxesWaiting)
        assertEquals(com.example.scene.CatState.WALK_FOLLOW, engine.catState)
        assertEquals(SceneType.SLEEP, engine.currentScene)
    }
}
