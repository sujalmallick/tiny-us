# Android Implementation Plan — Public Release Polish

Scope: **Android only**. iOS follows later (see "iOS follow-up" at the end).
Every phase keeps the app 100% offline and adds no networking/analytics dependency
(the `verifyPrivacySafeguards` task and `DependencySafeguardTest` must stay green).

Ground rules
- **Never break existing saves.** SharedPreferences keys and JSON field names (`bf_name`,
  `girl_outfit_index`, `boyAnswer`, …) stay as they are. New data uses new keys; new JSON
  fields are optional on read.
- Pure logic (timeline building, garden growth, PIN hashing rules) goes in `:shared/commonMain`
  with unit tests in `shared/src/commonTest`, so iOS can reuse it.
- `./gradlew testDebugUnitTest` stays green after each phase.

---

## Phase 0 — Release hygiene (the "fix" table)

| # | Item | Change |
|---|---|---|
| 0.1 | Split giant files | `OverlayDialogs.kt` (5.4k lines) → one file per dialog. `PixelWorldView.kt` (6.5k lines) → `PixelWorldView.kt` (composable) + `world/` drawing files (sky, meadow, flowers, kitchen, living room, path, scenes, dreams, particles, bubbles). Pure move: private helpers become `internal`, no behaviour change. |
| 0.2 | Backup honesty | Replace the sample `backup_rules.xml` / `data_extraction_rules.xml` with real rules (keep app data so a new phone restores it; exclude transient/lock-session state). Reword "never leaves your device" in README/privacy docs to say *we* never receive data and Android's own encrypted device backup may include it. |
| 0.3 | Export / import | Settings → "Backup & Restore": password-protected `.tinyus` file (PBKDF2 + AES-GCM, `javax.crypto`, no new deps) containing both prefs files + polaroid images. Saved/opened through the system file picker (SAF), so no storage permission is needed. |
| 0.4 | Hide not-yet-working partner features | Long-Distance Signals and the "share with partner" mood toggle are hidden behind `FeatureFlags.PARTNER_SYNC` (off) until Love Packets exist. Data layer and tests stay. |
| 0.5 | R8 + release signing | `isMinifyEnabled = true`, `isShrinkResources = true`, keep rules as needed. Release signing reads an uncommitted `keystore.properties` (falls back to debug signing only when absent). |
| 0.6 | Strings | All **new** UI in this plan uses `stringResource`. Full extraction of legacy strings is tracked as a follow-up (too large to mix with feature work). |
| 0.7 | README | Fix min SDK (24 → 26) and feature list. |
| 0.8 | Personal-data guard | `verifyNoPersonalData` Gradle task: if a local (ignored) `personal_profile.json` exists, scan all tracked files for its names/letters and fail the build if any leak. No real names are ever committed. |

## Phase 1 — Partner-neutral experience

Goal: no user-facing assumption that a couple is "a boy and a girl".
- Onboarding/Settings labels: "Boy's Name / Girl's Name" → "Your name / Your person's name";
  defaults "Him"/"Her" → "You"/"Your person" (matches iOS).
- Copy cleanup: "smells like him", "his/her accessory", "A Little Guide for My Girl",
  "The boy with big dreams…", widget "Him & Her", wardrobe tabs labelled by name.
- Internally the two character slots keep their `boy`/`girl` identifiers (persisted keys
  depend on them). Their *appearance* is decoupled in Phase 2.

## Phase 2 — Avatar customizer ("Make Us")

- New `AvatarAppearance` (commonMain): `skinTone`, `hairColor`, `hairStyle` (short / long),
  `outfitStyle` (dress / trousers). Persisted per slot under new keys
  (`avatar_a_*`, `avatar_b_*`); absent keys = today's look, so nothing changes for existing users.
- `PixelCharacter` gets an `appearance`; `PixelArtRenderer` resolves skin/hair/outfit-style from
  it instead of from `isGirl`. Sofa, scooter and umbrella sprites take the same appearance.
- New "Make Us" screen (from Settings → Characters & Wardrobe and onboarding's last step):
  live pixel preview of both characters, swatches for skin (8), hair colour (8), hair style,
  outfit style. Existing wardrobe (outfits/accessories) stays and filters by outfit style.
- Monetization hook (not built now): cosmetic sets are just more palette entries.

## Phase 3 — App lock & discreet mode

- Settings → "Privacy Lock": 4–6 digit app PIN (PBKDF2 hash + salt, never stored plain) plus
  optional fingerprint/face via `androidx.biometric` (`MainActivity` → `FragmentActivity`).
- Lock on cold start and after the app has been in the background longer than the chosen grace
  period (immediately / 1 min / 5 min). Pixel-styled lock screen.
- "Forgot PIN" → confirm with the phone's own screen lock → set a new PIN. No recovery server.
- When the lock is on, the app is hidden from the recents thumbnail (`FLAG_SECURE`).
- **Discreet mode**: switch the launcher icon/label to a neutral "Journal" leaf icon
  (`activity-alias`), and make Tiny Care notifications generic ("A gentle reminder").

## Phase 4 — "Our Story" timeline

- Add an optional `createdAt` epoch to `MemoryItem` / `LoveNoteItem` (written for new items,
  optional on read) and a `capturedAt` to polaroids.
- `StoryTimelineBuilder` (commonMain): merges memories, letters, polaroids, dreams, completed
  date adventures, daily-moment answers, garden blooms and computed milestones
  (first day, 100 days, 1/2/… years, anniversaries) into one date-sorted list grouped by
  year → month. Lenient date parsing for old free-text dates; unparseable items go to a
  "Somewhere in our story" section instead of being dropped.
- "Our Story" scrapbook dialog: month headers, pixel ribbons for milestones, polaroid
  thumbnails, filter chips (All / Memories / Letters / Photos / Milestones).
  Entry: Settings → Memories & Keepsakes, and long-press on the in-world photo frame.

## Phase 5 — A garden that never punishes

The garden already never decays (it counts *total* days visited, not streaks), but it stops
changing at day 14. Changes:
- `GardenGrowth` (commonMain, tested): stages 0–6 unchanged; after that, every 5 visit-days
  a new **keepsake plant** blooms (sunflower, lavender, tulip, moonflower, …) and joins the
  meadow. Growth is monotonic — a test guarantees nothing is ever removed.
- Coming back after a break shows a gentle line ("Mochi kept the garden watered while you
  were away") — never a loss message.
- Each new bloom appears in Our Story as a garden milestone.

---

## Status (Android, 2026-10-04)

| Phase | State | Notes |
|---|---|---|
| 0.1 Split giant files | Done | `OverlayDialogs.kt` → 12 dialog files; `PixelWorldView.kt` → composable + 9 `World*.kt` drawing files |
| 0.2 Backup honesty | Done | Real `backup_rules.xml` / `data_extraction_rules.xml`; README, privacy policy, Data-safety notes and in-app footer reworded |
| 0.3 Export / import | Done | `data/backup/ChunkedCipher.kt` + `TinyBackup.kt`, Settings → Backup & Restore |
| 0.4 Hide partner-sync features | Done | `FeatureFlags.PARTNER_SYNC = false` |
| 0.5 R8 + release signing | Done | Release APK builds minified; signing via uncommitted `keystore.properties` |
| 0.6 Strings | Partial | All new UI uses `strings.xml`; legacy screens still hard-coded |
| 0.7 README | Done | |
| 0.8 Personal-data guard | Done | `./gradlew verifyNoPersonalData` (runs on every app build when the file exists) |
| 1 Partner-neutral copy | Done | Neutral labels and defaults (`Bean` / `Sprout`); internal `boy`/`girl` slot names kept for save compatibility |
| 2 Make Us | Done | Skin (8), hair colour (8), short/long, trousers/dresses; standing, sitting, sleeping, hug, kiss, sofa, scooter and umbrella sprites |
| 3 App lock & discreet mode | Done | PIN + biometrics + grace period + FLAG_SECURE + "Journal" launcher alias + generic notifications |
| 4 Our Story | Done | `StoryTimeline` (commonMain) + `OurStoryDialog` |
| 5 Never-punishing garden | Done | `GardenGrowth` (commonMain): 12 keepsake plants then golden blooms; welcome-back messages |

Follow-ups noticed along the way:
- Users who skipped onboarding on older builds have "Him"/"Her" saved as names; they are treated as placeholders in onboarding but still shown until edited.
- ~25 MB of the 28 MB release APK is bundled MP3 music (code is 1.6 MB) — consider lower bitrates or Play Asset Delivery.
- The bookshelf photo mini-figures in the loft still use the default skin/hair colours.
- When a partner switches between dresses and trousers, the saved outfit index is re-used across the two outfit lists.

## Order of work

1. Phase 0.1 (split files) first — every later phase edits these files.
2. Phase 1 → Phase 2 (they touch the same rendering/labels).
3. Phase 3, Phase 4, Phase 5, then the rest of Phase 0.

## iOS follow-up (after Android)

- iOS already uses neutral names (`nameOne`/`nameTwo`). Port: app lock (`LocalAuthentication`
  + `NSFaceIDUsageDescription`), avatar appearance, Our Story (reuse `StoryTimelineBuilder`),
  garden (reuse `GardenGrowth`), export/import format.
- Known iOS bug found during mapping: `ContentView.swift` writes `bf_name`/`gf_name` to
  `UserDefaults.standard`, but `iosMain/ProfileManager` reads the App Group suite.
