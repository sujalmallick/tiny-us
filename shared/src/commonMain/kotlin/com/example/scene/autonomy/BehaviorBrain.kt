package com.example.scene.autonomy

import com.example.scene.SceneType
import com.example.scene.WeatherType
import kotlin.random.Random

/**
 * Everything a character can decide to do on their own. Each behavior belongs to a
 * category and has a base weight; the brain adjusts weights by context before picking.
 */
enum class Behavior(val category: BehaviorCategory, val baseWeight: Float, val cooldownSeconds: Float) {
    // Ambient
    LOOK_AROUND(BehaviorCategory.AMBIENT, 25f, 6f),
    LOOK_AT_SKY(BehaviorCategory.AMBIENT, 6f, 20f),
    STRETCH(BehaviorCategory.AMBIENT, 4f, 40f),
    WANDER(BehaviorCategory.AMBIENT, 18f, 8f),
    REST(BehaviorCategory.AMBIENT, 8f, 15f),

    // Environment
    VISIT_PROP(BehaviorCategory.ENVIRONMENT, 15f, 10f),

    // Partner
    APPROACH_PARTNER(BehaviorCategory.PARTNER, 5f, 18f),
    TALK(BehaviorCategory.PARTNER, 8f, 14f),
    HUG(BehaviorCategory.PARTNER, 2f, 45f),
    HOLD_HANDS(BehaviorCategory.PARTNER, 2f, 35f),
    SIT_TOGETHER(BehaviorCategory.PARTNER, 3f, 40f),

    // Mochi
    APPROACH_MOCHI(BehaviorCategory.MOCHI, 4f, 20f),
    PET_MOCHI(BehaviorCategory.MOCHI, 4f, 25f),
    WATCH_MOCHI(BehaviorCategory.MOCHI, 3f, 15f),

    // Scene, weather and coming home
    SCENE_MOMENT(BehaviorCategory.SCENE, 6f, 30f),
    WEATHER_REACT(BehaviorCategory.WEATHER, 0f, 20f),
    SHELTER_CLOSE(BehaviorCategory.WEATHER, 0f, 25f),
    GO_HOME(BehaviorCategory.HOME, 0f, 4f),

    // Discovery and rare surprises
    DISCOVER(BehaviorCategory.DISCOVERY, 5f, 20f),
    RARE_FLOWER_GIFT(BehaviorCategory.RARE, 1f, 300f),
    RARE_SHOOTING_STAR(BehaviorCategory.RARE, 1f, 300f),
    RARE_DOZE_OFF(BehaviorCategory.RARE, 1f, 360f),
    RARE_DANCE(BehaviorCategory.RARE, 1f, 300f),
    RARE_MOCHI_ZOOMIES(BehaviorCategory.RARE, 1f, 300f),

    // Asking the player for something small (plan 07 D1); its timing lives in CoupleRequests.
    ASK_FOR_SOMETHING(BehaviorCategory.REQUEST, 0f, 30f);

    val involvesPartner: Boolean
        get() = category == BehaviorCategory.PARTNER || this == SCENE_MOMENT || this == SHELTER_CLOSE ||
            this == RARE_FLOWER_GIFT || this == RARE_SHOOTING_STAR || this == RARE_DOZE_OFF || this == RARE_DANCE
}

enum class BehaviorCategory { AMBIENT, ENVIRONMENT, PARTNER, MOCHI, SCENE, WEATHER, HOME, DISCOVERY, RARE, REQUEST }

/** What a character can see and knows when deciding. Filled in place before each decision (no allocation). */
class BehaviorContext {
    var scene: SceneType = SceneType.FLOWER
    var isOutdoor: Boolean = true
    var weather: WeatherType = WeatherType.SUNNY
    var isNight: Boolean = false
    var isMorning: Boolean = false
    var isSunset: Boolean = false

    /** The character may walk around in this scene (false on the scooter). */
    var canWalk: Boolean = true
    /** Seated scenes (loft) only get occasional outings. */
    var prefersSeated: Boolean = false

    var distanceToPartner: Float = 1f
    var partnerAvailable: Boolean = true
    var distanceToMochi: Float = 1f
    var mochiPresent: Boolean = true
    var mochiWalking: Boolean = false
    var mochiAsleep: Boolean = false

    var distanceFromHome: Float = 0f
    var activitiesSinceHome: Int = 0
    var propAvailable: Boolean = false
    var sceneMomentAvailable: Boolean = false
    var discoveryAvailable: Boolean = false
    var isSitting: Boolean = false
    /** Seconds until any rare behavior may happen again (shared by the couple). */
    var rareCooldown: Float = 0f
    /** A request may start now and some kind of request fits the moment. */
    var requestAvailable: Boolean = false
}

/** Per-character short-term memory: what they did recently and what is still cooling down. */
class BehaviorMemory {
    private val history = IntArray(HISTORY) { -1 }
    private var historyCount = 0
    private val cooldowns = FloatArray(Behavior.entries.size)

    fun remember(behavior: Behavior) {
        for (i in HISTORY - 1 downTo 1) history[i] = history[i - 1]
        history[0] = behavior.ordinal
        if (historyCount < HISTORY) historyCount++
        cooldowns[behavior.ordinal] = behavior.cooldownSeconds
    }

    fun tick(dt: Float) {
        for (i in cooldowns.indices) if (cooldowns[i] > 0f) cooldowns[i] = (cooldowns[i] - dt).coerceAtLeast(0f)
    }

    fun cooldownOf(behavior: Behavior): Float = cooldowns[behavior.ordinal]

    /** 0 = most recent, -1 if not in the recent history. */
    fun recency(behavior: Behavior): Int {
        for (i in 0 until historyCount) if (history[i] == behavior.ordinal) return i
        return -1
    }

    fun lastBehavior(): Behavior? = if (historyCount == 0) null else Behavior.entries[history[0]]

    fun clear() {
        history.fill(-1)
        historyCount = 0
        cooldowns.fill(0f)
    }

    companion object {
        const val HISTORY = 4
    }
}

/**
 * Picks what a character does next with weighted randomness: base weights, then context
 * modifiers, then anti-repeat penalties and cooldowns. The heaviest option is likely but
 * never guaranteed, so the couple stays a little unpredictable.
 */
class BehaviorBrain(var random: Random = Random.Default) {
    private val weights = FloatArray(Behavior.entries.size)

    /** Exposed for tests and tuning: the final weights from the most recent [choose]. */
    fun lastWeight(behavior: Behavior): Float = weights[behavior.ordinal]

    fun choose(ctx: BehaviorContext, memory: BehaviorMemory): Behavior {
        computeWeights(ctx, memory)
        var total = 0f
        for (w in weights) total += w
        if (total <= 0f) return Behavior.LOOK_AROUND
        var roll = random.nextFloat() * total
        for (b in Behavior.entries) {
            val w = weights[b.ordinal]
            if (w <= 0f) continue
            if (roll < w) return b
            roll -= w
        }
        return Behavior.LOOK_AROUND
    }

    fun computeWeights(ctx: BehaviorContext, memory: BehaviorMemory) {
        for (b in Behavior.entries) weights[b.ordinal] = b.baseWeight
        applyContext(ctx)
        applyMemory(memory)
    }

    private fun set(b: Behavior, w: Float) { weights[b.ordinal] = w }
    private fun add(b: Behavior, w: Float) { weights[b.ordinal] += w }
    private fun scale(b: Behavior, f: Float) { weights[b.ordinal] *= f }

    private fun applyContext(c: BehaviorContext) {
        // Partner: talk and cuddle when close, walk over when far, nothing if they're busy.
        if (!c.partnerAvailable) {
            for (b in Behavior.entries) if (b.involvesPartner) set(b, 0f)
        } else {
            // Talking works from a seat apart; cuddling needs them right next to each other.
            val close = c.distanceToPartner < TALK_DISTANCE
            if (close) {
                add(Behavior.TALK, 20f)
                if (c.distanceToPartner < CUDDLE_DISTANCE) {
                    add(Behavior.HUG, 6f)
                    add(Behavior.HOLD_HANDS, 5f)
                    add(Behavior.SIT_TOGETHER, 4f)
                } else {
                    set(Behavior.HUG, 0f)
                    set(Behavior.HOLD_HANDS, 0f)
                    set(Behavior.SIT_TOGETHER, 0f)
                }
                set(Behavior.APPROACH_PARTNER, 0f)
            } else {
                add(Behavior.APPROACH_PARTNER, 15f)
                set(Behavior.HUG, 0f)
                set(Behavior.HOLD_HANDS, 0f)
                set(Behavior.SIT_TOGETHER, 0f)
                scale(Behavior.TALK, 0.3f)
            }
        }

        // Mochi.
        if (!c.mochiPresent) {
            set(Behavior.APPROACH_MOCHI, 0f); set(Behavior.PET_MOCHI, 0f); set(Behavior.WATCH_MOCHI, 0f)
            set(Behavior.RARE_MOCHI_ZOOMIES, 0f)
        } else {
            if (c.distanceToMochi < 0.14f) {
                add(Behavior.PET_MOCHI, 12f)
                set(Behavior.APPROACH_MOCHI, 0f)
            } else {
                add(Behavior.APPROACH_MOCHI, 6f)
                set(Behavior.PET_MOCHI, 0f)
            }
            if (c.mochiWalking) add(Behavior.WATCH_MOCHI, 10f)
            if (c.mochiAsleep) scale(Behavior.PET_MOCHI, 0.5f)
        }

        // Environment and scene content only when it exists here.
        if (!c.propAvailable) set(Behavior.VISIT_PROP, 0f)
        if (!c.sceneMomentAvailable || !c.partnerAvailable) set(Behavior.SCENE_MOMENT, 0f)
        if (!c.discoveryAvailable) set(Behavior.DISCOVER, 0f) else add(Behavior.DISCOVER, 10f)
        if (c.requestAvailable) add(Behavior.ASK_FOR_SOMETHING, REQUEST_WEIGHT) else set(Behavior.ASK_FOR_SOMETHING, 0f)

        // Weather.
        if (c.isOutdoor) {
            when (c.weather) {
                WeatherType.RAIN -> {
                    // One umbrella: stay close, wander less.
                    if (c.partnerAvailable && c.distanceToPartner > 0.14f) add(Behavior.SHELTER_CLOSE, 30f)
                    scale(Behavior.WANDER, 0.3f)
                    scale(Behavior.VISIT_PROP, 0.5f)
                    scale(Behavior.APPROACH_MOCHI, 0.5f)
                    add(Behavior.WEATHER_REACT, 6f)
                    set(Behavior.RARE_DANCE, 0f)
                }
                WeatherType.SNOW, WeatherType.SAKURA, WeatherType.AUTUMN -> add(Behavior.WEATHER_REACT, 15f)
                WeatherType.SUNNY -> { scale(Behavior.WANDER, 1.3f); add(Behavior.WEATHER_REACT, 3f) }
            }
        } else if (c.weather == WeatherType.RAIN) {
            // Indoors on a rainy day the windows are the place to be.
            scale(Behavior.VISIT_PROP, 1.6f)
        }
        if (c.weather != WeatherType.RAIN || !c.isOutdoor) set(Behavior.SHELTER_CLOSE, 0f)

        // Time of day.
        when {
            c.isNight -> {
                if (c.isOutdoor) add(Behavior.LOOK_AT_SKY, 25f) else add(Behavior.REST, 10f)
                scale(Behavior.WANDER, 0.6f)
                set(Behavior.STRETCH, 0f)
            }
            c.isMorning -> { add(Behavior.STRETCH, 12f); scale(Behavior.WANDER, 1.2f) }
            c.isSunset -> { add(Behavior.LOOK_AT_SKY, 10f); add(Behavior.SIT_TOGETHER, if (c.partnerAvailable && c.distanceToPartner < CUDDLE_DISTANCE) 6f else 0f) }
        }

        // Rare surprises only in the right conditions, and only once in a while.
        if (c.rareCooldown > 0f) {
            for (b in Behavior.entries) if (b.category == BehaviorCategory.RARE) set(b, 0f)
        }
        if (!(c.isNight && c.isOutdoor)) set(Behavior.RARE_SHOOTING_STAR, 0f)
        if (!c.isNight || c.distanceToPartner > 0.2f) set(Behavior.RARE_DOZE_OFF, 0f)
        if (c.isNight || !c.canWalk) set(Behavior.RARE_DANCE, 0f)
        if (!c.isOutdoor || c.weather == WeatherType.SNOW) set(Behavior.RARE_FLOWER_GIFT, 0f)

        // Coming home: after an outing or two, home gets very attractive.
        if (c.distanceFromHome > HOME_TOLERANCE) {
            // The pull home grows with every extra outing, so excursions never drag on.
            if (c.activitiesSinceHome >= 2) add(Behavior.GO_HOME, 45f + (c.activitiesSinceHome - 2) * 30f)
            else add(Behavior.GO_HOME, 6f)
        } else {
            set(Behavior.GO_HOME, 0f)
        }

        // Sitting characters don't stretch or rest again.
        if (c.isSitting) { set(Behavior.REST, 0f); set(Behavior.SIT_TOGETHER, 0f) }

        // Where walking isn't possible (the scooter) only in-place behaviors remain.
        if (!c.canWalk) {
            for (b in Behavior.entries) {
                val inPlace = b == Behavior.LOOK_AROUND || b == Behavior.LOOK_AT_SKY || b == Behavior.TALK ||
                    b == Behavior.WATCH_MOCHI || b == Behavior.RARE_SHOOTING_STAR || b == Behavior.SCENE_MOMENT
                if (!inPlace) set(b, 0f)
            }
        } else if (c.prefersSeated) {
            // The loft: mostly in-place life, with the occasional trip to the window or shelf.
            scale(Behavior.WANDER, 0f)
            scale(Behavior.VISIT_PROP, 0.5f)
            scale(Behavior.APPROACH_MOCHI, 0.3f)
            scale(Behavior.APPROACH_PARTNER, 0f)
            add(Behavior.REST, 6f)
        }
    }

    private fun applyMemory(memory: BehaviorMemory) {
        for (b in Behavior.entries) {
            if (memory.cooldownOf(b) > 0f) {
                weights[b.ordinal] = 0f
                continue
            }
            val r = memory.recency(b)
            if (r >= 0) weights[b.ordinal] *= RECENCY_PENALTY[r]
        }
    }

    companion object {
        /** How close to their home spot counts as home (world units). */
        const val HOME_TOLERANCE = 0.005f
        const val TALK_DISTANCE = 0.24f
        const val CUDDLE_DISTANCE = 0.18f
        /**
         * Weight of asking for something when a request fits. CoupleRequests' long gaps already
         * keep requests rare, so once one is allowed it should come fairly soon (at 3 it often
         * didn't come for many minutes).
         */
        const val REQUEST_WEIGHT = 8f

        /** Weight multiplier by how recently a behavior was done (most recent first). */
        private val RECENCY_PENALTY = floatArrayOf(0.12f, 0.35f, 0.6f, 0.85f)
    }
}
