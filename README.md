# Tiny Us 🌸

A tiny, cozy pixel-art world for two. Tiny Us combines an Android Jetpack Compose app with a native SwiftUI iOS app, backed by Kotlin Multiplatform catalogs and relationship logic. The core world, memories, weather, music, and reminders work offline.

---

## ✨ Features

- **60 FPS Procedural Pixel Canvas**: Fully custom procedural pixel-art characters, environments, and animations drawn directly with Jetpack Compose Canvas.
- **Offline & Private**: Zero network requests, remote APIs, analytics or accounts — Tiny Us never sends your data anywhere. The only copies that can leave the phone are ones you make: a password-protected backup file you save yourself, or Android's own end-to-end encrypted device backup (photos excluded; see `app/src/main/res/xml/data_extraction_rules.xml`).
- **Interactive Scenes**:
  - 🌸 **A Flower For You**: Presenting a sweet flower in the meadow.
  - 🌳 **Under Our Tree**: Sitting peacefully under a swaying cherry blossom tree.
  - 🍳 **Kitchen Secret**: Playfully stealing bites from the simmering stew pot.
  - 🛋️ **Couch Snooze**: Falling asleep together on the couch as the room gently dims.
  - 🏮 **Lantern Stroll**: Walking hand in hand under the night streetlamp and stars.
  - 💖 **Just Looking at You**: Shared quiet glance with a gentle head pat.
- **Ambient Audio**: Real-time procedural lullaby and music-box synthesis using Android `AudioTrack`, plus a small bundled soundtrack.
- **Dynamic Particle System**: Floating hearts, falling leaves, drifting petals, bubbling steam, starry sparkles, and sleep 'Z's.
- **Our Keepsakes**: Local journal of special memories with retro icons.
- **Secret Letters Mailbox**: Sweet notes and letters stored locally on your device.
- **Today's Tiny Moment**: Automatically rotating daily couple moments and days-together counter.
- **Make Us (avatar customizer)**: Each partner picks skin tone, hair colour, short or long hair, and dresses or trousers — any couple can look like themselves.
- **Privacy Lock & discreet icon**: Optional app PIN with fingerprint/face unlock, hidden app preview in Recents, and a neutral "Journal" launcher icon with generic reminders.
- **Our Story**: An automatic scrapbook of milestones, keepsakes, letters, photos, dreams, adventures and garden blooms in date order.
- **A garden that never punishes**: Grows with total days visited (never streaks), earns keepsake flowers over time, and only ever says "welcome back".
- **Backup & Restore**: Everything in one password-protected (AES-256-GCM) file saved through the system file picker — no account, no upload.
- **Native iOS experience**: SwiftUI world, WidgetKit glance, local reminders, photo keepsakes, and the shared weather soundtrack.
- **Shared relationship features**: KMP daily prompts, date adventures, mini-game questions, time-of-day phases, and widget data.

---

## 🛠️ Tech Stack & Architecture

- **Shared language**: Kotlin 2.2 Multiplatform
- **UI frameworks**: Android Jetpack Compose (Material 3), native iOS SwiftUI and WidgetKit
- **Local persistence**: Android `SharedPreferences`; iOS `NSUserDefaults` and app-private image files
- **Audio**: shared procedural synthesis, Android `AudioTrack`, and iOS `AVAudioPlayer`
- **Target SDK**: Android 36 (Android 16 / Vanilla Ice Cream)
- **Min SDK**: Android 26 (Android 8.0 Oreo)

---

## 🚀 How to Build and Run

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (Ladybug / Meerkat or newer recommended)
- JDK 17 or higher

### Steps
1. Open Android Studio.
2. Select **Open** and choose the `tiny-us` project directory.
3. Sync Gradle and build:
   ```bash
   ./gradlew assembleDebug
   ```
4. Run the app on an Android emulator or a physical device.
5. The application works completely offline — test in Airplane Mode anytime!

### Release builds
Release builds are minified with R8. To sign for the Play Store, create an uncommitted `keystore.properties` at the repo root:

```properties
storeFile=release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Without it, release builds fall back to the debug key (fine for local testing only).

### Keeping personal data out of the repo
Your real names, dates and letters belong in the uncommitted `personal_profile.json` (see `personal_profile.template.json`). When that file is present locally, every build runs `verifyNoPersonalData`, which fails if any of its text appears in a git-tracked file.

### iOS

Build `iosApp/iosApp.xcodeproj` on macOS with Xcode. The `iosApp` scheme builds the app and its WidgetKit extension; `.github/workflows/ios.yml` also builds the shared iOS framework and runs the shared iOS simulator tests. Register the App Group identifier in `iosApp/iosApp/iosApp.entitlements` and `iosApp/iosApp/TinyUsWidget.entitlements` with the Apple team used for signing so the widget can read the app's local snapshot.
