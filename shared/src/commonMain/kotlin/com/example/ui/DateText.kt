package com.example.ui

import kotlinx.datetime.LocalDate

/** Dates and numbers written out for people, in the phone's language. */
expect object DateText {
    /**
     * [date] in a pattern such as "MMM dd, yyyy" or "MMMM yyyy" (the same letters on Android and
     * iOS); [english] keeps English month names whatever the phone's language.
     */
    fun format(date: LocalDate, pattern: String, english: Boolean = false): String

    /** Short weekday names, Monday first. */
    fun shortWeekdays(): List<String>

    /** [value] with the phone's thousands separators. */
    fun grouped(value: Long): String
}
