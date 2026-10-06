package com.example.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual object SavedDates {
    actual fun shortDate(withYear: Boolean): String =
        SimpleDateFormat(if (withYear) "MMM d, yyyy" else "MMM d", Locale.getDefault()).format(Date())
}
