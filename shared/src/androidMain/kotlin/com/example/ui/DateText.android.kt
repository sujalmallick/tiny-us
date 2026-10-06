package com.example.ui

import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate

actual object DateText {
    actual fun format(date: LocalDate, pattern: String, english: Boolean): String =
        date.toJavaLocalDate().format(DateTimeFormatter.ofPattern(pattern, if (english) Locale.ENGLISH else Locale.getDefault()))

    actual fun mediumDate(date: LocalDate): String =
        date.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    actual fun shortWeekdays(): List<String> =
        DayOfWeek.entries.map { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }

    actual fun grouped(value: Long): String = NumberFormat.getNumberInstance().format(value)
}
