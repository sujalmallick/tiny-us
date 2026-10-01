import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // Initialize platform storage adapter
        let storage = IosUserDefaultsStorage()
        _ = storage
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
