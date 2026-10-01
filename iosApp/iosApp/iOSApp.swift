import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // Initialize platform storage adapter
        let storage = IosUserDefaultsStorage(defaults: UserDefaults.standard)
        _ = storage
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
