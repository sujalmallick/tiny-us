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
}
