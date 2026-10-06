package com.example.games

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TinyGamesTest {
    private val deck = TinyDecks.parse("t_", (0 until 12).map { "Question $it?|A$it|B$it" } + "broken line|only one")

    @Test
    fun deckLinesParseWithStableIdsAndBadLinesSkipped() {
        assertEquals(12, deck.size)
        assertEquals("t_3", deck[3].id)
        assertEquals(listOf("A3", "B3"), deck[3].options)
    }

    @Test
    fun aRoundNeverRepeatsAndPrefersFreshQuestions() {
        val random = Random(4)
        val first = TinyDecks.draw(deck, 5, random)
        assertEquals(5, first.map { it.id }.toSet().size)
        val second = TinyDecks.draw(deck, 5, random, recent = first.map { it.id }.toSet())
        assertTrue(second.none { it in first })
        // Not enough fresh ones: the rest come from the recent ones, still without repeats.
        val third = TinyDecks.draw(deck, 5, random, recent = (first + second).map { it.id }.toSet())
        assertEquals(5, third.map { it.id }.toSet().size)
    }

    @Test
    fun thisOrThatTakesTurnsThenReveals() {
        val round = TinyRound(TinyGame.THIS_OR_THAT, deck.take(5), boyFirst = false)
        assertEquals(false, round.pickerIsBoy) // she goes first this time
        round.pick(1)
        assertEquals(TurnStep.SECOND, round.step)
        assertEquals(true, round.pickerIsBoy)
        round.pick(1)
        assertEquals(TurnStep.REVEAL, round.step)
        assertNull(round.pickerIsBoy)
        assertTrue(round.isMatch)
        assertEquals(1, round.score)

        round.next()
        round.pick(0); round.pick(1)
        assertFalse(round.isMatch)
        assertEquals(1, round.score)
        repeat(3) { round.next(); round.pick(0); round.pick(0) }
        assertTrue(round.isFinished)
        assertEquals(4, round.score)
        assertFalse(round.next())
    }

    @Test
    fun guessMeAlternatesWhoAnswersAboutThemselves() {
        val round = TinyRound(TinyGame.GUESS_ME, deck.take(5), boyFirst = true)
        assertTrue(round.isAboutBoy())
        assertEquals(true, round.pickerIsBoy) // he answers about himself
        round.pick(0)
        assertEquals(false, round.pickerIsBoy) // she guesses
        round.pick(0)
        assertTrue(round.isMatch)
        round.next()
        assertFalse(round.isAboutBoy())
        assertEquals(false, round.pickerIsBoy) // now she answers about herself
    }

    @Test
    fun theQuizIsAnsweredTogetherAndHasRightAnswers() {
        val q = TinyQuestion("q", "Which month?", listOf("May", "June"), correct = 1)
        val round = TinyRound(TinyGame.STORY_QUIZ, listOf(q, q.copy(id = "q2")))
        assertNull(round.pickerIsBoy)
        round.pick(1)
        assertEquals(TurnStep.REVEAL, round.step)
        assertTrue(round.isMatch)
        round.next()
        round.pick(0)
        assertFalse(round.isMatch)
        assertEquals(1, round.score)
    }

    private val texts = StoryQuizTexts(
        month = "month?", weekday = "weekday?", days = "days?", daysOption = { "$it days" },
        photo = { "photo $it?" }, recipe = "recipe?", found = "found?", first = "first?", catch = "catch?",
        monthNames = (1..12).map { "M$it" }, weekdayNames = (1..7).map { "D$it" }
    )

    @Test
    fun theQuizNeedsEnoughStoryAndIsNeverMadeUp() {
        // Only the anniversary: not enough for a round, so the quiz stays hidden.
        val little = StoryQuiz.questions(StoryQuizFacts(anniversaryMonth = 3, anniversaryWeekday = 5, daysTogether = 200), texts, Random(1))
        assertTrue(little.size < StoryQuiz.MIN_QUESTIONS)

        val facts = StoryQuizFacts(
            anniversaryMonth = 3, anniversaryWeekday = 5, daysTogether = 412,
            photos = listOf("Tea for two" to "Rainy Cafe", "Our pier" to "Seaside Pier"),
            allSceneNames = listOf("Rainy Cafe", "Seaside Pier", "Cozy Loft", "Sunroom"),
            dishesCooked = listOf("Pancakes"), dishesNotCooked = listOf("Soup", "Tea"),
            found = listOf("A seashell"), notFound = listOf("A red leaf"),
            firstsEarned = listOf("First rainbow"), firstsNotEarned = listOf("First snowman", "First fish")
        )
        val questions = StoryQuiz.questions(facts, texts, Random(2))
        assertTrue(questions.size >= StoryQuiz.MIN_QUESTIONS)
        for (q in questions) {
            val right = q.correct!!
            assertTrue(right in q.options.indices, q.id)
            assertEquals(q.options.size, q.options.toSet().size, "options repeat in ${q.id}")
            assertTrue(q.options.size >= 2)
        }
        val month = questions.first { it.id == "quiz_month" }
        assertEquals("M3", month.options[month.correct!!])
        val days = questions.first { it.id == "quiz_days" }
        assertEquals("412 days", days.options[days.correct!!])
        val photo = questions.first { it.prompt == "photo Tea for two?" }
        assertEquals("Rainy Cafe", photo.options[photo.correct!!])
    }
}
