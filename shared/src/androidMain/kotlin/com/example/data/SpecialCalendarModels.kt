package com.example.data

import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalDateTime

/** [TinyUsMemory] from a java.time date, for Android code and tests. */
fun TinyUsMemory(
    id: String,
    date: LocalDate,
    title: String,
    description: String,
    type: SpecialMemoryType,
    isPrivate: Boolean = false,
    isFuture: Boolean = false,
    annualRecurring: Boolean = false
): TinyUsMemory = TinyUsMemory(id, date.toKotlinLocalDate(), title, description, type, isPrivate, isFuture, annualRecurring)

/**
 * The special calendar for Android code, in java.time types. The calendar itself is shared
 * ([CoupleCalendar]); this keeps its settings there.
 */
object SpecialCalendarManager {

    // Single source of truth for the relationship start date
    val startDate: LocalDate get() = RelationshipTimeManager.relationshipStartDate

    // Optional custom memory entries loaded from local data layer or user entries
    var customMemories: List<TinyUsMemory>?
        get() = CoupleCalendar.customMemories
        set(value) {
            CoupleCalendar.customMemories = value
        }

    var boyBirthday: LocalDate?
        get() = CoupleDates.boyBirthday?.toJavaLocalDate()
        set(value) {
            CoupleDates.boyBirthday = value?.toKotlinLocalDate()
        }
    var girlBirthday: LocalDate?
        get() = CoupleDates.girlBirthday?.toJavaLocalDate()
        set(value) {
            CoupleDates.girlBirthday = value?.toKotlinLocalDate()
        }
    var boyName: String
        get() = CoupleCalendar.boyName
        set(value) {
            CoupleCalendar.boyName = value
        }
    var girlName: String
        get() = CoupleCalendar.girlName
        set(value) {
            CoupleCalendar.girlName = value
        }

    // Base template memories (dynamic based on relationship start date & birthdays)
    val fixedMemories: List<TinyUsMemory>
        get() = CoupleCalendar.fixedMemories

    fun getMemoriesForDate(date: LocalDate): List<TinyUsMemory> = CoupleCalendar.getMemoriesForDate(date.toKotlinLocalDate())

    fun hasMemoryOnDate(date: LocalDate): Boolean = getMemoriesForDate(date).isNotEmpty()

    fun calculateCountdown(targetDate: LocalDate, now: LocalDateTime = LocalDateTime.now()): LiveCountdown =
        CoupleCalendar.calculateCountdown(targetDate.toKotlinLocalDate(), now.toKotlinLocalDateTime())
}
