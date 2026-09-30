package com.example.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

enum class SpecialMemoryType {
    RELATIONSHIP,
    KISS,
    BIRTHDAY,
    PRIVATE,
    FUTURE_MEETING
}

data class TinyUsMemory(
    val id: String,
    val date: LocalDate,
    val title: String,
    val description: String,
    val type: SpecialMemoryType,
    val isPrivate: Boolean = false,
    val isFuture: Boolean = false,
    val annualRecurring: Boolean = false
)

data class LiveCountdown(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val isToday: Boolean,
    val isPassed: Boolean
)

object SpecialCalendarManager {

    // Single source of truth for the relationship start date
    val startDate: LocalDate get() = RelationshipTimeManager.relationshipStartDate

    // Optional custom memory entries loaded from local data layer or user entries
    var customMemories: List<TinyUsMemory>? = null

    var boyBirthday: LocalDate? = null
    var girlBirthday: LocalDate? = null
    var boyName: String = "Him"
    var girlName: String = "Her"

    // Base template memories (dynamic based on relationship start date & birthdays)
    val fixedMemories: List<TinyUsMemory>
        get() {
            customMemories?.let { return it }
            val list = mutableListOf(
                TinyUsMemory(
                    id = "our_beginning",
                    date = startDate,
                    title = "Our Beginning",
                    description = "Where our story began. The first day of forever.",
                    type = SpecialMemoryType.RELATIONSHIP,
                    annualRecurring = true
                )
            )
            boyBirthday?.let { bDate ->
                list.add(
                    TinyUsMemory(
                        id = "boy_birthday",
                        date = bDate,
                        title = "${boyName}'s Birthday",
                        description = "Celebrating the most wonderful person in the world!",
                        type = SpecialMemoryType.BIRTHDAY,
                        annualRecurring = true
                    )
                )
            }
            girlBirthday?.let { gDate ->
                list.add(
                    TinyUsMemory(
                        id = "girl_birthday",
                        date = gDate,
                        title = "${girlName}'s Birthday",
                        description = "Celebrating the most special, beautiful soul!",
                        type = SpecialMemoryType.BIRTHDAY,
                        annualRecurring = true
                    )
                )
            }
            return list
        }

    fun getMemoriesForDate(date: LocalDate): List<TinyUsMemory> {
        return fixedMemories.filter { mem ->
            if (mem.annualRecurring) {
                mem.date.month == date.month && mem.date.dayOfMonth == date.dayOfMonth
            } else {
                mem.date == date
            }
        }
    }

    fun hasMemoryOnDate(date: LocalDate): Boolean {
        return getMemoriesForDate(date).isNotEmpty()
    }

    fun calculateCountdown(targetDate: LocalDate, now: LocalDateTime = LocalDateTime.now()): LiveCountdown {
        val targetDateTime = targetDate.atStartOfDay()
        val totalSecs = ChronoUnit.SECONDS.between(now, targetDateTime)
        if (now.toLocalDate() == targetDate) {
            return LiveCountdown(days = 0, hours = 0, minutes = 0, seconds = 0, isToday = true, isPassed = false)
        }
        if (totalSecs < 0) {
            return LiveCountdown(days = 0, hours = 0, minutes = 0, seconds = 0, isToday = false, isPassed = true)
        }
        val days = totalSecs / 86400
        val hours = (totalSecs % 86400) / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return LiveCountdown(days = days, hours = hours, minutes = mins, seconds = secs, isToday = false, isPassed = false)
    }
}
