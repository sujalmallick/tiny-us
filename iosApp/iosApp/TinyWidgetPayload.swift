import Foundation

/// App Group identifiers shared by Tiny Us and its WidgetKit extension. Must match both .entitlements files.
enum TinyAppGroup {
    static let suiteName = "group.com.example.tinyus.shared"
    static let payloadKey = "tiny-us.widget.payload"
    static let widgetKind = "TinyUsWidget"
    /// Without the entitlement (unsigned sideloads) iOS keeps this suite app-local, so it still persists.
    static var defaults: UserDefaults { UserDefaults(suiteName: suiteName) ?? .standard }
}

/// App Group payload shared by Tiny Us and its WidgetKit extension.
struct TinyWidgetPayload: Codable {
    var coupleNames: String
    var daysTogether: Int64
    var anniversaryDate: Date
    var sceneName: String
    var weatherName: String
    var timePhase: String
    var dailyMomentPrompt: String
    var sharedMoodEmoji: String
    var latestSignalText: String
    var updatedAt: Date

    static let empty = TinyWidgetPayload(
        coupleNames: "Tiny Us", daysTogether: 0, anniversaryDate: .now, sceneName: "A little world",
        weatherName: "Sunny breeze", timePhase: "A cozy moment",
        dailyMomentPrompt: "What tiny thing made you smile today?",
        sharedMoodEmoji: "💛", latestSignalText: "Open Tiny Us for a little hello.", updatedAt: .now
    )
}
