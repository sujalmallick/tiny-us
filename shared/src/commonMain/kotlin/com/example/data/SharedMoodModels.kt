package com.example.data

/**
 * Lightweight, non-intrusive shared mood states.
 * Respectful, calm, and designed without manipulative analytics.
 */
enum class SharedMoodType(
    val id: String,
    val displayName: String,
    val emoji: String,
    val subtleDescription: String
) {
    GREAT("great", "Great", "✨", "Full of bright energy and warm smiles"),
    GOOD("good", "Good", "🌸", "Peaceful, cozy, and content"),
    TIRED("tired", "Tired", "☕", "Gentle, sleepy, and in need of quiet rest"),
    LOW("low", "A Bit Low", "🌧️", "Needs extra soft warmth and sweet reassurance"),
    MISSING_YOU("missing_you", "Missing You", "💌", "Thinking of you across the distance");

    companion object {
        fun fromId(id: String): SharedMoodType = values().find { it.id.equals(id, ignoreCase = true) } ?: GOOD
    }
}

/**
 * Current mood state for both partners with privacy control.
 */
data class PartnerMoodState(
    val boyMood: SharedMoodType = SharedMoodType.GOOD,
    val boyMoodShared: Boolean = true,
    val girlMood: SharedMoodType = SharedMoodType.GOOD,
    val girlMoodShared: Boolean = true,
    val lastUpdated: Long = 0L
) {
    /**
     * Resolves the visible mood for the partner, respecting privacy setting.
     */
    fun getVisibleMoodForPartner(isViewerGirl: Boolean): SharedMoodType? {
        return if (isViewerGirl) {
            if (boyMoodShared) boyMood else null
        } else {
            if (girlMoodShared) girlMood else null
        }
    }
}
