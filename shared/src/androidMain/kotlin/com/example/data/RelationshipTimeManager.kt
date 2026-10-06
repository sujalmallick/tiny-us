package com.example.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toKotlinLocalTime

/**
 * Relationship time for Android code, in java.time types. 100% offline, local device time based.
 * The calculation itself is shared ([CoupleCalendar]); this keeps the start date and time there.
 */
object RelationshipTimeManager {
    // Configurable relationship start date (defaults to profile anniversary or today)
    var relationshipStartDate: LocalDate = ProfileManager.getProfile().anniversaryDate?.toJavaLocalDate() ?: LocalDate.now()
        set(value) {
            field = value
            CoupleDates.anniversary = value.toKotlinLocalDate()
        }

    init {
        CoupleDates.anniversary = relationshipStartDate.toKotlinLocalDate()
    }

    // Configurable exact start time: default 00:00 (can be updated with exact hour & minute)
    var relationshipStartTime: LocalTime
        get() = CoupleCalendar.startTime.toJavaLocalTime()
        set(value) {
            CoupleCalendar.startTime = value.toKotlinLocalTime()
        }

    /**
     * Calculate current day of Tiny Us.
     * Starts at Day 1 on relationshipStartDate.
     */
    fun calculateTinyUsDay(currentDate: LocalDate = LocalDate.now()): Long =
        CoupleCalendar.tinyUsDay(currentDate.toKotlinLocalDate())

    /**
     * Calculate exact live duration down to the second.
     */
    fun getExactDuration(now: LocalDateTime = LocalDateTime.now()): RelationshipDuration =
        CoupleCalendar.exactDuration(now.toKotlinLocalDateTime())
}
