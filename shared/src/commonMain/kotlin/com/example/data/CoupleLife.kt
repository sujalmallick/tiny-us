package com.example.data

import kotlin.random.Random
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.toLocalDateTime

/*
 * Plan 09, C: small tools for real couple life. The Dinner Decider, the Thank-You Jar, the
 * Make-Up Bench and Phones Down. ("Open when..." letters live in SealedLetters.)
 */

/** What the Dinner Decider is deciding. */
enum class DeciderCategory { EAT, WATCH, DO }

/** One option on the wheel; [author] is who added it, or null for the built-in ones. */
data class DeciderOption(val text: String, val author: Partner? = null)

/**
 * "I don't mind, you pick." Each of them secretly crosses out up to [VETOES] options; the wheel
 * spins only what survived both. If nothing survives, every option goes back on the wheel.
 */
object DinnerDecider {
    const val VETOES = 2
    const val WHEEL_SIZE = 8

    /** The options left after both sets of vetoes, as indexes into the shown options. */
    fun survivors(optionCount: Int, boyVetoes: Set<Int>, girlVetoes: Set<Int>): List<Int> {
        val left = (0 until optionCount).filter { it !in boyVetoes && it !in girlVetoes }
        return left.ifEmpty { (0 until optionCount).toList() }
    }

    /** Where the wheel stops, among [survivors]. */
    fun spin(survivors: List<Int>, random: Random): Int = survivors[random.nextInt(survivors.size)]

    /**
     * The options for one evening: every custom one first (up to [WHEEL_SIZE]), then built-in
     * ones picked at random to fill the wheel.
     */
    fun wheel(builtIn: List<String>, custom: List<DeciderOption>, random: Random): List<DeciderOption> {
        val own = custom.takeLast(WHEEL_SIZE)
        val fill = builtIn.filter { b -> own.none { it.text.equals(b, ignoreCase = true) } }
            .shuffled(random)
            .take(WHEEL_SIZE - own.size)
            .map { DeciderOption(it) }
        return (own + fill).shuffled(random)
    }
}

/** A thank-you dropped in the jar. */
data class ThankYou(val id: String, val fromBoy: Boolean, val text: String, val createdAt: Long)

/** A jar that was filled, kept as a keepsake. */
data class FilledJar(val filledOn: String, val count: Int)

object ThankYouJar {
    /** A full jar: it becomes a keepsake and a new one starts. */
    const val FULL = 20
}

/** What they chose to do after the Make-Up Bench. */
enum class MakeUpChoice { HUG, TEA, TALK_LATER }

/** A Make-Up Bench moment they chose to keep. Not kept unless they asked. */
data class MakeUpMoment(
    val id: String,
    val createdAt: Long,
    val boyFelt: String,
    val boyNeed: String,
    val girlFelt: String,
    val girlNeed: String,
    val choice: MakeUpChoice
)

/** A Phones Down session that ran its full time. */
data class PhonesDownSession(val endedAt: Long, val minutes: Int)

/**
 * Everything section C keeps, in `tiny_us_prefs` next to the rest (so backups carry it).
 */
@OptIn(ExperimentalUuidApi::class)
class CoupleLifeStore(private val storage: KeyValueStorage) {
    private fun now() = Clock.System.now().toEpochMilliseconds()

    // ── Dinner Decider ──

    fun customOptions(category: DeciderCategory): List<DeciderOption> {
        val raw = storage.getString(KEY_DECIDER + category.name, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DeciderOption(o.getString("text"), o.optString("author").takeIf { it.isNotEmpty() }?.let { Partner.valueOf(it) })
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addCustomOption(category: DeciderCategory, text: String, author: Partner) {
        val clean = text.trim().take(40)
        if (clean.isEmpty()) return
        val list = customOptions(category).filterNot { it.text.equals(clean, ignoreCase = true) } + DeciderOption(clean, author)
        saveOptions(category, list.takeLast(MAX_CUSTOM))
    }

    fun removeCustomOption(category: DeciderCategory, text: String) =
        saveOptions(category, customOptions(category).filterNot { it.text == text })

    private fun saveOptions(category: DeciderCategory, list: List<DeciderOption>) {
        val arr = JSONArray()
        list.forEach { o -> arr.put(JSONObject().apply { put("text", o.text); o.author?.let { put("author", it.name) } }) }
        storage.putString(KEY_DECIDER + category.name, arr.toString())
    }

    // ── Thank-You Jar ──

    private var cachedThanks: List<ThankYou>? = null

    /** The thank-yous in the current jar, oldest first. */
    fun thankYous(): List<ThankYou> {
        cachedThanks?.let { return it }
        val list = readThanks(KEY_THANKS)
        cachedThanks = list
        return list
    }

    /** Every thank-you ever, from filled jars too (for the reading on a rainy day). */
    fun allThankYous(): List<ThankYou> = readThanks(KEY_THANKS_ARCHIVE) + thankYous()

    /**
     * Drops a thank-you in the jar. Returns the jar that was just filled when this one filled it
     * (it's then kept and a new, empty jar starts), or null.
     */
    fun addThankYou(fromBoy: Boolean, text: String): FilledJar? {
        val clean = text.trim().take(120)
        if (clean.isEmpty()) return null
        val list = thankYous() + ThankYou(Uuid.random().toString(), fromBoy, clean, now())
        if (list.size < ThankYouJar.FULL) {
            saveThanks(KEY_THANKS, list)
            cachedThanks = list
            return null
        }
        saveThanks(KEY_THANKS_ARCHIVE, readThanks(KEY_THANKS_ARCHIVE) + list)
        saveThanks(KEY_THANKS, emptyList())
        cachedThanks = emptyList()
        val jar = FilledJar(CoupleDates.today().toString(), list.size)
        val jars = filledJars() + jar
        val arr = JSONArray()
        jars.forEach { j -> arr.put(JSONObject().apply { put("filledOn", j.filledOn); put("count", j.count) }) }
        storage.putString(KEY_JARS, arr.toString())
        return jar
    }

    fun filledJars(): List<FilledJar> {
        val raw = storage.getString(KEY_JARS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i -> arr.getJSONObject(i).let { FilledJar(it.getString("filledOn"), it.getInt("count")) } }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** One old thank-you to read aloud, not one from today, or null. */
    fun pickToReadAloud(random: Random): ThankYou? {
        val today = CoupleDates.today().toString()
        val zone = kotlinx.datetime.TimeZone.currentSystemDefault()
        fun dayOf(ms: Long) = kotlin.time.Instant.fromEpochMilliseconds(ms).toLocalDateTime(zone).date.toString()
        val old = allThankYous().filter { it.createdAt > 0 && dayOf(it.createdAt) != today }
        return old.randomOrNull(random)
    }

    /** The day a thank-you was last read aloud on a rainy day (once a day at most). */
    var lastRainyReading: String
        get() = storage.getString(KEY_RAINY_READING, "") ?: ""
        set(value) = storage.putString(KEY_RAINY_READING, value)

    private fun readThanks(key: String): List<ThankYou> {
        val raw = storage.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ThankYou(o.getString("id"), o.optBoolean("fromBoy"), o.getString("text"), o.optLong("createdAt"))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveThanks(key: String, list: List<ThankYou>) {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id); put("fromBoy", t.fromBoy); put("text", t.text); put("createdAt", t.createdAt)
            })
        }
        storage.putString(key, arr.toString())
    }

    // ── Make-Up Bench ──

    fun keptMakeUps(): List<MakeUpMoment> {
        val raw = storage.getString(KEY_MAKEUPS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                MakeUpMoment(
                    o.getString("id"), o.optLong("createdAt"),
                    o.optString("boyFelt"), o.optString("boyNeed"), o.optString("girlFelt"), o.optString("girlNeed"),
                    runCatching { MakeUpChoice.valueOf(o.getString("choice")) }.getOrDefault(MakeUpChoice.HUG)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Keeps a Make-Up Bench moment (only when they choose to). */
    fun keepMakeUp(boyFelt: String, boyNeed: String, girlFelt: String, girlNeed: String, choice: MakeUpChoice): MakeUpMoment {
        val m = MakeUpMoment(Uuid.random().toString(), now(), boyFelt.trim(), boyNeed.trim(), girlFelt.trim(), girlNeed.trim(), choice)
        val arr = JSONArray()
        (keptMakeUps() + m).forEach { k ->
            arr.put(JSONObject().apply {
                put("id", k.id); put("createdAt", k.createdAt)
                put("boyFelt", k.boyFelt); put("boyNeed", k.boyNeed); put("girlFelt", k.girlFelt); put("girlNeed", k.girlNeed)
                put("choice", k.choice.name)
            })
        }
        storage.putString(KEY_MAKEUPS, arr.toString())
        return m
    }

    /** "Talk later": when they said they'd come back to it (epoch millis), or 0. */
    var talkLaterAt: Long
        get() = storage.getLong(KEY_TALK_LATER, 0L)
        set(value) = storage.putLong(KEY_TALK_LATER, value)

    // ── Phones Down ──

    /** A running session: when it started (epoch millis) and for how long, or 0. */
    var phonesDownStartedAt: Long
        get() = storage.getLong(KEY_PD_START, 0L)
        set(value) = storage.putLong(KEY_PD_START, value)

    var phonesDownMinutes: Int
        get() = storage.getInt(KEY_PD_MINUTES, 0)
        set(value) = storage.putInt(KEY_PD_MINUTES, value)

    fun startPhonesDown(minutes: Int, at: Long = now()) {
        phonesDownStartedAt = at
        phonesDownMinutes = minutes
    }

    /** Seconds left in the running session, or null when none is running. */
    fun phonesDownSecondsLeft(at: Long = now()): Long? {
        if (phonesDownStartedAt <= 0L) return null
        val end = phonesDownStartedAt + phonesDownMinutes * 60_000L
        return ((end - at) / 1000L).coerceAtLeast(0L)
    }

    /** Ends the session; true and kept when it ran its full time. */
    fun endPhonesDown(at: Long = now()): Boolean {
        val left = phonesDownSecondsLeft(at) ?: return false
        val minutes = phonesDownMinutes
        phonesDownStartedAt = 0L
        phonesDownMinutes = 0
        if (left > 0L) return false
        val arr = JSONArray()
        (phonesDownSessions() + PhonesDownSession(at, minutes)).forEach { s ->
            arr.put(JSONObject().apply { put("endedAt", s.endedAt); put("minutes", s.minutes) })
        }
        storage.putString(KEY_PD_SESSIONS, arr.toString())
        return true
    }

    fun phonesDownSessions(): List<PhonesDownSession> {
        val raw = storage.getString(KEY_PD_SESSIONS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i -> arr.getJSONObject(i).let { PhonesDownSession(it.getLong("endedAt"), it.getInt("minutes")) } }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── Reminders for our dates (C-6), opt-in ──

    var anniversaryReminder: Boolean
        get() = storage.getBoolean(KEY_ANNIV_REMINDER, false)
        set(value) = storage.putBoolean(KEY_ANNIV_REMINDER, value)

    var monthiversaryReminder: Boolean
        get() = storage.getBoolean(KEY_MONTH_REMINDER, false)
        set(value) = storage.putBoolean(KEY_MONTH_REMINDER, value)

    companion object {
        const val MAX_CUSTOM = 16
        private const val KEY_DECIDER = "decider_custom_"
        private const val KEY_THANKS = "thank_you_jar_json"
        private const val KEY_THANKS_ARCHIVE = "thank_you_archive_json"
        private const val KEY_JARS = "thank_you_jars_json"
        private const val KEY_RAINY_READING = "thank_you_rainy_reading"
        private const val KEY_MAKEUPS = "make_up_moments_json"
        private const val KEY_TALK_LATER = "make_up_talk_later"
        private const val KEY_PD_START = "phones_down_started_at"
        private const val KEY_PD_MINUTES = "phones_down_minutes"
        private const val KEY_PD_SESSIONS = "phones_down_sessions_json"
        private const val KEY_ANNIV_REMINDER = "reminder_anniversary"
        private const val KEY_MONTH_REMINDER = "reminder_monthiversary"
    }
}
