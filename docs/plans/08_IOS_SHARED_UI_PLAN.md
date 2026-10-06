# Plan 08: iOS runs the same app as Android (Compose Multiplatform)

Written 2026-10-05 against `origin/master` at `9788a10`.

**Status 2026-10-07: S0 to S5 done.** The iPad runs the shared app. PRs: #10 (S0, S1), #14 (S2, S3), #15 (iOS weather and sound), #16, #19, #22, #24, #25 (S4), #27 (iOS shell), #29 (iPad size), and the SwiftUI clean-up. Where a stage went differently from the plan, the notes in italics say how. Still to come on iOS: reminders, app lock, backup and the printed Polaroid card.

**Why:** the first iOS build installs and runs (branch `ios/first-build`, CI run #13), but it is a separate SwiftUI rewrite: its own simpler drawing, no pixel-art renderer, no Cozy Frame UI, no Seaside Pier. Every Android change would have to be redone by hand. The user chose to share Android's code instead, so iOS runs the same Kotlin world renderer and screens through Compose Multiplatform (CMP).

**State of the code:**
- `app` has 85 Kotlin files (45,140 lines) under `com.example`. 31 files (12,851 lines) use only Compose and the Kotlin standard library; about 10 more need only small swaps (`Math.PI`, `System.arraycopy`, `Calendar`, `String.format`, `currentTimeMillis`).
- **The world drawing is already portable:** every drawing module (`World*Drawing`, `Sprites`, `LoftSprites`, `PierSprites`, `PixelCanvas`, `PixelFont`, `WorldParticles`, ...) draws through Compose `DrawScope`. No file uses `nativeCanvas` or text measuring.
- **The one Android-only piece of the renderer** is `ui/LowResWorldBuffer.kt`: an `android.graphics.Bitmap` buffer drawn through `HardEdgeCanvas` (a `Canvas` subclass that turns anti-aliasing off, snaps rects to whole pixels and makes thin strokes hairlines), plus `getPixels`/`setPixels` for the stage extension and the dissolve effect.
- `SceneEngine` (7,775 lines) uses Compose runtime state, `AmbientAudio` directly (about 45 members), `GameText.get(R.string...)` 172 times, a little `java.time`, and androidMain `RelationshipTimeManager`.
- Text: one `res/values/strings.xml` with 627 strings and 9 plurals, read through `stringResource` (about 360 call sites) and `GameText` (engine). One font (`res/font/tiny_pixel.ttf`), five weather mp3s.
- Saved data: `PreferencesManager` (shared/androidMain, 1,047 lines) works directly on `SharedPreferences` with org.json; the common `KeyValueStorage` exists (Android, iOS and in-memory implementations) but `PreferencesManager` does not use it yet.
- Versions: Kotlin 2.2.10, AGP 9.1.1, Compose BOM 2024.09.00 (Compose 1.7), no JetBrains Compose plugin yet.

**Staying platform-specific (not moved):**
- Android: `MainActivity`, `TinyUsApp`, app lock (biometric, `FLAG_SECURE`), discreet mode (activity aliases), home-screen widget (`RemoteViews` + `WidgetSceneRenderer`), Tiny Care notifications (AlarmManager + receivers), backup (SAF, AES-GCM), Polaroid export to the gallery.
- iOS: the SwiftUI app shell, the WidgetKit widget, notifications, photo saving. iOS versions of app lock and backup are later work, not part of this plan.

**Ground rules:**
- Every stage is its own branch from `master` and its own PR, which the user opens. No Claude attribution in commits or PRs.
- Moves keep package names (`com.example.engine`, `com.example.ui`, ...), so Android imports do not change. Each move is a pure `git mv` commit first, edits after, so history follows the files.
- Before moving files, IOS announces the exact file list to GROWTH and FEATURES and waits for them to commit or pause edits to those files. After the merge they rebase.
- One Gradle build at a time; "build free" between sessions. No emulator: Android checks are the JVM test suite and JVM renders (`ScenePreviewTest`).
- No emoji (`NoEmojiPolicyTest` scans docs and iOS too). The privacy task scans build files for forbidden words, comments included: avoid "adjust" and "segment" in any build file.
- iOS is checked on GitHub's macOS runner (`ios.yml`), then on the user's iPad.

---

## S0. Foundation: Compose Multiplatform builds on both platforms (no files move)

- [x] Add the JetBrains Compose plugin to `shared` (CMP 1.9.x: works with Kotlin 2.2.10 and maps to Jetpack Compose 1.9) plus `compose.runtime/foundation/material3/ui` and Compose Resources.
- [x] Align the Android app: Compose BOM to the release matching Compose 1.9, then the full Android suite. This is the riskiest step for Android (1.7 to 1.9); it gets its own commit so it can be reverted alone.
- [x] iOS: `MainViewController()` in `shared/src/iosMain` returning `ComposeUIViewController { ... }`; the SwiftUI app hosts it with `UIViewControllerRepresentable`. First content: a test screen.
- [x] CI: `ios.yml` green; measure the IPA size change (Skia adds roughly 10 to 20 MB).
- **Done when:** both CIs are green, Android tests unchanged, the iPad shows a Compose screen inside Tiny Us.

## S1. Move the pure engine and sprite code (no behavior change)

Files (all pure today): `engine/` PixelCanvas, Sprites, LoftSprites, PierSprites, PixelFont, WorldCamera, WorldViewport, SpriteClock, StageExtension (`System.arraycopy` to `copyInto`), RoomTheme, SeasonalWeather, AvatarLook; `scene/` SceneLayouts, PierModels, `autonomy/`; `FeatureFlags`.
- [x] `git mv` to `shared/src/commonMain/kotlin/...`, same packages; fix the few JVM calls.
- [x] Android tests and `ScenePreviewTest` renders identical before and after.

## S2. Platform seams

- [x] **Audio:** a common `TinyAudio` interface with the members `SceneEngine` and the UI use; `AmbientAudio` implements it on Android unchanged. iOS implementation on AVAudioEngine, reusing the existing procedural synthesis (common `ProceduralAudioSynthesizer` / `PcmAudioSink`) and the bundled weather mp3s.
- [x] **Text:** `strings.xml` moves to `shared/src/commonMain/composeResources/values/strings.xml` (same XML format, plurals supported). UI code switches from `R.string.x` to `Res.string.x` (mechanical rename, scripted with `tools/i18n`). `GameText` becomes common: preloaded once at start-up so the engine keeps synchronous lookups. Pseudo-locale and lint checks are re-pointed at the new file.
- [x] **Font:** `tiny_pixel.ttf` to Compose Resources; `TinyTokens` uses `Res.font`.
- [x] **Dates:** `java.time` and `Calendar` to `kotlinx-datetime` (already a shared dependency); `RelationshipTimeManager` and `SpecialCalendarManager` move to common.
- [x] **Hard-edge buffer:** replace `HardEdgeCanvas` with a common implementation of the Compose `Canvas` interface that wraps the real canvas, turns anti-aliasing off on every `Paint`, and snaps rects using the scale/translate it tracks itself. Pixel read-back uses common `ImageBitmap.readPixels`; writing pixels back (stage extension, dissolve) is a small expect/actual (Android `Bitmap.setPixels`, iOS Skia `Bitmap.installPixels`). Gate: `ScenePreviewTest` renders pixel-identical to today's on the JVM.

## S3. The world on iOS (the big visible milestone)

- [x] Move `SceneEngine`, `PixelWorldView`, all `World*` drawing files, `WorldParticles`, `WorldSpeechBubbles`, `WorldDreamOverlay`, `ParticleSystem`, `BirdSystem`, `MoonPhase`, `SpecialDays`, `WeatherMemory` (storage via `KeyValueStorage`) to common.
- [x] iOS shows the real pixel world (all 13 scenes, autonomy, weather, touches) in place of `TinyWorldPainter`; the SwiftUI overlays stay for now.
- **Done when:** the iPad shows the same scenes as Android; Android renders and tests unchanged.

## S4. Screens, theme and saved data

- [x] Theme and chrome (`ui/theme/*`, `TinyTokens`, `PixelIcons`, `PixelShapes`, `TinyChrome`), then `MainScreen` and the dialogs, one group per PR.
  - *Theme and chrome done (branch `ios/s4-theme`): `Theme`, `TinyTokens`, `Type` and `TinyChrome` are common; the pixel font lives in `shared/src/androidMain/res/font` and the iOS app bundles the same file (`PixelFamily` is expect/actual). `MainScreen` and the dialogs wait until the cozy-games work that edits them is committed.* Platform actions (photo picking, saving a Polaroid, notification permission, backup, app lock) become callbacks the platform app provides.
- [x] `PreferencesManager` moves onto `KeyValueStorage`, keeping the same keys and JSON so existing Android installs keep their data. *(Done on `ios/s4-theme`: instead of kotlinx.serialization, a small common `SavedJson` that writes exactly what org.json wrote; `SavedDataCompatTest` checks old data loads and new writes are byte-identical.)*
- [x] `PolaroidManager` splits: common model and list; platform image capture and gallery export.

## S5. Retire the SwiftUI duplicate

- [x] iOS becomes a thin SwiftUI shell around `MainViewController()`, plus the WidgetKit widget (fed from the same App Group payload) and iOS notifications. *(Shell: `SharedMainViewController` with `IosMainPlatform` and `IosPolaroidPhotos`, PR #27; the iPad draws the couple at phone size via `WorldViewport.maxZoom`, PR #29; the widget is fed through `IosWidget`, which the Swift app writes as the App Group payload. iOS notifications, app lock and backup are still to come: the settings sheet leaves them out on iOS.)*
- [x] Delete `TinyWorldPainter`, `TinyWorldStage`, `TinyOverlays`, `TinySoundFX` and most of `ContentView`. *(Done; `ContentView` shows the shared app, or the last error with Copy and Try again.)*

---

## Risks

- **Android Compose 1.7 to 1.9:** small layout or behavior differences; mitigated by the S0 split commit and the full test suite.
- **Merge pain:** S1, S3 and S4 move files that GROWTH and FEATURES edit every day. Mitigated by announcing file lists, pure-move commits, and short-lived branches.
- **iOS app size and build time:** Skia and Kotlin/Native make the IPA bigger and CI slower (about 10 minutes today).
- **Rendering differences between Android's canvas and Skia on iOS:** pixel snapping is done by our own code (S2), so both platforms produce the same pixels.
