import SwiftUI
import Shared
import UIKit

/// The app: the shared Tiny Us (the same world, menus and dialogs as Android, plan 08). If it
/// stopped the app last time, the error shows first so it can be copied and sent along.
struct ContentView: View {
    /// What stopped the shared app last time (read once at launch), if anything.
    @State private var problem: String? = LaunchDiagnostics.shared.lastProblem()
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        if let problem = problem {
            ProblemView(problem: problem) {
                LaunchDiagnostics.shared.clear()
                self.problem = nil
            }
        } else {
            SharedMainView()
                .ignoresSafeArea()
                .preferredColorScheme(.light)
                .overlay {
                    // With the app lock's "Hide in recent apps", the app switcher sees a plain cover.
                    if scenePhase != .active && IosLock.shared.shouldHidePreview() {
                        PrivacyCover()
                    }
                }
        }
    }
}

/// Shown at launch when the shared app stopped last time, so the error can be sent along.
private struct ProblemView: View {
    let problem: String
    let onRetry: () -> Void
    @State private var copied = false

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 14) {
                Text("Tiny Us stopped last time. Copy the error to send it along, then try again.")
                    .font(.system(.subheadline, design: .rounded))
                ScrollView {
                    Text(problem).font(.system(.caption2, design: .monospaced)).textSelection(.enabled)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(10).background(.quaternary, in: RoundedRectangle(cornerRadius: 10))
                Button { UIPasteboard.general.string = problem; copied = true } label: {
                    Label(copied ? "Copied" : "Copy the error", systemImage: copied ? "checkmark" : "doc.on.doc")
                }.buttonStyle(.borderedProminent)
                Button("Try again", action: onRetry).buttonStyle(.bordered)
            }
            .padding(20)
            .navigationTitle("Something went wrong").navigationBarTitleDisplayMode(.inline)
        }
    }
}

/// What the app switcher shows while the app lock hides the app.
private struct PrivacyCover: View {
    var body: some View {
        ZStack {
            Color(red: 1.0, green: 0.976, blue: 0.961).ignoresSafeArea()
            Image(systemName: "lock.fill").font(.system(size: 44)).foregroundStyle(Color(red: 0.89, green: 0.45, blue: 0.53))
        }
    }
}
