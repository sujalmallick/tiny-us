import SwiftUI
import UIKit
import Shared

/// Hosts Compose Multiplatform UI from the shared Kotlin module inside SwiftUI (plan 08).
struct SharedComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.SharedComposeViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/// The shared pixel world (SceneEngine + PixelWorldView from the Kotlin module), as on Android.
struct SharedWorldView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.SharedWorldViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}

    /// Switching to the classic world: the shared world's music stops with it.
    static func dismantleUIViewController(_ uiViewController: UIViewController, coordinator: ()) {
        SharedSound.shared.stop()
    }
}

/// The whole app shared with Android: main screen, menus and dialogs (plan 08, S5).
struct SharedMainView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.SharedMainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}

    static func dismantleUIViewController(_ uiViewController: UIViewController, coordinator: ()) {
        SharedSound.shared.stop()
    }
}
