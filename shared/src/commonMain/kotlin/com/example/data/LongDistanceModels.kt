package com.example.data

/**
 * Types of asynchronous signals couples can send to each other in Long-Distance Mode.
 * Designed to feel warm, gentle, and tactile without intrusive messaging bloat.
 */
enum class LongDistanceSignalType(
    val id: String,
    val title: String,
    val animationKey: String
) {
    THINKING_OF_YOU("thinking_of_you", "Thinking of You", "THOUGHT_BUBBLE"),
    GOOD_MORNING("good_morning", "Good Morning", "SUNSHINE_WAVE"),
    GOOD_NIGHT("good_night", "Good Night", "STARRY_SLEEP"),
    SEND_HEART("send_heart", "Sending a Heart", "HEART_BURST"),
    NEED_A_HUG("need_a_hug", "Need a Hug", "COZY_HUG"),
    TINY_GIFT("tiny_gift", "Tiny Sweet Gift", "GIFT_BOX");

    companion object {
        fun fromId(id: String): LongDistanceSignalType =
            values().find { it.id.equals(id, ignoreCase = true) } ?: THINKING_OF_YOU
    }
}

/**
 * An asynchronous signal sent between partners.
 * Stored locally and safely queued; idempotent delivery.
 */
data class LongDistanceSignal(
    val id: String,
    val sender: String, // "boy" or "girl"
    val type: LongDistanceSignalType,
    val note: String? = null,
    val timestamp: Long,
    val isViewed: Boolean = false
)
