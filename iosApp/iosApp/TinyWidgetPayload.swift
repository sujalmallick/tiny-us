import Foundation

/// App Group payload shared by Tiny Us and its WidgetKit extension.
struct TinyWidgetPayload: Codable {
    var coupleNames: String
    var daysTogether: Int64
    var sceneName: String
    var weatherName: String
    var timePhase: String
    var dailyMomentPrompt: String
    var sharedMoodEmoji: String
    var latestSignalText: String
    var updatedAt: Date

    static let empty = TinyWidgetPayload(
        coupleNames: "Tiny Us", daysTogether: 0, sceneName: "A little world",
        weatherName: "Sunny breeze", timePhase: "A cozy moment",
        dailyMomentPrompt: "What tiny thing made you smile today?",
        sharedMoodEmoji: "💛", latestSignalText: "Open Tiny Us for a little hello.", updatedAt: .now
    )
}
