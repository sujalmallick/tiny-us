# Plan 04: Next steps A–D (merge, device check, pixel overhaul, leftovers)

Written 2026-10-04, to start 2026-10-05.

**State at the time of writing:**
- Branch `android-public-release` holds `c89663d` (public-release polish), `6ffb0be` (pixel-art redraw of the cafe, sunroom and campfire) and `47b532e` (Grandpa Bao and the Seaside Pier).
- Uncommitted in the main tree: FEATURES' weather work and `docs/plans/03_PIXEL_ART_OVERHAUL_PLAN.md`.
- Other branches: `ui-chrome-pass` (worktree `.claude/worktrees/ui-chrome-pass`) and `ios/parity-first-pass`.
- Nothing has been pushed; `origin/master` is still `dd3a979`.

**Ground rules for the day:**
- **One session merges.** Others work in their own worktrees and don't touch the main tree during A.
- **One Gradle build at a time.** The machine struggles with parallel builds plus the emulator. Run targeted tests while iterating and the full suite only at the checkpoints below.
- **Emulator use is booked** between sessions: message "emulator free" when done.

---

## 0. Finish the UI work first (the UI session, in its worktree)

The user wants the UI restyle finished before anything else starts.

**Already done** on `ui-chrome-pass` (`adfecc7`, `923532a`; worktree clean):
- the "Cozy Frame" design system: tokens, `TinyChrome` components, light theme,
- every dialog, the settings sheet, onboarding, app lock, backup, wardrobe, calendar, music box, polaroid and Our Story moved onto it,
- 48dp touch targets, WCAG AA contrast,
- zero emoji, enforced by `NoEmojiPolicyTest`.

**To finish:**
- [x] The UI session lists what's left on its own plan and completes it in the worktree.
- [x] After-screenshots of every restyled screen. Book the emulator; the machine is short on memory, so run nothing else alongside it. *(Emulator and JVM rendering both ran out of memory on this machine; the user reviewed the build on a device instead. Before-shots: `.claude/worktrees/ui-chrome-pass/build/ui-pass/before/`.)*
- [x] The user reviews the before/after screenshots and approves the look. *(Approved 2026-10-04.)*
- [x] Decide whether the pixel UI (font, framed pixel buttons, pixel icons, Phase 5 of `03_PIXEL_ART_OVERHAUL_PLAN.md`) belongs in this pass or waits for C. *(Decision: waits for C.)*
- [x] The UI session runs its tests (including `NoEmojiPolicyTest`) and commits, then sends "ui-chrome-pass ready" with the final commit hash. *(Final hash `3952091`.)*
- **Check:** the user signs off on the UI. Only then start A1.

## A. Tidy up and merge (about 1 day)

### A1. Commit pending work on `android-public-release`
- [x] FEATURES commits its weather work: `ParticleSystem.kt`, `WorldParticles.kt`, `WorldWeatherExtras.kt`, the weather blocks in `SceneEngine.kt`, `SceneLayouts.kt` (`WeatherLayout`), `PixelWorldView.kt` hooks, 4 speed constants in `WorldSpecialScenes.kt`, and `CozyWeatherTest.kt`. *(`4dfe314`.)*
- [x] Commit `docs/plans/03_PIXEL_ART_OVERHAUL_PLAN.md` and this file. *(`06274e0`.)*
- **Check:** `git status` is clean.

### A2. Merge `ui-chrome-pass` (the UI session confirms it's finished first)
- [x] `git merge ui-chrome-pass` into `android-public-release`. *(Fast-forward to `3952091`: the UI branch had already merged `06274e0`.)*
- [x] Resolve the known conflict in `SceneEngine.kt`, keeping both sides' changes. *(Lighthouse tap: kept this branch's emoji-free gull-flock version.)*
- [x] Its new `NoEmojiPolicyTest` will flag emoji added on this branch after it forked. Replace them with TinyChrome pixel icons or plain text:
  - the "Make Us" and "Our Story" buttons (`strings.xml`),
  - garden bloom and welcome-back messages (`GardenGrowth.kt`, `MainScreen.kt`),
  - Our Story entry icons (`StoryTimeline.kt`),
  - pier, weather and scene-message emoji (`SceneEngine.kt`).
  *(None left after the merge: the repository-wide audit and `NoEmojiPolicyTest` both report zero.)*
- [x] Re-check the screens the restyle touched that were added after it forked: `AppLockScreens`, `AvatarCustomizerDialog`, `BackupRestoreSettings`, `OurStoryDialog`. *(All four were restyled in `923532a`; nothing newer landed in them.)*
- **Check:** `./gradlew :app:testDebugUnitTest :shared:testAndroidHostTest verifyPrivacySafeguards` is green. *(Green on `3952091`: 190 app tests, shared host tests, privacy check.)*

### A3. Merge `ios/parity-first-pass`
- [x] `git merge ios/parity-first-pass`. Most iOS files already arrived through `c89663d`, so expect duplicates. *(`3d1c451`: six conflicts, all resolved to this branch's newer version; the merge changes no files.)*
- [x] Resolve `iosApp/iosApp/ContentView.swift`. Keep the newer iOS session version; check whether the iOS session is still active and ask it first.
- [x] Resolve `shared/src/commonTest/kotlin/com/example/SharedCoreTest.kt` (both sides added it). Merge the tests and keep the explicit scene-name list (it now includes `SEASIDE_PIER`).
- [x] Confirm the shared `SceneType` change ("keep SceneType unchanged until Campfire lands") is satisfied: Campfire has landed.
- **Check:** shared host tests are green. The Swift code can't be built on Windows, so trigger the manual iOS GitHub workflow after A4.

### A4. Full checkpoint, push and pull request
- [x] One full run: `./gradlew :shared:testAndroidHostTest :app:testDebugUnitTest :app:assembleRelease verifyPrivacySafeguards`. *(Green on `3d1c451`: 190 app tests, 24 shared tests; release APK 27.4 MB, see D3.)*
- [x] `git push -u origin android-public-release`.
- [x] Open a pull request into `master` describing all the commits. Let CI run, and trigger the iOS workflow by hand.
- [x] Merge the pull request once CI is green (ask the user before merging into `master`). *(sujalmallick/tiny-us#1, merged 2026-10-04 as `68706cd`.)*

### A5. Clean up
- [x] `git worktree remove .claude/worktrees/ui-chrome-pass`. *(The empty folder stays until the UI session closes.)*
- [x] Delete merged local branches `ui-chrome-pass` and `ios/parity-first-pass`.
- [x] Drop the old safety stash `stash@{0}` ("snapshot before Android plan work"). *(Kept as the local tag `backup/pre-android-plan-stash`, not pushed.)*
- **Check:** `git branch` shows only `master` and `android-public-release` (or just `master` after the PR merges).

---

## B. Real-device check (about half a day)

**Not done yet.** The user has no Android phone, so this runs on the emulator, with everything else closed (Android Studio, other Claude sessions, the browser) and the emulator's RAM lowered to about 2 GB. Also measure the pixel renderer's frame time in each scene.

Install the merged release build, uninstalling the old one first if the signatures differ. Open scenes directly with `adb shell am start -n com.tinyus.app/com.example.MainActivity --es scene <SCENE>`.

| # | What to check | Pass when |
|---|---|---|
| B1 | Cafe, sunroom, campfire (day, sunset, night) | Pixel blocks look even; the cafe table sits in front of the seated couple; the tent stands on the grass; taps on the latte, croissant, pup, barista, menu, window, fire, guitar, lantern, watering can, plants and skylight all respond |
| B2 | Seaside Pier and Grandpa Bao | Interactions work; Pip the seagull and the bottle can be tapped |
| B3 | Weather | Rain, snow, petals and leaves are slow enough to see; the weather interactions respond |
| B4 | Privacy lock | PIN set, wrong PIN, cooldown, unlock; fingerprint (enrol a test fingerprint on the emulator, then `adb emu finger touch 1`); "Forgot PIN" with the phone's screen lock; hidden in Recents |
| B5 | Discreet icon | The launcher shows "Journal" and switches back; reminders show generic text |
| B6 | Backup & Restore | Save through the file picker, change some data, restore, data is back (photos included); a wrong password leaves data untouched |
| B7 | Make Us | Look changes apply in every scene (sofa, scooter, umbrella included) and survive a restart |
| B8 | Our Story | Milestones, letters, keepsakes and photos in date order; filters work |
| B9 | Garden | Speed time up with a debug day offset, or restore a prepared backup: blooms appear, the welcome-back message shows, blooms appear in Our Story |

- [ ] Fix anything found. Use targeted tests while iterating and one full run at the end.
- **Check:** every row passes, with screenshots kept for the store listing later.

---

## C. Pixel-art overhaul (about 2–3 weeks; details in `03_PIXEL_ART_OVERHAUL_PLAN.md`)

Work on a branch per phase (`pixel/phase-N-…`), merged after its checks pass.

**Progress (2026-10-04)**, on the local branch `pixel/phase-0-1-campfire` (not yet merged or pushed):
- `e71f0f6` C1 campfire test; the user approved the look and the couple's size (2 game pixels per sprite pixel).
- `e7e00ef` pixel renderer for all 13 scenes: `HardEdgeCanvas` (no anti-aliasing; rectangles snap to whole pixels, never thinner than one), `PixelFont` for the momo sign, whole-pixel NPC and loft sizes.
- `4f87cfb` barista and Grandpa Bao a bit smaller (1.5 game pixels), as the user asked.
- `gridP` patch removed (the renderer snaps every shape now).
- Still open: the device frame-time check (see B), then merging into `android-public-release`.

### C1. Test on the campfire scene (phases 0–1, 2–3 days)
- [x] Phase 0: add `WorldViewport` (one whole-number scale and the game-pixel size) and replace the 27 local `pixelScale` computations. Commit the Robolectric scene-preview renderer as a picture-comparison test.
- [x] Phase 1 on the campfire only:
  - draw the world into a small offscreen image and enlarge it by a whole number with `FilterQuality.None`,
  - characters drop the 1.38× scale *(done differently: characters use exactly 2 game pixels, so their details survive)*,
  - taps are divided by the scale before hit-testing *(not needed: the world still draws in screen coordinates and the canvas shrinks it, so taps and overlays work unchanged)*,
  - particles and overlays move to game pixels *(same reason)*.
- [x] Show the user a before/after of the campfire. **Decide: continue or stop.** *(Continue.)*

### C2. Roll out (if approved)
- [x] Phase 1 for every scene, then remove the `gridP` patch from `WorldSpecialScenes.kt`. *(Done; only the device frame-time check (see B) is left.)*
- [x] Phase 2: the 48-colour `TinyPalette` in `shared`; a palette-and-alpha pass on the finished frame (AGSL shader on Android 13+, CPU fallback); Bayer dithering; night and sunset as palette swaps with lamp light pools; `PixelPurityTest`.
  *(Tried and not adopted, 2026-10-04. A palette was built from renders of every scene, light and weather (weighted k-means in OKLab, with the 40 Make Us skin and hair colours kept exact), and frames were snapped to it. At 96 colours the momo awning's red turned brown, the night sky's navy went black and the boy's sweater went grey. At 128 the sky still darkened and lamp glows became flat discs. Bayer dithering covered everything in a screen-door pattern. The pixel renderer already makes every shape crisp and opaque (`HardEdgeCanvas`, and the staged-frame test checks no pixel is left transparent), which was the point of this phase. So the drawn colours stay. Comparison and scripts are in `build/pixel-test/phase2/`.)*
- [x] **Phase 3 (do this first after Phase 1; the user flagged it): the same close-up scene on every screen size.** *(`33e08b9`; comparisons in `build/pixel-test/phase3/`.)*
  - **What the user saw** (tablet-emulator screenshots, 2026-10-04): on a wide, nearly square tablet each scene reads as a close-up. Props sit around the couple and the detail shows. But the short height cuts parts off: the momo sign is missing, the lighthouse overlaps the moon, and the kitchen table is chopped at the bottom. On a tall phone the same scene spreads out. Props drift apart, there's a lot of empty sky and floor, and the couple look far away and the scene disorganised.
  - **Why:** positions are fractions of the screen (`cw * x`, `ch * y`), and the game-pixel size is set by width alone. A tall screen stretches the layout; a short one crops it.
  - **Goal:** every phone and tablet shows the whole scene with the tablet's close-up feel, nothing cut off, and the couple about the same size relative to the scene.
  - [x] Give each scene a fixed design stage in game pixels (for example about 160 × 200) and lay its props, characters, Mochi and tap targets out inside it, instead of using screen fractions (`SceneLayouts`, the engine's character and cat positions, the hit tests). *(Done with `WorldCamera`: the stage is about 144 game pixels wide and 4:3 to 3:2 tall. The layout code is unchanged; it now gets the stage's size instead of the screen's.)*
  - [x] Pick the largest whole-number zoom at which the whole stage fits the screen's width **and** height. On phones this makes everything bigger than now. *(7× on a 1080 × 2400 phone.)*
  - [x] Fill leftover screen space by extending the background (more sky or ceiling above, more ground or floor below, more scenery at the sides), never by spreading props apart. *(`StageExtension`; each scene sets where the spare height goes.)*
  - [x] Nicer extensions where they're plain: the kitchen's tall wall and the sunroom's floor are flat. Consider per-scene extra props there (shelves, more pots). *(`WorldStageDecor.kt`: sage upper cabinets and a jar shelf on the kitchen wall; potted plants and a woven basket on the sunroom floor. Floors with props on them now keep their pattern when it repeats, and the kitchen's floor runs to the bottom edge.)*
  - [x] Weather in the extension: rain, snow, petals and leaves fall only over the stage. Draw the falling layer over the whole screen. *(The falling layer is drawn again above and below the stage, shifted by one stage height.)*
  - [x] Keep the HUD (top buttons, heart button, message box) clear of the stage, or reserve room for it. *(The camera reserves the status bar plus 58 dp at the top and the navigation bar plus 70 dp at the bottom when the screen is tall enough; short screens share what's left.)*
  - [x] `ScenePreviewTest` renders every scene at phone 20:9, 19.5:9 and 16:9, at tablet 4:3, and nearly square. Check that nothing is cut off and the composition matches. *(`SCENE_PREVIEW_SIZE`; phone and square checked, and `WorldCameraTest` covers seven screen sizes.)*
  - [x] Layered backgrounds with slight parallax (the original Phase 3 idea), once the stage works. *(Not done, on purpose: every scene draws its layers in one pass, so parallax would mean splitting all 13 scenes into layers; the extended sky's drifting clouds give some depth instead. Revisit only if scenes get redrawn as layered sprites.)*
- [x] Phase 4: `SpriteClock` at 10 fps, whole-pixel positions, walk cycles, readable particles, pixel-dissolve scene change, haptics. *(`SpriteClock` steps sprite animation at 12 fps while movement updates every frame; positions snap to whole pixels; the 4-frame walk cycle and tap haptics were already there; particles became solid sprites in `1fc5e9f`; scene changes dissolve in 2x2 Bayer blocks (`PixelDissolve`).)*
- [x] Phase 5: bundled OFL pixel font, framed pixel buttons and dialogs, a 16×16 pixel icon set (built on the UI restyle's components), with accessibility fallbacks. *(Our own font instead of an OFL one: `res/font/tiny_pixel.ttf`, built by `tools/pixelfont/build_tiny_pixel_font.py` (all printable ASCII plus quotes, dashes, bullet, ellipsis), used for Display/Title/Section/Label; body text and captions stay in the default sans as the accessibility fallback. `PixelCornerShape` replaces every rounded corner and circle with whole-pixel stair steps. `PixelIcons`: 61 icons on a 12×12 grid (2 dp per pixel at 24 dp), generated by `tools/pixelfont/build_pixel_icons.py`, replacing all 147 Material icon uses. `ChromePreviewTest` renders the chrome; pictures in `build/pixel-test/phase5/`.)*
- **Checks for each phase:** picture tests reviewed, every pixel opaque and from the palette (from phase 2 on), tap tests green, frame time 8 ms or less.

---

## D. Leftovers from the first plan (about 2–3 days)

- [ ] **D1. Translatable text:** move the remaining hard-coded strings in older screens (Settings sheet, dialogs, scene messages) into `strings.xml`. Scene messages built in `SceneEngine` need string resources with arguments.
- [x] **D2. Old "Him"/"Her" names:** when the saved names are the old placeholders, show a one-time gentle prompt ("Want to set your names?") instead of renaming silently. Add a test. *(`NamePromptDialog`: asked once (`namePromptAnswered`) when a saved name is still "Him"/"Her"; "Set our names" reopens the setup with empty name fields, "Keep them" keeps them. Rule in `PersonalProfile.shouldOfferNamePrompt`, tested in `NamePromptTest`.)*
- [x] **D3. App size:** re-encode the bundled music at a lower bitrate (for example 96–128 kbps), or stream nothing and keep fewer tracks. Target a release APK under about 12 MB. Make sure the audio still sounds clean. *(Release APK 27.4 MB → 13.0 MB. The five background tracks are re-encoded from 192 to 96 kbps with `tools/audio/reencode_bgm.py` (originals kept locally in `tools/audio/originals/`, git-ignored); 80 kbps would reach about 11.2 MB at a quality cost. At the user's choice the four commercial recordings (Golden Brown, Can't Take My Eyes Off You, (I Just) Died in Your Arms, Wicked Game) and the transcribed "Until I Found You" melody were removed from Android and iOS, since they can't be published without a licence; the iOS song list is empty until original or royalty-free songs are added. Still to do: listen to the re-encoded tracks on a device.)*
- [x] **D4. Loft bookshelf figures:** pass each partner's Make Us look into `LoftSprites` (the mini figures currently use the default skin and hair). *(The framed couple picture on the loft bookshelf now uses both partners' Make Us hair and skin, with long-hair strands.)*
- [ ] **D5. Per-scene puddle spots** for rain (FEATURES' open item): puddles placed to fit each scene's ground instead of generic positions.
- [ ] **D6. Deferred plan-01 performance items** (FEATURES' open item): cache static background layers instead of redrawing them every frame, and remove allocations made each frame. This dovetails with C Phase 1 (offscreen rendering), so it may be cheapest to do there.
- **Check:** full suite green, release APK size recorded, the D2 prompt checked on the device.

---

## Order at a glance

0 (finish UI, user sign-off) → A1 → A2 → A3 → A4 → A5 → B (fix and retest) → C1 (user decision) → C2 phases → D (D1–D4 can also slot in between C phases).

**Done:** 0, A1–A5, C1, C2's Phase 1 rollout and Phase 3 (local branch). **Next (the user moved the emulator check to the end):** D3 (app size) → C2 Phases 2, 4 and 5 → the rest of D → B on the emulator with the frame-time check → merge the pixel branch.
