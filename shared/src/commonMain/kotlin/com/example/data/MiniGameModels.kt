package com.example.data

/**
 * Types of intimate two-person mini-games.
 * Designed for 30 seconds to 3 minutes, asynchronous participation.
 */
enum class MiniGameType(val title: String) {
    WOULD_YOU_RATHER("Would You Rather"),
    WHO_KNOWS_WHO("Who Knows Who?"),
    SWEET_PREFERENCE("Sweet Preferences"),
    MEMORY_TRIVIA("Our Memory Trivia")
}

/**
 * A question definition for a couple mini-game.
 */
data class MiniGameQuestion(
    val id: String,
    val type: MiniGameType,
    val prompt: String,
    val options: List<String>
)

/**
 * An active or completed round of a mini-game.
 * Supports asynchronous answering: Partner A answers now, Partner B answers later.
 */
data class MiniGameRound(
    val id: String,
    val questionId: String,
    val type: MiniGameType,
    val prompt: String,
    val options: List<String>,
    val boyChosenIndex: Int? = null,
    val girlChosenIndex: Int? = null,
    val isRevealed: Boolean = false,
    val timestamp: Long = 0L
) {
    val isAnsweredByBoy: Boolean get() = boyChosenIndex != null
    val isAnsweredByGirl: Boolean get() = girlChosenIndex != null
    val isBothAnswered: Boolean get() = isAnsweredByBoy && isAnsweredByGirl
    val isMatch: Boolean get() = isBothAnswered && (boyChosenIndex == girlChosenIndex)
}

object MiniGameCatalog {
    val questions: List<MiniGameQuestion> = listOf(
        MiniGameQuestion(
            id = "wyr_1",
            type = MiniGameType.WOULD_YOU_RATHER,
            prompt = "Would you rather have a cozy weekend cabin in the mountains or a breezy beach cottage?",
            options = listOf("Mountain Cabin with Fireplace", "Beach Cottage with Sea Breeze")
        ),
        MiniGameQuestion(
            id = "wyr_2",
            type = MiniGameType.WOULD_YOU_RATHER,
            prompt = "Would you rather wake up early to watch sunrise together or stay up late stargazing?",
            options = listOf("Sunrise with Coffee", "Midnight Stargazing")
        ),
        MiniGameQuestion(
            id = "wkw_1",
            type = MiniGameType.WHO_KNOWS_WHO,
            prompt = "What is your partner's ultimate comfort snack on a rainy day?",
            options = listOf("Warm Dumplings / Momos", "Hot Chocolate & Cookies", "Noodle Soup", "Crispy Fries")
        ),
        MiniGameQuestion(
            id = "pref_1",
            type = MiniGameType.SWEET_PREFERENCE,
            prompt = "Pick the ideal way to spend a quiet Sunday afternoon together:",
            options = listOf("Reading & Napping", "Baking something sweet", "Watching movies under a blanket", "Taking a slow stroll in the park")
        ),
        MiniGameQuestion(
            id = "wyr_3",
            type = MiniGameType.WOULD_YOU_RATHER,
            prompt = "Would you rather always have unlimited warm tea/coffee or unlimited cozy hoodies?",
            options = listOf("Unlimited Warm Drinks", "Endless Soft Hoodies")
        )
    )
}
