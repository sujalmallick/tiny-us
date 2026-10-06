package com.example.autonomy

import com.example.engine.AmbientAudio
import com.example.engine.PixelArtRenderer
import com.example.progress.ProgressEvent
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.autonomy.CoupleRequests
import com.example.scene.autonomy.RequestKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The request bubble, its lines and its progress (plan 07 D1, GROWTH's side). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RequestBubbleTest {

    private val w = 1080f
    private val h = 2400f

    private fun engine(scene: SceneType) =
        SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
            updateNames("Leo", "Mia")
            loadScene(scene)
            updateAtmosphereMode("NIGHT")
            weatherDriftEnabled = false
            repeat(30) { update(1f / 60f, w, h) }
        }

    private fun SceneEngine.run(seconds: Float) = repeat((seconds * 60).toInt()) { update(1f / 60f, w, h) }

    @Test
    fun everyKindHasAnIcon() {
        assertEquals(RequestKind.entries.map { it.name }.toSet(), PixelArtRenderer.requestIconNames)
    }

    @Test
    fun theAskerCarriesTheBubbleAndSaysWhatTheyWant() {
        val e = engine(SceneType.COZY_LOFT)
        e.startRequestForTest(RequestKind.TEA, e.girl)
        e.run(0.2f)
        assertEquals("TEA", e.girl.requestIcon)
        assertNull(e.boy.requestIcon)
        assertTrue(e.sceneMessage.orEmpty(), e.sceneMessage?.contains("Mia") == true)
        assertEquals(1f, e.girl.requestFade, 0.001f)
    }

    @Test
    fun grantingThanksRecordsAndClearsTheBubble() {
        val e = engine(SceneType.COZY_LOFT)
        val events = mutableListOf<ProgressEvent>()
        e.onProgress = { events += it }
        e.startRequestForTest(RequestKind.TEA, e.boy)
        e.run(0.2f)
        e.onTouchLoftTable(w, h)
        assertTrue(events.contains(ProgressEvent.RequestGranted("TEA")))
        assertNull(e.boy.requestIcon)
        assertEquals("Just how I like it.", e.boySpeechText)
    }

    @Test
    fun anUnansweredRequestFadesAndLeavesNothing() {
        val e = engine(SceneType.COZY_LOFT)
        val events = mutableListOf<ProgressEvent>()
        e.onProgress = { events += it }
        e.startRequestForTest(RequestKind.SONG, e.girl)
        e.run(CoupleRequests.WINDOW - CoupleRequests.FADE_SECONDS / 2f)
        assertTrue("fading: ${e.girl.requestFade}", e.girl.requestFade in 0.01f..0.99f)
        e.run(CoupleRequests.FADE_SECONDS)
        assertNull(e.girl.requestIcon)
        assertTrue(events.none { it is ProgressEvent.RequestGranted })
    }
}
