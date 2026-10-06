package com.example.data

import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate

/**
 * What a sealed letter waits for: a birthday (a date), a moment its reader chooses, or the day a
 * message in a bottle washes up on the line at the pier (plan 09, F).
 */
enum class LetterKind { BIRTHDAY, OPEN_WHEN, BOTTLE,
    /** A wish written on a lantern at Lantern Night (plan 09, D), kept by its writer for a year. */
    LANTERN }

/**
 * A letter one of them writes for the other and seals (plan 09, A and C). It stays hidden from
 * Love Notes and Our Story until it's opened. On a shared phone this is a matter of trust.
 */
data class SealedLetter(
    val id: String,
    val recipient: Partner,
    val kind: LetterKind,
    /** For OPEN_WHEN: the moment it's for ("when you miss me"); for BIRTHDAY: empty. */
    val occasion: String,
    val body: String,
    val author: String,
    val createdAt: Long,
    /** For BIRTHDAY: the day it may be opened (`yyyy-mm-dd`); null otherwise. */
    val opensOn: String? = null,
    /** Epoch millis when it was opened; 0 while it's still sealed. */
    val openedAt: Long = 0L
) {
    val isOpened: Boolean get() = openedAt > 0L

    /** Whether it may be opened on [today]: an open-when letter any time, a birthday letter from its day. */
    fun canOpen(today: LocalDate): Boolean = when (kind) {
        LetterKind.OPEN_WHEN -> true
        LetterKind.BIRTHDAY -> Birthdays.parse(opensOn)?.let { today >= it } ?: true
        LetterKind.BOTTLE -> Birthdays.parse(opensOn)?.let { today >= it } ?: true
        LetterKind.LANTERN -> Birthdays.parse(opensOn)?.let { today >= it } ?: false
    }
}

/** A birthday party that was held: for Our Story. The wish is kept, but only shown when asked. */
data class BirthdayRecord(
    val partner: Partner,
    /** The birthday it was for (`yyyy-mm-dd`, this year's date). */
    val date: String,
    val age: Int? = null,
    val wish: String = "",
    val letterId: String? = null,
    val belated: Boolean = false
)

/**
 * The birthday surprise's saved state (plan 09, A): sealed letters, when each party was last held,
 * and the parties themselves. Kept in `tiny_us_prefs` next to everything else, so the in-app
 * backup and Android's backup carry it.
 */
@OptIn(ExperimentalUuidApi::class)
class BirthdayStore(private val storage: KeyValueStorage) {
    private var cachedLetters: List<SealedLetter>? = null
    private var cachedRecords: List<BirthdayRecord>? = null

    /** The saved birthdays, by partner (the same keys the Settings fields use). */
    fun birthdays(): Map<Partner, LocalDate?> = mapOf(
        Partner.BOY to Birthdays.parse(storage.getString(KEY_BOY_BIRTHDAY, "")),
        Partner.GIRL to Birthdays.parse(storage.getString(KEY_GIRL_BIRTHDAY, ""))
    )

    fun setBirthday(partner: Partner, value: String) {
        storage.putString(if (partner == Partner.BOY) KEY_BOY_BIRTHDAY else KEY_GIRL_BIRTHDAY, value.trim())
        CoupleDates.boyBirthday = birthdays()[Partner.BOY]
        CoupleDates.girlBirthday = birthdays()[Partner.GIRL]
    }

    // ── Sealed letters ──

    fun letters(): List<SealedLetter> {
        cachedLetters?.let { return it }
        val raw = storage.getString(KEY_LETTERS, null)
        val list = if (raw.isNullOrEmpty()) emptyList() else try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                SealedLetter(
                    id = o.getString("id"),
                    recipient = Partner.valueOf(o.getString("recipient")),
                    kind = LetterKind.valueOf(o.getString("kind")),
                    occasion = o.optString("occasion"),
                    body = o.getString("body"),
                    author = o.optString("author"),
                    createdAt = o.optLong("createdAt"),
                    opensOn = o.optString("opensOn").takeIf { it.isNotEmpty() },
                    openedAt = o.optLong("openedAt")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
        cachedLetters = list
        return list
    }

    /** Seals a new letter for [recipient] and returns it. */
    fun seal(recipient: Partner, kind: LetterKind, body: String, author: String, occasion: String = "", opensOn: LocalDate? = null): SealedLetter {
        val letter = SealedLetter(
            id = Uuid.random().toString(),
            recipient = recipient,
            kind = kind,
            occasion = occasion.trim(),
            body = body.trim(),
            author = author.trim(),
            createdAt = Clock.System.now().toEpochMilliseconds(),
            opensOn = opensOn?.toString()
        )
        saveLetters(letters() + letter)
        return letter
    }

    fun markOpened(id: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        saveLetters(letters().map { if (it.id == id && !it.isOpened) it.copy(openedAt = now) else it })
    }

    fun delete(id: String) = saveLetters(letters().filterNot { it.id == id })

    /** The still-sealed birthday letter waiting for [partner]'s birthday on [birthday], if one was written. */
    fun birthdayLetter(partner: Partner, birthday: LocalDate): SealedLetter? =
        letters().firstOrNull {
            it.kind == LetterKind.BIRTHDAY && it.recipient == partner && !it.isOpened && it.opensOn == birthday.toString()
        }

    /** True once a sealed letter waits for [partner]'s next birthday on [birthday]. */
    fun hasBirthdayLetter(partner: Partner, birthday: LocalDate): Boolean = birthdayLetter(partner, birthday) != null

    private fun saveLetters(list: List<SealedLetter>) {
        cachedLetters = list
        val arr = JSONArray()
        list.forEach { l ->
            arr.put(JSONObject().apply {
                put("id", l.id)
                put("recipient", l.recipient.name)
                put("kind", l.kind.name)
                put("occasion", l.occasion)
                put("body", l.body)
                put("author", l.author)
                put("createdAt", l.createdAt)
                if (l.opensOn != null) put("opensOn", l.opensOn)
                if (l.openedAt > 0L) put("openedAt", l.openedAt)
            })
        }
        storage.putString(KEY_LETTERS, arr.toString())
    }

    // ── Parties ──

    /** The date of [partner]'s last party, if one was ever held. */
    fun partyHeldOn(partner: Partner): LocalDate? =
        Birthdays.parse(storage.getString(if (partner == Partner.BOY) KEY_PARTY_BOY else KEY_PARTY_GIRL, ""))

    /** Whose party is due today (see [Birthdays.partyDue]). */
    fun partyDue(today: LocalDate): List<Partner> = Birthdays.partyDue(today, birthdays(), ::partyHeldOn)

    fun records(): List<BirthdayRecord> {
        cachedRecords?.let { return it }
        val raw = storage.getString(KEY_RECORDS, null)
        val list = if (raw.isNullOrEmpty()) emptyList() else try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                BirthdayRecord(
                    partner = Partner.valueOf(o.getString("partner")),
                    date = o.getString("date"),
                    age = o.optLong("age", -1L).takeIf { it > 0L }?.toInt(),
                    wish = o.optString("wish"),
                    letterId = o.optString("letterId").takeIf { it.isNotEmpty() },
                    belated = o.optBoolean("belated")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
        cachedRecords = list
        return list
    }

    /** The party for [record.partner] happened: remember it (once per birthday). */
    fun recordParty(record: BirthdayRecord) {
        storage.putString(if (record.partner == Partner.BOY) KEY_PARTY_BOY else KEY_PARTY_GIRL, record.date)
        val list = records().filterNot { it.partner == record.partner && it.date == record.date } + record
        cachedRecords = list
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(JSONObject().apply {
                put("partner", r.partner.name)
                put("date", r.date)
                if (r.age != null) put("age", r.age)
                if (r.wish.isNotEmpty()) put("wish", r.wish)
                if (r.letterId != null) put("letterId", r.letterId)
                if (r.belated) put("belated", true)
            })
        }
        storage.putString(KEY_RECORDS, arr.toString())
    }

    /** Adds [wish] to the party record for [partner] on [date]. */
    fun saveWish(partner: Partner, date: String, wish: String) {
        val r = records().firstOrNull { it.partner == partner && it.date == date } ?: return
        recordParty(r.copy(wish = wish.trim()))
    }

    // ── Reminders (opt-in) ──

    var letterHintsEnabled: Boolean
        get() = storage.getBoolean(KEY_LETTER_HINTS, true)
        set(value) = storage.putBoolean(KEY_LETTER_HINTS, value)

    var morningReminderEnabled: Boolean
        get() = storage.getBoolean(KEY_MORNING_REMINDER, false)
        set(value) = storage.putBoolean(KEY_MORNING_REMINDER, value)

    companion object {
        const val KEY_BOY_BIRTHDAY = "bf_birthday"
        const val KEY_GIRL_BIRTHDAY = "gf_birthday"
        const val KEY_LETTERS = "sealed_letters_json"
        const val KEY_PARTY_BOY = "birthday_party_boy"
        const val KEY_PARTY_GIRL = "birthday_party_girl"
        const val KEY_RECORDS = "birthday_records_json"
        const val KEY_LETTER_HINTS = "birthday_letter_hints"
        const val KEY_MORNING_REMINDER = "birthday_morning_reminder"
    }
}
