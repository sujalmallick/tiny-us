package com.example.scene

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.Partner
import com.example.data.SealedLetter
import kotlinx.datetime.LocalDate

/** The beats of a birthday surprise (plan 09, A), in order. */
enum class SurpriseStep {
    /** The room is dark ("why is it so dark?"); a tap turns the light on. */
    DARK,
    /** The light comes up. */
    LIGHTS,
    /** "Surprise!": confetti, the cake, a hat for the birthday person and for Mochi. */
    SURPRISE,
    /** Tap to blow out the candles, one by one. */
    CANDLES,
    /** An optional wish, typed in the overlay (skipped when no overlay is showing). */
    WISH,
    /** A gift box waits; a tap opens it. */
    GIFT,
    /** The sealed birthday letter is open in the overlay. */
    LETTER,
    /** Over: the party carries on for the rest of the day. */
    DONE
}

/**
 * The state of today's birthday surprise. The engine moves it along and plays it in the world;
 * the overlay (wish field, letter) reads it. Kept apart from [SceneEngine] so the beats are easy
 * to follow and to test.
 */
class BirthdaySurprise {
    /** Compose state, so the overlay follows the beats. */
    var step: SurpriseStep? by mutableStateOf(null)
        internal set
    /** Seconds since the current step began. */
    var stepTime: Float = 0f
        internal set
    /** Whose birthday: one of them, or both when they share the day. */
    var birthdayOf: List<Partner> = emptyList()
        internal set
    /** The birthday being celebrated (this year's date). */
    var date: LocalDate? = null
        internal set
    /** True when the party is a few days late (the app wasn't opened on the day). */
    var belated: Boolean = false
        internal set
    /** Sealed birthday letters to open at the gift, in order. */
    var letters: List<SealedLetter> = emptyList()
        internal set
    /** Which of [letters] is showing in the LETTER step. */
    var letterIndex: Int by mutableIntStateOf(0)
        internal set
    var wish: String = ""
        internal set
    /** The day the party mode lasts for (hats, banner, birthday lines), once the surprise is over. */
    var partyDay: LocalDate? = null
        internal set

    val isRunning: Boolean get() = step != null && step != SurpriseStep.DONE

    /** How dark the room is, 0 to 1, for the overlay drawn over the world. */
    val darkness: Float
        get() = when (step) {
            SurpriseStep.DARK -> 1f
            SurpriseStep.LIGHTS -> (1f - stepTime / LIGHTS_SECONDS).coerceIn(0f, 1f)
            else -> 0f
        }

    /** The letter showing now, in the LETTER step. */
    val currentLetter: SealedLetter? get() = if (step == SurpriseStep.LETTER) letters.getOrNull(letterIndex) else null

    /** The overlay should ask for a wish. */
    val wantsWish: Boolean get() = step == SurpriseStep.WISH

    /** The gift box stands in the room (until it's opened). */
    val showsGift: Boolean get() = step == SurpriseStep.GIFT

    /** The banner and confetti are up: from the surprise to the end of the day. */
    fun partyOn(today: LocalDate): Boolean =
        (step != null && step != SurpriseStep.DARK && step != SurpriseStep.LIGHTS) || partyDay == today

    internal fun begin(who: List<Partner>, on: LocalDate, late: Boolean, sealed: List<SealedLetter>) {
        birthdayOf = who
        date = on
        belated = late
        letters = sealed
        letterIndex = 0
        wish = ""
        go(SurpriseStep.DARK)
    }

    internal fun go(next: SurpriseStep) {
        step = next
        stepTime = 0f
    }

    internal fun clear() {
        step = null
        stepTime = 0f
        letters = emptyList()
    }

    companion object {
        const val LIGHTS_SECONDS = 0.8f
        const val SURPRISE_SECONDS = 3.4f
        /** A short pause after the last candle before the wish. */
        const val AFTER_CANDLES_SECONDS = 1.0f
        /** The wish field waits this long before the surprise moves on by itself. */
        const val WISH_TIMEOUT_SECONDS = 90f
        const val CANDLES = 3
    }
}
