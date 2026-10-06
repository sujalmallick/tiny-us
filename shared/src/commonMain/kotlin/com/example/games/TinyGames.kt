package com.example.games

import kotlin.random.Random

/** The three Tiny Games (plan 09, B). Each round is five questions on one shared phone. */
enum class TinyGame {
    /** Both pick in secret, then the picks are revealed: a match or not. */
    THIS_OR_THAT,
    /** One answers about themselves, the other guesses; who answers alternates. */
    GUESS_ME,
    /** Questions made from the couple's own story, answered together; each has a right answer. */
    STORY_QUIZ;

    /** The id used for best scores in progress. */
    val progressId: String get() = "tiny_" + name.lowercase()
}

/** One question: a prompt and its options; [correct] is set for Story Quiz questions. */
data class TinyQuestion(
    val id: String,
    val prompt: String,
    val options: List<String>,
    val correct: Int? = null
)

object TinyDecks {
    const val ROUND_SIZE = 5

    /**
     * Questions from deck lines written as `prompt|option|option[|option...]`. Lines with fewer
     * than two options are skipped. Ids are [prefix] plus the line's position, so they stay stable.
     */
    fun parse(prefix: String, lines: List<String>): List<TinyQuestion> =
        lines.mapIndexedNotNull { i, line ->
            val parts = line.split('|').map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size < 3) null else TinyQuestion("$prefix$i", parts[0], parts.drop(1))
        }

    /**
     * [count] different questions from [deck], leaving out the ones in [recent] (asked in the last
     * rounds) while there are enough others.
     */
    fun draw(deck: List<TinyQuestion>, count: Int, random: Random, recent: Set<String> = emptySet()): List<TinyQuestion> {
        val fresh = deck.filter { it.id !in recent }.shuffled(random)
        if (fresh.size >= count) return fresh.take(count)
        return (fresh + deck.filter { it.id in recent }.shuffled(random)).take(count)
    }
}

/** Where a question is: the first pick, the second pick, or both shown. */
enum class TurnStep { FIRST, SECOND, REVEAL }

/**
 * A round of five questions, turn by turn. On a shared phone the picks are made one after the
 * other with the screen hidden in between ("pass the phone"); [pickerIsBoy] says whose turn it is.
 */
class TinyRound(val game: TinyGame, val questions: List<TinyQuestion>, private val boyFirst: Boolean = true) {
    var index: Int = 0
        private set
    var step: TurnStep = TurnStep.FIRST
        private set
    private val boyPicks = IntArray(questions.size) { -1 }
    private val girlPicks = IntArray(questions.size) { -1 }
    private val matched = BooleanArray(questions.size)

    val total: Int get() = questions.size
    val current: TinyQuestion get() = questions[index]
    val isLast: Boolean get() = index == questions.size - 1

    /** Matches (or right answers in the quiz) so far. */
    val score: Int get() = (0 until index + (if (step == TurnStep.REVEAL) 1 else 0)).count { matched[it] }

    /** In Guess Me: whether this question is about the boy (he answers, she guesses). */
    fun isAboutBoy(i: Int = index): Boolean = if (i % 2 == 0) boyFirst else !boyFirst

    /** Whose turn it is to pick, or null when it's a joint answer (the quiz) or the reveal. */
    val pickerIsBoy: Boolean?
        get() = when {
            step == TurnStep.REVEAL || game == TinyGame.STORY_QUIZ -> null
            game == TinyGame.GUESS_ME -> if (step == TurnStep.FIRST) isAboutBoy() else !isAboutBoy()
            else -> if (step == TurnStep.FIRST) boyFirst else !boyFirst
        }

    /** A pick for whoever's turn it is (both, in the quiz). */
    fun pick(option: Int) {
        if (step == TurnStep.REVEAL || option !in current.options.indices) return
        when (pickerIsBoy) {
            null -> { boyPicks[index] = option; girlPicks[index] = option }
            true -> boyPicks[index] = option
            false -> girlPicks[index] = option
        }
        step = if (step == TurnStep.FIRST && game != TinyGame.STORY_QUIZ) TurnStep.SECOND else TurnStep.REVEAL
        if (step == TurnStep.REVEAL) {
            matched[index] = if (game == TinyGame.STORY_QUIZ) boyPicks[index] == current.correct
            else boyPicks[index] == girlPicks[index]
        }
    }

    fun boyPick(i: Int = index): Int = boyPicks[i]
    fun girlPick(i: Int = index): Int = girlPicks[i]

    /** At the reveal: a match (or a right answer in the quiz). */
    val isMatch: Boolean get() = step == TurnStep.REVEAL && matched[index]

    /** On to the next question; false when the round is over. */
    fun next(): Boolean {
        if (step != TurnStep.REVEAL || isLast) return false
        index++
        step = TurnStep.FIRST
        return true
    }

    val isFinished: Boolean get() = isLast && step == TurnStep.REVEAL
}

/**
 * The couple's own story, for the quiz: everything is optional, and a kind of question only
 * appears when there's enough for it (a right answer and at least one wrong one).
 */
data class StoryQuizFacts(
    /** Anniversary month (1..12) and day of the week (1 = Monday..7), if known. */
    val anniversaryMonth: Int? = null,
    val anniversaryWeekday: Int? = null,
    val daysTogether: Long? = null,
    /** Polaroids: (title, scene name). */
    val photos: List<Pair<String, String>> = emptyList(),
    val allSceneNames: List<String> = emptyList(),
    val dishesCooked: List<String> = emptyList(),
    val dishesNotCooked: List<String> = emptyList(),
    val found: List<String> = emptyList(),
    val notFound: List<String> = emptyList(),
    val firstsEarned: List<String> = emptyList(),
    val firstsNotEarned: List<String> = emptyList(),
    val caught: List<String> = emptyList(),
    val notCaught: List<String> = emptyList()
)

/** The quiz's question texts, already translated (so this file stays free of resources). */
class StoryQuizTexts(
    val month: String,
    val weekday: String,
    val days: String,
    val daysOption: (Int) -> String,
    val photo: (String) -> String,
    val recipe: String,
    val found: String,
    val first: String,
    val catch: String,
    val monthNames: List<String>,
    val weekdayNames: List<String>
)

object StoryQuiz {
    /** Fewer than this many questions and the quiz stays hidden. */
    const val MIN_QUESTIONS = TinyDecks.ROUND_SIZE

    /** Every question the couple's story can make right now, shuffled. */
    fun questions(facts: StoryQuizFacts, texts: StoryQuizTexts, random: Random): List<TinyQuestion> {
        val list = ArrayList<TinyQuestion>()
        fun add(id: String, prompt: String, right: String, wrong: List<String>) {
            val decoys = wrong.filter { it != right }.distinct().shuffled(random).take(3)
            if (decoys.isEmpty()) return
            val options = (decoys + right).shuffled(random)
            list += TinyQuestion(id, prompt, options, options.indexOf(right))
        }
        facts.anniversaryMonth?.let { m ->
            if (m in 1..12 && texts.monthNames.size == 12) add("quiz_month", texts.month, texts.monthNames[m - 1], texts.monthNames)
        }
        facts.anniversaryWeekday?.let { d ->
            if (d in 1..7 && texts.weekdayNames.size == 7) add("quiz_weekday", texts.weekday, texts.weekdayNames[d - 1], texts.weekdayNames)
        }
        facts.daysTogether?.takeIf { it >= 10 }?.let { days ->
            val right = days.toInt()
            val wrong = listOf(0.55, 0.75, 1.35, 1.7).map { (right * it).toInt() }.filter { it != right && it > 0 }
            add("quiz_days", texts.days, texts.daysOption(right), wrong.map(texts.daysOption))
        }
        facts.photos.distinctBy { it.first }.take(3).forEachIndexed { i, (title, scene) ->
            add("quiz_photo_$i", texts.photo(title), scene, facts.allSceneNames)
        }
        facts.dishesCooked.randomOrNull(random)?.let { add("quiz_recipe", texts.recipe, it, facts.dishesNotCooked) }
        facts.found.randomOrNull(random)?.let { add("quiz_found", texts.found, it, facts.notFound) }
        facts.firstsEarned.randomOrNull(random)?.let { add("quiz_first", texts.first, it, facts.firstsNotEarned) }
        facts.caught.randomOrNull(random)?.let { add("quiz_catch", texts.catch, it, facts.notCaught) }
        return list.shuffled(random)
    }
}
