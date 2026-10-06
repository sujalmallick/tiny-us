import SwiftUI
import Shared
import WidgetKit

@main
struct iOSApp: App {
    init() {
        WidgetPublisher.install()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

/// Writes the home-screen widget's payload when the shared app's scene or weather changes
/// (WidgetKit is Swift-only, so the Kotlin side hands the data over through IosWidget).
enum WidgetPublisher {
    static func install() {
        IosWidget.shared.publish = { snapshot in
            let parser = DateFormatter()
            parser.locale = Locale(identifier: "en_US_POSIX")
            parser.dateFormat = "yyyy-MM-dd"
            let payload = TinyWidgetPayload(
                coupleNames: snapshot.coupleNames,
                daysTogether: snapshot.daysTogether,
                anniversaryDate: parser.date(from: snapshot.anniversary) ?? Date(),
                sceneName: snapshot.sceneName,
                weatherName: snapshot.weatherName,
                timePhase: snapshot.timePhase,
                dailyMomentPrompt: snapshot.dailyMomentPrompt.isEmpty ? TinyWidgetPayload.empty.dailyMomentPrompt : snapshot.dailyMomentPrompt,
                sharedMoodEmoji: "heart.fill",
                latestSignalText: snapshot.latestSignalText.isEmpty ? TinyWidgetPayload.empty.latestSignalText : snapshot.latestSignalText,
                updatedAt: Date()
            )
            guard let encoded = try? JSONEncoder().encode(payload) else { return }
            TinyAppGroup.defaults.set(encoded, forKey: TinyAppGroup.payloadKey)
            WidgetCenter.shared.reloadTimelines(ofKind: TinyAppGroup.widgetKind)
        }
    }
}
