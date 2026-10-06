import SwiftUI
import Shared
import WidgetKit
import UserNotifications
import CryptoKit
import UniformTypeIdentifiers

@main
struct iOSApp: App {
    init() {
        WidgetPublisher.install()
        UNUserNotificationCenter.current().delegate = NotificationPresenter.shared
        IosBackupCrypto.shared.aesGcm = AesGcmBridgeImpl()
        IosFilePickers.shared.presenter = FilePickerPresenterImpl.shared
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

/// Shows Tiny Care reminders even while the app is open, as Android does.
final class NotificationPresenter: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationPresenter()

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }
}

/// AES-256-GCM for backups (CryptoKit is Swift-only): the ciphertext followed by the 16-byte tag,
/// the same bytes Android's Cipher("AES/GCM/NoPadding") writes, so backups move between phones.
final class AesGcmBridgeImpl: NSObject, AesGcmBridge {
    func seal(key: Data, nonce: Data, plain: Data, aad: Data) -> Data? {
        guard let n = try? AES.GCM.Nonce(data: nonce),
              let box = try? AES.GCM.seal(plain, using: SymmetricKey(data: key), nonce: n, authenticating: aad) else { return nil }
        return box.ciphertext + box.tag
    }

    func open(key: Data, nonce: Data, sealed: Data, aad: Data) -> Data? {
        guard sealed.count >= 16, let n = try? AES.GCM.Nonce(data: nonce),
              let box = try? AES.GCM.SealedBox(nonce: n, ciphertext: sealed.prefix(sealed.count - 16), tag: sealed.suffix(16)) else { return nil }
        return try? AES.GCM.open(box, using: SymmetricKey(data: key), authenticating: aad)
    }
}

/// The document picker for Backup & Restore: saving a backup wherever the couple likes, and
/// choosing one to restore. Results go back to the shared app through IosFilePickers.
final class FilePickerPresenterImpl: NSObject, FilePickerPresenter, UIDocumentPickerDelegate {
    static let shared = FilePickerPresenterImpl()
    private var saving = false

    func presentSave(path: String) {
        saving = true
        let picker = UIDocumentPickerViewController(forExporting: [URL(fileURLWithPath: path)], asCopy: true)
        present(picker)
    }

    func presentOpen() {
        saving = false
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: [UTType.data, UTType.item], asCopy: true)
        present(picker)
    }

    private func present(_ picker: UIDocumentPickerViewController) {
        picker.delegate = self
        var top = UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow }.first?.rootViewController
        while let shown = top?.presentedViewController { top = shown }
        top?.present(picker, animated: true)
    }

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        if saving {
            IosFilePickers.shared.fileSaved(ok: true)
        } else {
            let data = urls.first.flatMap { try? Data(contentsOf: $0) }
            IosFilePickers.shared.filePicked(data: data)
        }
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        if saving {
            IosFilePickers.shared.fileSaved(ok: false)
        } else {
            IosFilePickers.shared.filePicked(data: nil)
        }
    }
}
