package com.example.data

/**
 * A thoughtful daily relationship reflection prompt.
 * Asks the couple to think, answer, share, or reflect.
 */
data class DailyPrompt(
    val id: String,
    val question: String,
    val category: String = "SWEET_REFLECTIONS"
)

/**
 * Asynchronous response state for a Daily Tiny Moment.
 * Partner A can answer first; Partner B answers later.
 * Answers can be revealed together when both complete, or previewed.
 */
data class DailyMomentResponse(
    val promptId: String,
    val dateString: String, // e.g. "2026-10-01"
    val boyAnswer: String? = null,
    val girlAnswer: String? = null,
    val isRevealed: Boolean = false,
    val completedTimestamp: Long? = null
) {
    val isAnsweredByBoy: Boolean get() = !boyAnswer.isNullOrBlank()
    val isAnsweredByGirl: Boolean get() = !girlAnswer.isNullOrBlank()
    val isBothAnswered: Boolean get() = isAnsweredByBoy && isAnsweredByGirl
}

/**
 * Curated catalog of thoughtful daily couple questions.
 */
object DailyPromptCatalog {
    val defaultPrompts: List<DailyPrompt> = listOf(
        DailyPrompt("prompt_1", "What is one tiny thing your partner did recently that made you secretly smile?"),
        DailyPrompt("prompt_2", "If we could teleport anywhere in the world tonight for just one hour, where would we go?"),
        DailyPrompt("prompt_3", "Describe your day today using just one cozy word."),
        DailyPrompt("prompt_4", "What is one little habit of mine that you find adorable?"),
        DailyPrompt("prompt_5", "What is your favorite quiet memory of us where nothing exciting happened, but it was perfect?"),
        DailyPrompt("prompt_6", "If we adopted another little pet to keep Mochi company, what kind of animal would you pick?"),
        DailyPrompt("prompt_7", "What is a song that instantly makes you think of us whenever it plays?"),
        DailyPrompt("prompt_8", "What is one meal or sweet treat you could never get tired of sharing with me?"),
        DailyPrompt("prompt_9", "What is a dream destination you hope our little characters visit someday?"),
        DailyPrompt("prompt_10", "What is one thing you are grateful for right now at this very moment?")
    )

    fun getPromptForDay(dayIndex: Int): DailyPrompt {
        val safeIndex = ((dayIndex % defaultPrompts.size) + defaultPrompts.size) % defaultPrompts.size
        return defaultPrompts[safeIndex]
    }
}
