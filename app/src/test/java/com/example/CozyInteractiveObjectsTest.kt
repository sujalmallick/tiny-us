package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.AmbientAudio
import com.example.engine.CharacterEmotion
import com.example.engine.CharacterPose
import com.example.engine.EmoteType
import com.example.scene.CatState
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CozyInteractiveObjectsTest {

    private lateinit var context: Context
    private lateinit var engine: SceneEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val sp = context.getSharedPreferences("tiny_us_prefs", Context.MODE_PRIVATE)
        sp.edit().clear().commit()
        val audio = AmbientAudio(context)
        engine = SceneEngine(
            audio = audio,
            onOpenLoveNotes = {},
            onOpenMemories = {}
        )
    }

    @Test
    fun `test Aromatherapy Candle toggles and evokes warm reactions`() {
        assertFalse(engine.hearthCandleLit)

        // Tap to light candle
        engine.onTouchAromatherapyCandle(400f, 800f, 200f, 500f)
        assertTrue(engine.hearthCandleLit)
        assertEquals(CharacterEmotion.LOVING, engine.boy.emotion)
        assertEquals(CharacterEmotion.LOVING, engine.girl.emotion)
        assertEquals(EmoteType.HEART, engine.boy.emote)
        assertEquals(EmoteType.SPARKLE, engine.girl.emote)

        // Tap to extinguish candle
        engine.onTouchAromatherapyCandle(400f, 800f, 200f, 500f)
        assertFalse(engine.hearthCandleLit)
    }

    @Test
    fun `test Whistling Teakettle interaction and cooldown guard`() {
        assertEquals(0f, engine.teakettleWhistleTimer)

        engine.onTouchTeakettle(400f, 800f, 240f, 520f)
        assertTrue(engine.teakettleWhistleTimer > 2.0f)
        assertEquals(CharacterPose.COOK, engine.girl.pose)
        assertEquals(CharacterPose.EAT_SNEAK, engine.boy.pose)

        // Duplicate tap while whistling should be ignored by cooldown guard
        val currentTimer = engine.teakettleWhistleTimer
        engine.onTouchTeakettle(400f, 800f, 240f, 520f)
        assertEquals(currentTimer, engine.teakettleWhistleTimer)
    }

    @Test
    fun `test Couch Throw snuggle blanket cuddling and Mochi sleep`() {
        assertEquals(0f, engine.cuddleBlanketTimer)

        engine.onTouchCouchThrow(400f, 800f)
        assertTrue(engine.cuddleBlanketTimer > 3.0f)
        assertEquals(CharacterPose.SIT_SNUGGLE, engine.boy.pose)
        assertEquals(CharacterPose.SIT_SNUGGLE, engine.girl.pose)
        assertEquals(CharacterEmotion.LOVING, engine.boy.emotion)
        assertEquals(CharacterEmotion.LOVING, engine.girl.emotion)
        assertEquals(CatState.SLEEPING, engine.catState)
        assertTrue(engine.catSleeping)
        assertEquals(0.40f, engine.catWorldX, 0.01f)
        assertEquals(0.68f, engine.catWorldY, 0.01f)
    }

    @Test
    fun `test Porch Wind Chimes sway and audio feedback`() {
        assertEquals(0f, engine.windChimeSwayTimer)

        engine.onTouchWindChimes(100f, 200f)
        assertTrue(engine.windChimeSwayTimer > 2.0f)
        assertEquals(CharacterEmotion.HAPPY, engine.boy.emotion)
        assertEquals(CharacterEmotion.HAPPY, engine.girl.emotion)

        // Cooldown guard prevents spam
        val timer = engine.windChimeSwayTimer
        engine.onTouchWindChimes(100f, 200f)
        assertEquals(timer, engine.windChimeSwayTimer)
    }

    @Test
    fun `test Feather Wand summons Mochi into playful pounce`() {
        assertEquals(0f, engine.featherWandWiggleTimer)

        engine.onTouchFeatherWand(90f, 600f)
        assertTrue(engine.featherWandWiggleTimer > 2.0f)
        assertEquals(CatState.PLAYFUL_POUNCE, engine.catState)
        assertFalse(engine.catSleeping)
        assertEquals(0.22f, engine.catWorldX, 0.01f)
        assertEquals(CharacterEmotion.PLAYFUL, engine.boy.emotion)
        assertEquals(CharacterEmotion.PLAYFUL, engine.girl.emotion)
    }

    @Test
    fun `test Plant Watering interaction sets reaction`() {
        assertEquals(0f, engine.plantWaterTimer)

        engine.onTouchPlantWatering(120f, 300f)
        assertTrue(engine.plantWaterTimer > 1.8f)
        assertEquals(CharacterEmotion.HAPPY, engine.girl.emotion)
        assertEquals(EmoteType.SPARKLE, engine.girl.emote)
    }

    @Test
    fun `test Vintage Telescope stargazing wish`() {
        assertEquals(0f, engine.telescopeStarTimer)

        engine.onTouchTelescope(400f, 800f, 150f, 540f)
        assertTrue(engine.telescopeStarTimer > 2.8f)
        assertEquals(EmoteType.SPARKLE, engine.boy.emote)
        assertEquals(EmoteType.HEART, engine.girl.emote)
    }

    @Test
    fun `test engine update decrements all cozy timers properly`() {
        engine.windChimeSwayTimer = 1.0f
        engine.teakettleWhistleTimer = 1.0f
        engine.cuddleBlanketTimer = 1.0f
        engine.featherWandWiggleTimer = 1.0f
        engine.plantWaterTimer = 1.0f
        engine.telescopeStarTimer = 1.0f

        engine.update(0.5f, 400f, 800f)

        assertEquals(0.5f, engine.windChimeSwayTimer, 0.05f)
        assertEquals(0.5f, engine.teakettleWhistleTimer, 0.05f)
        assertEquals(0.5f, engine.cuddleBlanketTimer, 0.05f)
        assertEquals(0.5f, engine.featherWandWiggleTimer, 0.05f)
        assertEquals(0.5f, engine.plantWaterTimer, 0.05f)
        assertEquals(0.5f, engine.telescopeStarTimer, 0.05f)

        engine.update(0.6f, 400f, 800f)

        assertEquals(0f, engine.windChimeSwayTimer)
        assertEquals(0f, engine.teakettleWhistleTimer)
        assertEquals(0f, engine.cuddleBlanketTimer)
        assertEquals(0f, engine.featherWandWiggleTimer)
        assertEquals(0f, engine.plantWaterTimer)
        assertEquals(0f, engine.telescopeStarTimer)
    }

    @Test
    fun `test loadScene resets all cozy timers cleanly`() {
        engine.windChimeSwayTimer = 2.0f
        engine.teakettleWhistleTimer = 2.0f
        engine.cuddleBlanketTimer = 2.0f
        engine.featherWandWiggleTimer = 2.0f
        engine.plantWaterTimer = 2.0f
        engine.telescopeStarTimer = 2.0f

        engine.loadScene(SceneType.COOKING)

        assertEquals(0f, engine.windChimeSwayTimer)
        assertEquals(0f, engine.teakettleWhistleTimer)
        assertEquals(0f, engine.cuddleBlanketTimer)
        assertEquals(0f, engine.featherWandWiggleTimer)
        assertEquals(0f, engine.plantWaterTimer)
        assertEquals(0f, engine.telescopeStarTimer)
    }
}
