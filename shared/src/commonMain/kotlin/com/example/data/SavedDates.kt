package com.example.data

/** Dates written into saved items the way the phone shows them. */
expect object SavedDates {
    /** Today as "Oct 6, 2026" ([withYear]) or "Oct 6", in the phone's language. */
    fun shortDate(withYear: Boolean): String

    /** The time now as "6:42 PM", in the phone's language. */
    fun shortTime(): String
}
