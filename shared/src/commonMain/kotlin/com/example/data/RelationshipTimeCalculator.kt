package com.example.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * Platform-independent relationship time calculation engine.
 * Pure Kotlin Multiplatform logic using kotlinx-datetime.
 */
object RelationshipTimeCalculator {

    /**
     * Calculate current day of Tiny Us from start date to current date.
     * Starts at Day 1 on the start date.
     */
    fun calculateTinyUsDay(start: LocalDate, current: LocalDate): Long {
        val daysBetween = start.daysUntil(current).toLong()
        return (daysBetween + 1L).coerceAtLeast(1L)
    }

    /**
     * Platform-agnostic countdown computation.
     */
    fun computeCountdown(
        totalSecondsRemaining: Long,
        isSameDay: Boolean
    ): LiveCountdown {
        if (isSameDay) {
            return LiveCountdown(days = 0, hours = 0, minutes = 0, seconds = 0, isToday = true, isPassed = false)
        }
        if (totalSecondsRemaining < 0) {
            return LiveCountdown(days = 0, hours = 0, minutes = 0, seconds = 0, isToday = false, isPassed = true)
        }
        val days = totalSecondsRemaining / 86400
        val hours = (totalSecondsRemaining % 86400) / 3600
        val mins = (totalSecondsRemaining % 3600) / 60
        val secs = totalSecondsRemaining % 60
        return LiveCountdown(days = days, hours = hours, minutes = mins, seconds = secs, isToday = false, isPassed = false)
    }
}
