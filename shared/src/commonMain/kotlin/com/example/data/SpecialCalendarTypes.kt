package com.example.data

enum class SpecialMemoryType {
    RELATIONSHIP,
    KISS,
    BIRTHDAY,
    PRIVATE,
    FUTURE_MEETING
}

data class LiveCountdown(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val isToday: Boolean,
    val isPassed: Boolean
)
