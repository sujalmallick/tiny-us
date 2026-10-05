package com.example.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toJavaLocalDate

data class RelationshipDuration(
    val years: Int,
    val months: Int,
    val days: Int,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val totalDays: Long
)

/**
 * Single source of truth for relationship time calculation.
 * 100% offline, local device time based.
 *
 * Configurable start date & time.
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
    var relationshipStartTime: LocalTime = LocalTime.of(0, 0)

    /**
     * Calculate current day of Tiny Us.
     * Starts at Day 1 on relationshipStartDate.
     */
    fun calculateTinyUsDay(currentDate: LocalDate = LocalDate.now()): Long {
        return RelationshipTimeCalculator.calculateTinyUsDay(
            relationshipStartDate.toKotlinLocalDate(),
            currentDate.toKotlinLocalDate()
        )
    }

    /**
     * Calculate exact live duration down to the second.
     */
    fun getExactDuration(
        now: LocalDateTime = LocalDateTime.now()
    ): RelationshipDuration {
        val startDateTime = LocalDateTime.of(relationshipStartDate, relationshipStartTime)
        if (now.isBefore(startDateTime)) {
            return RelationshipDuration(0, 0, 0, 0, 0, 0, 1)
        }

        val totalDays = calculateTinyUsDay(now.toLocalDate())

        // Calculate calendar years, months, days
        val endDateForPeriod = if (now.toLocalTime().isBefore(relationshipStartTime)) {
            now.toLocalDate().minusDays(1)
        } else {
            now.toLocalDate()
        }
        val period = Period.between(relationshipStartDate, endDateForPeriod)

        // Calculate hours, minutes, seconds elapsed within the 24-hour cycle
        val nowSecondsOfDay = now.toLocalTime().toSecondOfDay()
        val startSecondsOfDay = relationshipStartTime.toSecondOfDay()
        val secondsDiff = if (nowSecondsOfDay >= startSecondsOfDay) {
            (nowSecondsOfDay - startSecondsOfDay).toLong()
        } else {
            (86400 - startSecondsOfDay + nowSecondsOfDay).toLong()
        }

        val hours = secondsDiff / 3600
        val minutes = (secondsDiff % 3600) / 60
        val seconds = secondsDiff % 60

        return RelationshipDuration(
            years = period.years,
            months = period.months,
            days = period.days,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            totalDays = totalDays
        )
    }
}
