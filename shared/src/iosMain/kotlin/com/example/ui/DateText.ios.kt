package com.example.ui

import kotlinx.datetime.LocalDate
import platform.Foundation.* // currentLocale is an Objective-C category property

actual object DateText {
    private val utc = NSTimeZone.timeZoneWithName("UTC")

    actual fun format(date: LocalDate, pattern: String, english: Boolean): String {
        // Read the ISO date the same way in every locale and calendar, then write it in the phone's language.
        val parser = NSDateFormatter()
        parser.locale = NSLocale(localeIdentifier = "en_US_POSIX")
        parser.timeZone = utc!!
        parser.dateFormat = "yyyy-MM-dd"
        val day = parser.dateFromString(date.toString()) ?: return date.toString()
        val formatter = NSDateFormatter()
        formatter.locale = if (english) NSLocale(localeIdentifier = "en_US") else NSLocale.currentLocale
        formatter.timeZone = utc
        formatter.dateFormat = pattern
        return formatter.stringFromDate(day)
    }

    actual fun mediumDate(date: LocalDate): String {
        val parser = NSDateFormatter()
        parser.locale = NSLocale(localeIdentifier = "en_US_POSIX")
        parser.timeZone = utc!!
        parser.dateFormat = "yyyy-MM-dd"
        val day = parser.dateFromString(date.toString()) ?: return date.toString()
        val formatter = NSDateFormatter()
        formatter.locale = NSLocale.currentLocale
        formatter.timeZone = utc
        formatter.dateStyle = NSDateFormatterMediumStyle
        formatter.timeStyle = NSDateFormatterNoStyle
        return formatter.stringFromDate(day)
    }

    @Suppress("UNCHECKED_CAST")
    actual fun shortWeekdays(): List<String> {
        val formatter = NSDateFormatter()
        formatter.locale = NSLocale.currentLocale
        // Foundation lists Sunday first.
        val sundayFirst = formatter.shortWeekdaySymbols as List<String>
        return sundayFirst.drop(1) + sundayFirst.take(1)
    }

    actual fun grouped(value: Long): String {
        val formatter = NSNumberFormatter()
        formatter.locale = NSLocale.currentLocale
        formatter.numberStyle = NSNumberFormatterDecimalStyle
        return formatter.stringFromNumber(NSNumber(longLong = value)) ?: value.toString()
    }
}
