package com.example.data

/**
 * Status lifecycle for a Tiny Date Adventure.
 * Designed without streak or countdown pressure.
 */
enum class AdventureStatus {
    AVAILABLE,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED,
    EXPIRED
}

/**
 * A small real-world activity for couples to experience together.
 */
data class DateAdventure(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // e.g. "COZY_HOME", "NATURE_WALK", "SWEET_TREAT", "DEEP_TALK", "QUIET_MOMENT"
    val sceneHint: String? = null, // e.g. "KITCHEN", "MEADOW", "COZY_LOFT"
    val status: AdventureStatus = AdventureStatus.AVAILABLE,
    val completedByBoy: Boolean = false,
    val completedByGirl: Boolean = false,
    val completedTimestamp: Long? = null,
    val unlockedArtifact: String? = null
) {
    val isFullyCompleted: Boolean get() = status == AdventureStatus.COMPLETED || (completedByBoy && completedByGirl)
}

/** Curated, wholesome real-world activities for couples. */
object DateAdventureCatalog {
    val defaultAdventures: List<DateAdventure> = listOf(
        DateAdventure(
            id = "adv_cook_treat",
            title = "Cook a Cozy Meal Together",
            description = "Step into the kitchen together, turn on your favorite tunes, and prepare a warm meal or dessert side by side.",
            category = "SWEET_TREAT",
            sceneHint = "KITCHEN",
            unlockedArtifact = "PICNIC_BASKET"
        ),
        DateAdventure(
            id = "adv_sunset_walk",
            title = "Golden Hour Stroll",
            description = "Put on comfortable shoes and take a gentle 15-minute walk outside while the sun goes down.",
            category = "NATURE_WALK",
            sceneHint = "MEADOW"
        ),
        DateAdventure(
            id = "adv_screen_free",
            title = "Phone-Free Tea Time",
            description = "Brew a warm cup of tea or cocoa and sit together for 10 quiet minutes with all phones tucked away.",
            category = "QUIET_MOMENT",
            sceneHint = "COZY_LOFT"
        ),
        DateAdventure(
            id = "adv_curious_question",
            title = "Ask an Unusual Question",
            description = "Ask each other: 'If we had a tiny bookstore by the sea, what would we name it and what snacks would we serve?'",
            category = "DEEP_TALK",
            sceneHint = "LIVING_ROOM"
        ),
        DateAdventure(
            id = "adv_stargaze_blanket",
            title = "Night Sky Lookout",
            description = "Step out onto a balcony, porch, or garden at night and point out three quiet stars in the sky.",
            category = "NATURE_WALK",
            sceneHint = "STARGAZING"
        ),
        DateAdventure(
            id = "adv_recreate_photo",
            title = "Recreate a Memory",
            description = "Snap a photo of the two of you holding hands or sharing a hug in the exact pose of an early memory.",
            category = "COZY_HOME",
            sceneHint = "LIVING_ROOM"
        ),
        DateAdventure(
            id = "adv_sweet_drink",
            title = "Make Each Other a Drink",
            description = "Surprise each other by making their favorite beverage just the way they love it.",
            category = "SWEET_TREAT",
            sceneHint = "KITCHEN"
        )
    )
}
