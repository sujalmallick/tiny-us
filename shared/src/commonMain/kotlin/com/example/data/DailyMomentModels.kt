package com.example.data

import kotlinx.datetime.number

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
        DailyPrompt("prompt_10", "What is one thing you are grateful for right now at this very moment?"),
        DailyPrompt("prompt_11", "What made you laugh today, even a little?"),
        DailyPrompt("prompt_12", "What is one thing you want us to do more often?"),
        DailyPrompt("prompt_13", "What is a small thing I do that makes you feel looked after?"),
        DailyPrompt("prompt_14", "If today had a color, what would it be?"),
        DailyPrompt("prompt_15", "What is something you are looking forward to this week?"),
        DailyPrompt("prompt_16", "Which of our inside jokes still makes you smile?"),
        DailyPrompt("prompt_17", "What is one thing you learned about me this month?"),
        DailyPrompt("prompt_18", "Where would you like us to have breakfast one day?"),
        DailyPrompt("prompt_19", "What is a song you would put on a playlist just for us?"),
        DailyPrompt("prompt_20", "What is a little worry I could help you carry today?"),
        DailyPrompt("prompt_21", "What is the coziest place you have ever been?"),
        DailyPrompt("prompt_22", "What is something new you would like to try together?"),
        DailyPrompt("prompt_23", "What did you dream about recently?"),
        DailyPrompt("prompt_24", "What is a compliment you have never told me but think often?"),
        DailyPrompt("prompt_25", "What is one thing that always calms you down?"),
        DailyPrompt("prompt_26", "Which day of ours would you like to live again?"),
        DailyPrompt("prompt_27", "What snack should always be in our kitchen?"),
        DailyPrompt("prompt_28", "If we wrote a tiny book about us, what would the first chapter be called?"),
        DailyPrompt("prompt_29", "What is a habit you want to start, and how can I help?"),
        DailyPrompt("prompt_30", "What is your favorite way to spend a rainy evening with me?"),
        DailyPrompt("prompt_31", "What is something you are proud of this week?"),
        DailyPrompt("prompt_32", "If we had a tiny shop together, what would we sell?"),
        DailyPrompt("prompt_33", "What is one place in our town you would like to explore together?"),
        DailyPrompt("prompt_34", "What is a smell that reminds you of home?"),
        DailyPrompt("prompt_35", "What would a perfect lazy morning look like?"),
        DailyPrompt("prompt_36", "What is a movie we should watch together soon?"),
        DailyPrompt("prompt_37", "What is the best gift you have ever received, from anyone?"),
        DailyPrompt("prompt_38", "What is something small that made today better?"),
        DailyPrompt("prompt_39", "What would you like to tell your younger self about us?"),
        DailyPrompt("prompt_40", "What is a tradition you would like us to start?"),
        DailyPrompt("prompt_41", "Which of my hugs is your favorite: the quick one or the long one?"),
        DailyPrompt("prompt_42", "What is a place you would love to show me from your childhood?"),
        DailyPrompt("prompt_43", "What is something you find easier because of me?"),
        DailyPrompt("prompt_44", "What is a word that describes us this month?"),
        DailyPrompt("prompt_45", "What are three things you are grateful for tonight?"),
        DailyPrompt("prompt_46", "What is a meal you would cook for me if you had all day?"),
        DailyPrompt("prompt_47", "What do you need a little more of this week: rest, fun or quiet?"),
        DailyPrompt("prompt_48", "What is a silly thing you would like us to do together?"),
        DailyPrompt("prompt_49", "What is your favorite photo of us, and why?"),
        DailyPrompt("prompt_50", "What is one thing that made you feel loved recently?"),
        DailyPrompt("prompt_51", "If we could have any pet in our tiny world, what would it be?"),
        DailyPrompt("prompt_52", "What is a question you have always wanted to ask me?"),
        DailyPrompt("prompt_53", "What is something you would like to be better at?"),
        DailyPrompt("prompt_54", "Where would you like us to be in one year?"),
        DailyPrompt("prompt_55", "What is a small kindness you saw today?"),
        DailyPrompt("prompt_56", "What is one thing about our home that you love?"),
        DailyPrompt("prompt_57", "What is a sound that makes you feel safe?"),
        DailyPrompt("prompt_58", "If we had one free day tomorrow, how would we spend it?"),
        DailyPrompt("prompt_59", "What is something you appreciate about how we handle hard days?"),
        DailyPrompt("prompt_60", "What is a little adventure we could have this weekend?"),
        DailyPrompt("prompt_61", "What is a song that reminds you of a trip?"),
        DailyPrompt("prompt_62", "What is something you would like to hear from me more often?"),
        DailyPrompt("prompt_63", "What made you think of me today?"),
        DailyPrompt("prompt_64", "What is your favorite thing about mornings with me?"),
        DailyPrompt("prompt_65", "What would you pack for a surprise weekend away?"),
        DailyPrompt("prompt_66", "What is a promise you would like to make to us?"),
        DailyPrompt("prompt_67", "What is something that felt hard lately, and how are you now?"),
        DailyPrompt("prompt_68", "What is the best advice you have ever been given?"),
        DailyPrompt("prompt_69", "What is a tiny luxury that makes your day?"),
        DailyPrompt("prompt_70", "If our love was a weather, what would it be today?")
    )

    /** A few questions for each season (plan 09, B): one day in three asks one of these. */
    val seasonalPrompts: Map<String, List<DailyPrompt>> = mapOf(
        "SPRING" to listOf(
            DailyPrompt("prompt_spring_1", "What would you like to plant together this spring?", category = "SPRING"),
            DailyPrompt("prompt_spring_2", "Which flower reminds you of me, and why?", category = "SPRING"),
            DailyPrompt("prompt_spring_3", "What is something you want to start fresh this spring?", category = "SPRING"),
            DailyPrompt("prompt_spring_4", "Where would you like us to have a spring picnic?", category = "SPRING"),
            DailyPrompt("prompt_spring_5", "What is your favorite sign that spring has arrived?", category = "SPRING"),
            DailyPrompt("prompt_spring_6", "What would we name a little garden of our own?", category = "SPRING"),
            DailyPrompt("prompt_spring_7", "What is a spring memory from when you were small?", category = "SPRING"),
            DailyPrompt("prompt_spring_8", "Which spring smell do you love most?", category = "SPRING"),
            DailyPrompt("prompt_spring_9", "What is one thing you want to clear out, and one to keep?", category = "SPRING"),
            DailyPrompt("prompt_spring_10", "What would a perfect spring afternoon be for us?", category = "SPRING"),
            DailyPrompt("prompt_spring_11", "What bird or animal would you like to spot this spring?", category = "SPRING"),
            DailyPrompt("prompt_spring_12", "What is a hope you have for the months ahead?", category = "SPRING"),
            DailyPrompt("prompt_spring_13", "What spring outfit makes you feel happiest?", category = "SPRING"),
            DailyPrompt("prompt_spring_14", "What is a walk you would like to take when the blossoms come?", category = "SPRING"),
            DailyPrompt("prompt_spring_15", "What would you write on a seed packet for us?", category = "SPRING")
        ),
        "SUMMER" to listOf(
            DailyPrompt("prompt_summer_1", "What is your favorite summer evening sound?", category = "SUMMER"),
            DailyPrompt("prompt_summer_2", "Where would you like to watch a summer sunset with me?", category = "SUMMER"),
            DailyPrompt("prompt_summer_3", "What is the best ice cream flavor for a hot day?", category = "SUMMER"),
            DailyPrompt("prompt_summer_4", "What would be on our summer bucket list?", category = "SUMMER"),
            DailyPrompt("prompt_summer_5", "What is a summer memory that still makes you smile?", category = "SUMMER"),
            DailyPrompt("prompt_summer_6", "Beach day or a day by a lake, and why?", category = "SUMMER"),
            DailyPrompt("prompt_summer_7", "What would we write on a paper lantern this summer?", category = "SUMMER"),
            DailyPrompt("prompt_summer_8", "What is a summer song you never get tired of?", category = "SUMMER"),
            DailyPrompt("prompt_summer_9", "What is the coolest place to hide on a hot afternoon?", category = "SUMMER"),
            DailyPrompt("prompt_summer_10", "What fruit tastes most like summer to you?", category = "SUMMER"),
            DailyPrompt("prompt_summer_11", "What is a summer night you would like to have again?", category = "SUMMER"),
            DailyPrompt("prompt_summer_12", "What is something you would like to try outdoors this summer?", category = "SUMMER"),
            DailyPrompt("prompt_summer_13", "How do you like to stay cozy on a warm night?", category = "SUMMER"),
            DailyPrompt("prompt_summer_14", "Where would our perfect summer trip be?", category = "SUMMER"),
            DailyPrompt("prompt_summer_15", "What is a summer smell you love?", category = "SUMMER")
        ),
        "AUTUMN" to listOf(
            DailyPrompt("prompt_autumn_1", "What is your favorite thing about autumn evenings?", category = "AUTUMN"),
            DailyPrompt("prompt_autumn_2", "Which warm drink is the best for an autumn day?", category = "AUTUMN"),
            DailyPrompt("prompt_autumn_3", "What would we cook together on a chilly evening?", category = "AUTUMN"),
            DailyPrompt("prompt_autumn_4", "What is an autumn memory you hold close?", category = "AUTUMN"),
            DailyPrompt("prompt_autumn_5", "What color of leaves do you like most?", category = "AUTUMN"),
            DailyPrompt("prompt_autumn_6", "What cozy thing should we do when the days get shorter?", category = "AUTUMN"),
            DailyPrompt("prompt_autumn_7", "What would a perfect autumn walk be for us?", category = "AUTUMN"),
            DailyPrompt("prompt_autumn_8", "What are you grateful for this harvest season?", category = "AUTUMN"),
            DailyPrompt("prompt_autumn_9", "What book would you like to read under a blanket this autumn?", category = "AUTUMN"),
            DailyPrompt("prompt_autumn_10", "What is a sweater or scarf you love wearing?", category = "AUTUMN"),
            DailyPrompt("prompt_autumn_11", "What is something you would like to finish before winter?", category = "AUTUMN"),
            DailyPrompt("prompt_autumn_12", "What is your favorite autumn sound?", category = "AUTUMN"),
            DailyPrompt("prompt_autumn_13", "What would we bake for a harvest fair?", category = "AUTUMN"),
            DailyPrompt("prompt_autumn_14", "What makes a rainy autumn day feel cozy to you?", category = "AUTUMN"),
            DailyPrompt("prompt_autumn_15", "What is a little change you would like to make this autumn?", category = "AUTUMN")
        ),
        "WINTER" to listOf(
            DailyPrompt("prompt_winter_1", "What is your favorite way to warm up on a cold day?", category = "WINTER"),
            DailyPrompt("prompt_winter_2", "What would you like us to give each other this winter?", category = "WINTER"),
            DailyPrompt("prompt_winter_3", "What is a winter memory you love?", category = "WINTER"),
            DailyPrompt("prompt_winter_4", "Snow day plans: what are we doing first?", category = "WINTER"),
            DailyPrompt("prompt_winter_5", "What is the coziest winter drink?", category = "WINTER"),
            DailyPrompt("prompt_winter_6", "What would you write in a card to me this winter?", category = "WINTER"),
            DailyPrompt("prompt_winter_7", "What is a winter tradition you would like us to keep?", category = "WINTER"),
            DailyPrompt("prompt_winter_8", "What would we build if it snowed tonight?", category = "WINTER"),
            DailyPrompt("prompt_winter_9", "What is a winter song that makes you feel warm?", category = "WINTER"),
            DailyPrompt("prompt_winter_10", "What are you most grateful for this year?", category = "WINTER"),
            DailyPrompt("prompt_winter_11", "What is something you want to let go of before the new year?", category = "WINTER"),
            DailyPrompt("prompt_winter_12", "What is a hope you have for us next year?", category = "WINTER"),
            DailyPrompt("prompt_winter_13", "Where would you like to spend a snowy weekend?", category = "WINTER"),
            DailyPrompt("prompt_winter_14", "What is the best thing about long winter nights?", category = "WINTER"),
            DailyPrompt("prompt_winter_15", "What would make this winter feel special for us?", category = "WINTER")
        )
    )

    /** Every prompt, for looking one up by id (old answers keep their question). */
    val allPrompts: List<DailyPrompt> by lazy { defaultPrompts + seasonalPrompts.values.flatten() }

    fun byId(id: String): DailyPrompt? = allPrompts.firstOrNull { it.id == id }

    /**
     * The prompt for [dayIndex] in [season] (`SPRING`, `SUMMER`, `AUTUMN` or `WINTER`): every
     * third day a seasonal one, the other days the everyday deck. Without a season, the everyday deck.
     */
    fun forDay(dayIndex: Int, season: String?): DailyPrompt {
        val list = season?.let { seasonalPrompts[it] }
        if (list.isNullOrEmpty() || ((dayIndex % 3) + 3) % 3 != 0) return getPromptForDay(dayIndex)
        val i = ((dayIndex / 3) % list.size + list.size) % list.size
        return list[i]
    }

    /** Today's prompt, for the couple's day count [dayIndex], in this hemisphere's season. */
    fun today(dayIndex: Int): DailyPrompt {
        val month = CoupleDates.today().month.number
        val region = androidx.compose.ui.text.intl.Locale.current.region
        return forDay(dayIndex, com.example.engine.SeasonalWeather.seasonOf(month, region))
    }

    fun getPromptForDay(dayIndex: Int): DailyPrompt {
        val safeIndex = ((dayIndex % defaultPrompts.size) + defaultPrompts.size) % defaultPrompts.size
        return defaultPrompts[safeIndex]
    }
}
