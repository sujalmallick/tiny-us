package com.example.shell

/** What the home-screen widget shows, handed to the Swift app to write for WidgetKit. */
data class IosWidgetSnapshot(
    val coupleNames: String,
    val daysTogether: Long,
    /** The anniversary as yyyy-MM-dd. */
    val anniversary: String,
    val sceneName: String,
    val weatherName: String,
    val timePhase: String,
    val dailyMomentPrompt: String,
    val latestSignalText: String
)

/**
 * The bridge to the widget: WidgetKit has no Objective-C API, so the Swift app sets [publish] at
 * launch (`IosWidget.shared.publish = { snapshot in ... }`) and writes the App Group payload itself.
 */
object IosWidget {
    var publish: ((IosWidgetSnapshot) -> Unit)? = null
}
