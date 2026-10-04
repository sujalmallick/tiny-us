package com.example.data

/**
 * Platform-independent data payload representing the current state of Tiny Us for home-screen widgets.
 * Suitable for Android AppWidget / Glance and iOS WidgetKit.
 */
data class TinyUsWidgetData(
    val coupleNames: String,
    val daysTogether: Long,
    val sceneName: String,
    val weatherName: String,
    val timePhase: String,
    val dailyMomentPrompt: String? = null,
    val dailyMomentAnswered: Boolean = false,
    val latestSignalText: String? = null,
    val sharedMoodText: String? = null,
    val lastUpdatedTimestamp: Long = 0L
)
