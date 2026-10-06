import SwiftUI
import UIKit
import Shared

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
