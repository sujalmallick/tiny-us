package com.example.data

import platform.Foundation.* // currentLocale is an Objective-C category property

actual object SavedDates {
    actual fun shortDate(withYear: Boolean): String {
        val formatter = NSDateFormatter()
        formatter.locale = NSLocale.currentLocale
        // Same fields as Android's "MMM d, yyyy" / "MMM d", in the order the language uses.
        formatter.setLocalizedDateFormatFromTemplate(if (withYear) "MMMdyyyy" else "MMMd")
        return formatter.stringFromDate(NSDate())
    }

    actual fun shortTime(): String {
        val formatter = NSDateFormatter()
        formatter.locale = NSLocale.currentLocale
        formatter.dateFormat = "h:mm a"
        return formatter.stringFromDate(NSDate())
    }
}
