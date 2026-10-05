package com.example.progress

import com.example.engine.AmbientAudio
import com.example.engine.EmoteType
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Mochi's fondness and gifts inside the real engine (plan 07, D2-D3). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MochiAndGiftsEngineTest {

    private fun engine(scene: SceneType = SceneType.FLOWER) =
        SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
            loadScene(scene)
            updateAtmosphereMode("DAY")
            weatherDriftEnabled = false
            repeat(60) { update(1f / 60f, 1080f, 2400f) }
        }

    @Test
    fun pettingMochiCountsAndShowsTheMeter() {
        val e = engine()
        val events = mutableListOf<ProgressEvent>()
        e.onProgress = { events += it }
        e.onTouchCat(1080f, 2400f)
        assertTrue(events.any { it is ProgressEvent.MochiCare && it.points == 1 })
        assertTrue(e.mochiMeterTimer > 0f)
    }

    @Test
    fun aTreatCountsMore() {
        val e = engine(SceneType.COOKING)
        val events = mutableListOf<ProgressEvent>()
        e.onProgress = { events += it }
        e.onTouchKitchenTreatJar()
        assertTrue(events.any { it is ProgressEvent.MochiCare && it.points == 3 })
    }

    @Test
    fun aBestFriendSometimesBlinksSlowly() {
        val e = engine()
        e.mochiFondness = MochiFondness.LEVELS.last()
        var blinks = 0
        repeat(40) {
            e.onTouchCat(1080f, 2400f)
            if (e.sceneMessage?.contains("slow") == true) blinks++
        }
        assertTrue("slow blinks: $blinks", blinks in 3..30)
    }

    @Test
    fun aGiftMakesThemBothReact() {
        val e = engine()
        e.giveGift(fromBoy = true, itemName = "a seashell")
        assertEquals(EmoteType.HEART, e.boy.emote)
        assertEquals(EmoteType.BLUSH, e.girl.emote)
        assertTrue(e.sceneMessage?.contains("a seashell") == true)
    }
}
