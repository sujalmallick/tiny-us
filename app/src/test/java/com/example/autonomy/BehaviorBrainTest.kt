package com.example.autonomy

import com.example.scene.SceneType
import com.example.scene.WeatherType
import com.example.scene.autonomy.Behavior
import com.example.scene.autonomy.BehaviorBrain
import com.example.scene.autonomy.BehaviorCategory
import com.example.scene.autonomy.BehaviorContext
import com.example.scene.autonomy.BehaviorMemory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** The decision brain: weighted, context-aware, never stuck in a loop, rare things stay rare. */
class BehaviorBrainTest {

    private fun dayContext() = BehaviorContext().apply {
        scene = SceneType.FLOWER
        isOutdoor = true
        weather = WeatherType.SUNNY
        distanceToPartner = 0.4f
        distanceToMochi = 0.4f
        propAvailable = true
        sceneMomentAvailable = true
        rareCooldown = 0f
    }

    private fun weight(brain: BehaviorBrain, ctx: BehaviorContext, b: Behavior, memory: BehaviorMemory = BehaviorMemory()): Float {
        brain.computeWeights(ctx, memory)
        return brain.lastWeight(b)
    }

    @Test
    fun `weighted choice varies and the heaviest option does not always win`() {
        val brain = BehaviorBrain(Random(1))
        val counts = IntArray(Behavior.entries.size)
        repeat(3000) { counts[brain.choose(dayContext(), BehaviorMemory()).ordinal]++ }
        val top = counts.max()
        val distinct = counts.count { it > 0 }
        assertTrue("Many different behaviors appear: $distinct", distinct >= 8)
        assertTrue("No single behavior dominates: ${top / 3000f}", top < 3000 * 0.5f)
    }

    @Test
    fun `a nearby partner makes talking likely, a far one makes walking over likely`() {
        val brain = BehaviorBrain()
        val near = dayContext().apply { distanceToPartner = 0.1f }
        val far = dayContext().apply { distanceToPartner = 0.5f }
        assertTrue(weight(brain, near, Behavior.TALK) > weight(brain, far, Behavior.TALK) * 3f)
        assertEquals(0f, weight(brain, near, Behavior.APPROACH_PARTNER), 0f)
        assertTrue(weight(brain, far, Behavior.APPROACH_PARTNER) > 15f)
        assertEquals("No hugging from across the scene", 0f, weight(brain, far, Behavior.HUG), 0f)
    }

    @Test
    fun `a busy partner is left alone`() {
        val brain = BehaviorBrain()
        val ctx = dayContext().apply { distanceToPartner = 0.1f; partnerAvailable = false }
        brain.computeWeights(ctx, BehaviorMemory())
        for (b in Behavior.entries) if (b.involvesPartner) assertEquals(b.name, 0f, brain.lastWeight(b), 0f)
    }

    @Test
    fun `rain outdoors draws the couple together under the umbrella`() {
        val brain = BehaviorBrain()
        val rain = dayContext().apply { weather = WeatherType.RAIN }
        val sun = dayContext()
        assertTrue(weight(brain, rain, Behavior.SHELTER_CLOSE) >= 30f)
        assertEquals(0f, weight(brain, sun, Behavior.SHELTER_CLOSE), 0f)
        assertTrue(weight(brain, rain, Behavior.WANDER) < weight(brain, sun, Behavior.WANDER))
    }

    @Test
    fun `night outdoors means stargazing, mornings mean stretching`() {
        val brain = BehaviorBrain()
        val night = dayContext().apply { isNight = true }
        val morning = dayContext().apply { isMorning = true }
        assertTrue(weight(brain, night, Behavior.LOOK_AT_SKY) > weight(brain, dayContext(), Behavior.LOOK_AT_SKY) + 20f)
        assertEquals(0f, weight(brain, night, Behavior.STRETCH), 0f)
        assertTrue(weight(brain, morning, Behavior.STRETCH) > weight(brain, dayContext(), Behavior.STRETCH))
    }

    @Test
    fun `weather reactions follow the season`() {
        val brain = BehaviorBrain()
        val snow = dayContext().apply { weather = WeatherType.SNOW }
        assertTrue(weight(brain, snow, Behavior.WEATHER_REACT) >= 15f)
        val indoorSnow = dayContext().apply { weather = WeatherType.SNOW; isOutdoor = false }
        assertEquals(0f, weight(brain, indoorSnow, Behavior.WEATHER_REACT), 0f)
    }

    @Test
    fun `something just done is on cooldown, then penalised for a while`() {
        val brain = BehaviorBrain()
        val memory = BehaviorMemory()
        memory.remember(Behavior.WANDER)
        assertEquals(0f, weight(brain, dayContext(), Behavior.WANDER, memory), 0f)
        memory.tick(Behavior.WANDER.cooldownSeconds + 0.1f)
        val penalised = weight(brain, dayContext(), Behavior.WANDER, memory)
        val fresh = weight(brain, dayContext(), Behavior.WANDER)
        assertTrue(penalised > 0f && penalised < fresh * 0.5f)
    }

    @Test
    fun `a long run of decisions never gets stuck repeating itself`() {
        val brain = BehaviorBrain(Random(7))
        val memory = BehaviorMemory()
        var previous: Behavior? = null
        var run = 1
        var longestRun = 1
        repeat(400) {
            val ctx = dayContext().apply { activitiesSinceHome = it % 3; distanceFromHome = if (it % 3 == 0) 0f else 0.2f }
            val b = brain.choose(ctx, memory)
            memory.remember(b)
            memory.tick(5f)
            run = if (b == previous) run + 1 else 1
            if (run > longestRun) longestRun = run
            previous = b
        }
        assertTrue("Longest repeat run $longestRun", longestRun <= 2)
    }

    @Test
    fun `rare surprises stay rare and wait for their cooldown`() {
        val brain = BehaviorBrain(Random(3))
        var rare = 0
        repeat(2000) {
            val b = brain.choose(dayContext(), BehaviorMemory())
            if (b.category == BehaviorCategory.RARE) rare++
        }
        assertTrue("Rare share ${rare / 2000f}", rare in 1 until 2000 / 20)

        val cooling = dayContext().apply { rareCooldown = 30f }
        brain.computeWeights(cooling, BehaviorMemory())
        for (b in Behavior.entries) if (b.category == BehaviorCategory.RARE) assertEquals(0f, brain.lastWeight(b), 0f)
    }

    @Test
    fun `on the scooter only in-place behaviors are possible`() {
        val brain = BehaviorBrain()
        val ride = dayContext().apply { scene = SceneType.EVENING_RIDE; canWalk = false; distanceToPartner = 0.1f }
        brain.computeWeights(ride, BehaviorMemory())
        for (b in listOf(Behavior.WANDER, Behavior.VISIT_PROP, Behavior.APPROACH_MOCHI, Behavior.GO_HOME, Behavior.HUG, Behavior.DISCOVER)) {
            assertEquals(b.name, 0f, brain.lastWeight(b), 0f)
        }
        assertTrue(brain.lastWeight(Behavior.TALK) > 0f)
    }

    @Test
    fun `the loft keeps them mostly in place with the odd outing`() {
        val brain = BehaviorBrain()
        val loft = dayContext().apply { scene = SceneType.COZY_LOFT; prefersSeated = true; isOutdoor = false }
        assertEquals(0f, weight(brain, loft, Behavior.WANDER), 0f)
        val visit = weight(brain, loft, Behavior.VISIT_PROP)
        assertTrue(visit > 0f && visit < weight(brain, dayContext(), Behavior.VISIT_PROP))
    }

    @Test
    fun `after an outing or two, home calls`() {
        val brain = BehaviorBrain()
        val away = dayContext().apply { distanceFromHome = 0.3f; activitiesSinceHome = 2 }
        val justLeft = dayContext().apply { distanceFromHome = 0.3f; activitiesSinceHome = 0 }
        val atHome = dayContext().apply { distanceFromHome = 0f }
        assertTrue(weight(brain, away, Behavior.GO_HOME) >= 45f)
        assertTrue(weight(brain, justLeft, Behavior.GO_HOME) < 10f)
        assertEquals(0f, weight(brain, atHome, Behavior.GO_HOME), 0f)
    }
}
