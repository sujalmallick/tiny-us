package com.example.autonomy

import com.example.data.Friend
import com.example.data.FriendsStore
import com.example.data.InMemoryKeyValueStorage
import com.example.engine.AmbientAudio
import com.example.progress.ProgressEvent
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** Plan 09, I: Leo, Bao and Pip keep their hours, love their favourites, and share a secret. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FriendsEngineTest {

    private lateinit var engine: SceneEngine
    private lateinit var store: FriendsStore
    private val events = mutableListOf<ProgressEvent>()
    private var box = mutableMapOf<String, Int>()
    private val cw = 1080f
    private val ch = 2400f
    private val dt = 0.05f

    @Before
    fun setup() {
        engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {})
        engine.weatherDriftEnabled = false
        engine.updateAtmosphereMode("DAY")
        engine.behaviorBrain.random = Random(4)
        engine.autonomyEnabled = false
        store = FriendsStore(InMemoryKeyValueStorage())
        engine.friendsStore = store
        engine.keepsakesProvider = { box }
        engine.onProgress = { events += it }
    }

    private fun run(seconds: Float) {
        var t = 0f
        while (t < seconds) { engine.update(dt, cw, ch); t += dt }
    }

    @Test
    fun `the cafe keeps its hours, and Leo doesn't mind them staying late`() {
        engine.loadScene(SceneType.RAINY_CAFE)
        engine.clockHourOverride = 10
        assertTrue(engine.cafeOpen)
        engine.onTouchCafeBarista(cw, ch)
        assertTrue("He brews while open", engine.cafeBaristaBrewTimer > 0f)
        run(3f)
        engine.clockHourOverride = 22
        assertFalse(engine.cafeOpen)
        engine.onTouchCafeBarista(cw, ch)
        assertEquals("No brewing after hours", 0f, engine.cafeBaristaBrewTimer, 0.001f)
    }

    @Test
    fun `tea at four on the pier`() {
        engine.loadScene(SceneType.SEASIDE_PIER)
        engine.clockHourOverride = 16
        assertTrue(engine.isBaoTeaTime)
        engine.clockHourOverride = 9
        assertFalse(engine.isBaoTeaTime)
    }

    @Test
    fun `a favourite for Bao opens his secret, once, and only one gift a day`() {
        engine.loadScene(SceneType.SEASIDE_PIER)
        run(1f)
        engine.clockHourOverride = 10
        box["dish:tea"] = 2
        engine.onTouchGrandpaBao(cw, ch)
        assertTrue(ProgressEvent.FriendGift("BAO", "dish:tea") in events)
        assertTrue(ProgressEvent.SecretFound("BAO") in events)
        assertTrue(store.secretOpen(Friend.BAO))
        assertNull("The thank-you shows first", engine.shownSecret)
        run(SceneEngine.SECRET_AFTER + 0.5f)
        assertEquals(Friend.BAO, engine.shownSecret)

        // Again today: he's just glad to see them, no second gift
        engine.shownSecret = null
        engine.onTouchGrandpaBao(cw, ch)
        assertEquals(1, events.count { it is ProgressEvent.FriendGift })
    }

    @Test
    fun `no favourite in the box, no gift`() {
        engine.loadScene(SceneType.RAINY_CAFE)
        box["dish:soup"] = 3
        engine.onTouchCafeBarista(cw, ch)
        assertTrue(events.none { it is ProgressEvent.FriendGift })
        box["dish:pumpkin_pie"] = 1
        engine.onTouchCafeBarista(cw, ch)
        assertTrue(ProgressEvent.FriendGift("LEO", "dish:pumpkin_pie") in events)
    }

    @Test
    fun `Pip takes a fish instead of being shooed`() {
        engine.loadScene(SceneType.SEASIDE_PIER)
        var t = 0f
        while (!engine.pierGullState.isVisible && t < 60f) { engine.update(dt, cw, ch); t += dt }
        assertTrue("Pip turned up", engine.pierGullState.isVisible)
        box["catch:MINNOW"] = 1
        engine.onTouchPip(cw, ch)
        assertTrue(ProgressEvent.FriendGift("PIP", "catch:MINNOW") in events)
        assertTrue("He stays", engine.pierGullState != com.example.scene.GullState.ESCAPING)
    }

    @Test
    fun `the gift leaves the box`() {
        var progress = com.example.progress.ProgressState().keep("dish:tea", 2)
        progress = progress.record(ProgressEvent.FriendGift("BAO", "dish:tea"))
        assertEquals(1, progress.keepsakes["dish:tea"])
        progress = progress.record(ProgressEvent.SecretFound("BAO"))
        assertTrue(com.example.data.CollectionBook.isFound("secret:BAO", progress))
    }
}
