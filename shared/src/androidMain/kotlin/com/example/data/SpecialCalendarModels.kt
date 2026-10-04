package com.example.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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

object SpecialCalendarManager {

    // Single source of truth for the relationship start date
    val startDate: LocalDate get() = RelationshipTimeManager.relationshipStartDate

    // Optional custom memory entries loaded from local data layer or user entries
    var customMemories: List<TinyUsMemory>? = null

    var boyBirthday: LocalDate? = null
    var girlBirthday: LocalDate? = null
    var boyName: String = PersonalProfile.DEFAULT_NAME_A
    var girlName: String = PersonalProfile.DEFAULT_NAME_B

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
        val isSameDay = now.toLocalDate() == targetDate
        return RelationshipTimeCalculator.computeCountdown(totalSecs, isSameDay)
    }
}
