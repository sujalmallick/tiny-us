# Tiny Us 🌸

A tiny, cozy pixel-art world for two. Cute, cozy interactions, daily tiny moments, and gentle ambient animations — built 100% offline with modern Jetpack Compose.

---

## ✨ Features

- **60 FPS Procedural Pixel Canvas**: Fully custom procedural pixel-art characters, environments, and animations drawn directly with Jetpack Compose Canvas.
- **100% Offline & Private**: Zero external network requests, zero remote APIs, zero analytics, and zero cloud dependencies. Your keepsakes and notes never leave your device.
- **Interactive Scenes**:
  - 🌸 **A Flower For You**: Presenting a sweet flower in the meadow.
  - 🌳 **Under Our Tree**: Sitting peacefully under a swaying cherry blossom tree.
  - 🍳 **Kitchen Secret**: Playfully stealing bites from the simmering stew pot.
  - 🛋️ **Couch Snooze**: Falling asleep together on the couch as the room gently dims.
  - 🏮 **Lantern Stroll**: Walking hand in hand under the night streetlamp and stars.
  - 💖 **Just Looking at You**: Shared quiet glance with a gentle head pat.
- **Synthesized Ambient Audio**: Real-time procedural lullaby and music-box sound generation using Android `AudioTrack` PCM synthesis — no external audio assets needed.
- **Dynamic Particle System**: Floating hearts, falling leaves, drifting petals, bubbling steam, starry sparkles, and sleep 'Z's.
- **Our Keepsakes**: Local journal of special memories with retro icons.
- **Secret Letters Mailbox**: Sweet notes and letters stored locally on your device.
- **Today's Tiny Moment**: Automatically rotating daily couple moments and days-together counter.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.2
- **UI Framework**: Android Jetpack Compose (Material 3)
- **Local Persistence**: Android `SharedPreferences` (offline JSON storage)
- **Audio Engine**: Real-time procedural PCM waveform synthesis
- **Target SDK**: Android 36 (Android 16 / Vanilla Ice Cream)
- **Min SDK**: Android 24 (Android 7.0 Nougat)

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
