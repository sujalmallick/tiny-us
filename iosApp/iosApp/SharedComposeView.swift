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
