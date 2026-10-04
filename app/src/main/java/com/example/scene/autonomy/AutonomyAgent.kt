package com.example.scene.autonomy

/** Where a character is in their decide → walk → perform → rest routine. */
enum class AgentPhase {
    /** Resting between activities, counting down to the next decision. */
    IDLE,
    /** Walking to the chosen target. */
    WALKING,
    /** Doing the chosen thing (or joining in with the partner's). */
    PERFORMING
}

/** One character's autonomous routine. Reused for the whole session; never reallocated per frame. */
class AutonomyAgent {
    val memory = BehaviorMemory()
    var phase: AgentPhase = AgentPhase.IDLE
    var behavior: Behavior? = null
    var decisionTimer: Float = 0f
    var performTimer: Float = 0f
    /** Generic sub-step clock for multi-beat performances (look left, then right...). */
    var stepTimer: Float = 0f
    var step: Int = 0
    var walkTimer: Float = 0f
    var targetX: Float = 0f
    var targetY: Float = 0f
    var spot: SceneSpot? = null
    var lastSpotId: Int = -1
    var activitiesSinceHome: Int = 0
    /** True while this character is following the partner's lead (talk, hug, scene moment...). */
    var isFollower: Boolean = false
    /** Seconds to hold off on new plans because the partner is walking over to them. */
    var waitForPartner: Float = 0f

    fun reset(firstDecisionIn: Float) {
        phase = AgentPhase.IDLE
        behavior = null
        decisionTimer = firstDecisionIn
        performTimer = 0f
        stepTimer = 0f
        step = 0
        walkTimer = 0f
        spot = null
        lastSpotId = -1
        activitiesSinceHome = 0
        isFollower = false
        waitForPartner = 0f
        memory.clear()
    }

    /** Back to resting; the next decision comes after [restSeconds]. */
    fun rest(restSeconds: Float) {
        phase = AgentPhase.IDLE
        behavior = null
        spot = null
        isFollower = false
        performTimer = 0f
        decisionTimer = restSeconds
    }
}
