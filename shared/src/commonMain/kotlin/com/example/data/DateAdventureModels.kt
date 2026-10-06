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
        ),
        DateAdventure(
            id = "adv_breakfast_picnic",
            title = "Breakfast Picnic",
            description = "Pack something simple for breakfast and eat it somewhere new: a park bench, a balcony, or a blanket by the window.",
            category = "SWEET_TREAT",
            sceneHint = "MEADOW"
        ),
        DateAdventure(
            id = "adv_bookshop_swap",
            title = "Bookshop Swap",
            description = "Visit a bookshop or library and each pick a book for the other. Swap them and read the first page out loud.",
            category = "COZY_HOME",
            sceneHint = "COZY_LOFT"
        ),
        DateAdventure(
            id = "adv_postcard",
            title = "Postcard to the Future",
            description = "Write a postcard together to the two of you one year from now. Hide it somewhere you will find it later.",
            category = "DEEP_TALK",
            sceneHint = "LIVING_ROOM"
        ),
        DateAdventure(
            id = "adv_new_recipe",
            title = "A Recipe Neither of You Know",
            description = "Pick a dish from a country you have never cooked from, and make it together, mistakes and all.",
            category = "SWEET_TREAT",
            sceneHint = "KITCHEN"
        ),
        DateAdventure(
            id = "adv_dance_kitchen",
            title = "Kitchen Dance",
            description = "Put on one song each and dance in the kitchen until both songs end.",
            category = "COZY_HOME",
            sceneHint = "KITCHEN"
        ),
        DateAdventure(
            id = "adv_photo_walk",
            title = "Photo Walk",
            description = "Take a slow walk and each take five photos of little things the other might miss.",
            category = "NATURE_WALK",
            sceneHint = "MEADOW"
        ),
        DateAdventure(
            id = "adv_blanket_fort",
            title = "Blanket Fort Night",
            description = "Build a blanket fort, bring snacks inside, and tell each other a story from your childhood.",
            category = "COZY_HOME",
            sceneHint = "LIVING_ROOM"
        ),
        DateAdventure(
            id = "adv_sunrise",
            title = "Catch a Sunrise",
            description = "Set an alarm, bring something warm to drink, and watch the sun come up together.",
            category = "NATURE_WALK",
            sceneHint = "LOOKING"
        ),
        DateAdventure(
            id = "adv_thank_you_notes",
            title = "Three Little Thank-Yous",
            description = "Each write three small thank-you notes to the other and hide them around the home.",
            category = "DEEP_TALK",
            sceneHint = "LIVING_ROOM"
        ),
        DateAdventure(
            id = "adv_market",
            title = "Market Morning",
            description = "Visit a local market and choose one ingredient each for a meal you cook together.",
            category = "SWEET_TREAT",
            sceneHint = "MOMO_STALL"
        ),
        DateAdventure(
            id = "adv_plant_something",
            title = "Plant Something",
            description = "Plant a herb, a flower or a seed in a pot together and give it a name.",
            category = "NATURE_WALK",
            sceneHint = "SUNROOM"
        ),
        DateAdventure(
            id = "adv_old_photos",
            title = "Old Photo Evening",
            description = "Scroll back to your oldest photos together and pick a favorite from each year.",
            category = "COZY_HOME",
            sceneHint = "LIVING_ROOM"
        ),
        DateAdventure(
            id = "adv_learn_words",
            title = "Five New Words",
            description = "Learn five words in a language neither of you speaks, and use them all day.",
            category = "DEEP_TALK",
            sceneHint = null
        ),
        DateAdventure(
            id = "adv_stargaze_app",
            title = "Name a Constellation",
            description = "On a clear night, find a group of stars and make up your own name and story for it.",
            category = "NATURE_WALK",
            sceneHint = "LOOKING"
        ),
        DateAdventure(
            id = "adv_letter_mail",
            title = "Real Mail",
            description = "Write each other a short letter and post it, even if you live together.",
            category = "DEEP_TALK",
            sceneHint = null
        ),
        DateAdventure(
            id = "adv_cafe_hop",
            title = "Café Hop",
            description = "Visit two cafés in one afternoon and share one small treat at each.",
            category = "SWEET_TREAT",
            sceneHint = "RAINY_CAFE"
        ),
        DateAdventure(
            id = "adv_puzzle",
            title = "A Puzzle Evening",
            description = "Start a jigsaw puzzle together. It does not have to be finished tonight.",
            category = "QUIET_MOMENT",
            sceneHint = "LIVING_ROOM"
        ),
        DateAdventure(
            id = "adv_rain_walk",
            title = "Walk in the Rain",
            description = "Next time it rains softly, take an umbrella and go for a short walk together.",
            category = "NATURE_WALK",
            sceneHint = "WALK"
        ),
        DateAdventure(
            id = "adv_playlist",
            title = "Our Playlist",
            description = "Make a playlist of ten songs together, five each, and listen to it start to finish.",
            category = "QUIET_MOMENT",
            sceneHint = "COZY_LOFT"
        ),
        DateAdventure(
            id = "adv_volunteer",
            title = "A Kind Hour",
            description = "Spend an hour doing something kind together: helping a neighbor, a shelter, or a friend.",
            category = "DEEP_TALK",
            sceneHint = null
        ),
        DateAdventure(
            id = "adv_sketch",
            title = "Draw Each Other",
            description = "Take five minutes to draw each other. No skill needed, only good humor.",
            category = "COZY_HOME",
            sceneHint = "LIVING_ROOM"
        ),
        DateAdventure(
            id = "adv_pier_day",
            title = "A Day by the Water",
            description = "Find a lake, river or sea and sit by the water together for a while.",
            category = "NATURE_WALK",
            sceneHint = "SEASIDE_PIER"
        ),
        DateAdventure(
            id = "adv_campfire",
            title = "Campfire Stories",
            description = "Light a candle or a small fire, turn off the lights and tell each other spooky or silly stories.",
            category = "QUIET_MOMENT",
            sceneHint = "CAMPFIRE"
        ),
        DateAdventure(
            id = "adv_no_plan_day",
            title = "A Day Without Plans",
            description = "Spend half a day with no plans at all and follow whatever you both feel like doing.",
            category = "QUIET_MOMENT",
            sceneHint = null
        ),
        DateAdventure(
            id = "adv_bucket_list",
            title = "Our Little Bucket List",
            description = "Write ten small things you want to do together this year and pin the list somewhere you will see it.",
            category = "DEEP_TALK",
            sceneHint = "LIVING_ROOM"
        )
    )
}
