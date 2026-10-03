# Plan 01: Bug Fixes & Polish

> **Line numbers** are from the uncommitted working tree as of 2026-10-03.
> **iOS files are being edited right now.** `TinyWorldStage.swift`, `TinySoundFX.swift`, `WidgetExtension.swift` and `TinyWidgetPayload.swift` changed during this review. Re-check every iOS item against the latest files before you start it.
>
> **Order:** do Phase 0 first. Phases 1–2 create helpers (pose restore, the layout objects, `react()`) that [Plan 02](02_NEW_SCENE_SEASIDE_PIER_PLAN.md) depends on.

## Progress (2026-10-04)

> **Note:** the file split (`OverlayDialogs.kt` → per-dialog files; `PixelWorldView.kt` → `World*.kt`) happened after this plan was written. The cafe and campfire drawing now lives in `WorldSpecialScenes.kt`, so the line numbers below are approximate.

**Done**
- 0.2: `ci.yml` now runs `:shared:testAndroidHostTest` (`withHostTest {}` was already in place).
- 0.3 / 0.4: `SharedCoreTest` checks the full list of scene names and tests hours 23 and 0.
- 1.1: `settleToHome` in `SceneEngine`. Cafe, sunroom and campfire characters return to their home spot and pose after reactions, watch scenes and floor strolls.
- 1.2: the barista, campfire and guitar emotes now set `emoteTimer`, so they show.
- 1.3: new `scene/SceneLayouts.kt` (`CafeLayout`, `CampfireLayout`).
  - Drawing, hit-tests and particle spawns all use it.
  - When two targets overlap, the nearest one wins, and Mochi is always a candidate.
  - The passerby can only be tapped while visible; window hearts are clamped to the glass.
  - The pup no longer purrs; the campfire Mochi handler now goes through `onTouchCat`.
- 1.4a: the croissant empties on the last bite and refills on the next tap.
- 1.4b: the campfire uses outdoor rules for Mochi (`isCurrentSceneOutdoor`).
- 1.4c: the fire pit is a keep-out zone for walk targets and cat targets.
- 1.4d: tapping the latte puffs up the foam heart; tapping the fire makes it flare.
- 1.4e: the lantern message depends on the time of day. The `ambientDimming` change was not done.
- 1.4f: the input lock for unscripted scenes went from 4.5s to 1.5s.
- 1.4h: Polaroid titles added for the cafe and sunroom.
- 3.3: the particle cap now applies indoors too.

**New tests:** `SeatedScenesTest` (7) and `SceneLayoutHitTest` (5).

**Skipped or deferred**
- 1.4g (menu overlapping the window): every position collides with the shelves or lamp on narrow screens. It needs an art pass.
- 1.4i (Android widget): the widget area belongs to the GROWTH session.
- 1.4j (sunny particle cadence): needs the owner's decision.
- Phase 2 (iOS): iOS parity is paused until the Android work settles.
- 3.1, 3.2, 3.4, 3.5: performance work.
- Phase 4: docs. The README is being updated in GROWTH's Phase 0.7.

---

## Phase 0: Make the tree buildable and CI-honest (do first)

| # | Problem | Evidence | Fix |
|---|---|---|---|
| 0.1 | Files the build needs are not tracked by git | `git status` lists these as untracked: `iosApp/TinyUsWidget/` (pbxproj `INFOPLIST_FILE` points at it), `AppIcon.appiconset/TinyUsAppIcon.png`, `TinyWorldStage.swift`, `TinySoundFX.swift`, `shared/src/commonTest/` | Add and commit them together with the edits that reference them. If you don't, a clean CI checkout will not build. |
| 0.2 | **The shared tests never run in CI** | `ci.yml` only runs `./gradlew testDebugUnitTest`. `:shared` uses `com.android.kotlin.multiplatform.library` without `withHostTest {}`, so it has no unit-test task. `ios.yml` does run them, but only when started manually (`workflow_dispatch`). | Add `androidLibrary { withHostTest {} }` to `shared/build.gradle.kts`. Add `./gradlew :shared:testAndroidHostTest` to `ci.yml`. Check the exact task name with `./gradlew :shared:tasks --all`. |
| 0.3 | The scene-count assertion breaks every time a scene is added | `SharedCoreTest.kt:51` (`assertEquals(12, …)`) | Replace it with `assertEquals(listOf("FLOWER", …, "CAMPFIRE"), SceneType.entries.map { it.name })`. That also catches renames, which matters because persistence stores `.name` (`PreferencesManager.kt:47-49, 215-226`). |
| 0.4 | One assertion only tests the `else` branch | `SharedCoreTest.kt` asserts `TimeOfDayPhase.fromHour(24)` | Assert hours `23` and `0` instead. |

**Done when:** `./gradlew testDebugUnitTest :shared:testAndroidHostTest :verifyPrivacySafeguards` passes locally and in CI.

---

## Phase 1: Android behavior bugs in the new scenes (high impact)

### 1.1 Couple stays stuck in reaction poses in RAINY_CAFE, SUNROOM and CAMPFIRE ⚠️ biggest bug

**What happens**
- `SceneEngine.kt:1975` sends all three scenes to `-> Unit`, so nothing restores their seated pose.
- After a campfire tap they stay in `EAT_SNEAK` (standing). The cuddle drops to 0 and they slide apart (`SceneEngine.kt:1605-1616`).
- The scene picker always runs `triggerWatchScene()` (`MainScreen.kt:985-987`). These three scenes fall back to the generic kiss (`SceneEngine.kt:1523-1559`), which leaves both characters in `KISS`. That pose then spawns hearts forever (`SceneEngine.kt:1728`).
- Jumps after tapping a character, and walks started by tapping the ground, also never settle.
- The new test `TinyUsOfflineTest.kt:103` asserts `EAT_SNEAK`, which **locks this bug in**.

**Fix: one generic "home pose" system that every scene can use**
1. In `SceneEngine`, add `private data class HomePose(val x: Float, val y: Float, val pose: CharacterPose, val dir: …)`, plus `boyHome` and `girlHome`.
2. At the end of `loadScene` (after the `when(type)` at L637-875), capture both characters' home poses.
3. Add `private fun settleToHome(dt)`. When `!isWatchSceneActive && reactionTimer <= 0 && !isMoving && !isScriptActive`, it should ease `worldX/Y` back to home (reuse the existing walk helper) and restore `pose` and `direction`.
4. Change the dispatch at L1975 to `RAINY_CAFE, SUNROOM, CAMPFIRE -> settleToHome(deltaSeconds)`.
5. Change `TinyUsOfflineTest.kt:103`. Keep the `EAT_SNEAK` assertion, then advance `update()` by more than 3.5s and assert both are back to `SIT_SNUGGLE` at their home x.

**Optional, later:** short watch-scene cinematics for these scenes (sharing a marshmallow at the campfire, clinking cups in the cafe) to replace the generic kiss.

### 1.2 Emotes set by the new handlers never appear

**What happens:** `onTouchCafeBarista` (L4971-4972), `onTouchCampfire` (L5032-5033) and `onTouchCampGuitar` (L5044, 5046) set `emote` but not `emoteTimer`. The renderer requires `emoteTimer > 0` (`PixelCanvas.kt:581`).

**Fix:** add one helper and use it in every `onTouch*` handler:
```kotlin
private fun react(c: Character, emote: EmoteType, emotion: CharacterEmotion,
                  pose: CharacterPose? = null, seconds: Float = 2.8f) { … sets emoteTimer + reactionTimer … }
```

**Test:** after each handler, assert `emoteTimer > 0`.

### 1.3 Tap targets don't match the drawn sprites → single source of truth for layout

The drawing code and the hit-test code use separate magic numbers, and they have drifted apart:

| Prop | Drawn at | Hit test | Effect |
|---|---|---|---|
| Latte | ≈0.41w (`PixelWorldView.kt:2962`) | 0.31w, r=18p (`:500`) | Tapping the cup hits the boy |
| Croissant | ≈0.65w (`:2994`) | `:505` | Misses on tall screens |
| Window | 0.36–0.94w (`:2800-2803`) | heart taps accepted at 0.10–0.90w (`:515`) | Hearts can be drawn on the bricks |
| Passerby | Animated, `passerX(sceneTime)` (`:2826-2848`) | Static band 0.12–0.88w × 0.20–0.38h (`:510`) | Fires when nobody is there and covers the window hearts |
| Pup | (0.86, 0.72), r=22p (`:495`) | Checked before Mochi, who spawns at (0.84, 0.70) | Tapping Mochi pets the dog. The dog also plays `playCatPurr()`. |
| Guitar | r=22p at (0.68, 0.68) (`:547`) | Overlaps Mochi at (0.76, 0.70) | Mochi's left half opens the guitar |
| Lantern | `0.06w + 58p` ≈ 0.33w (`:3154`) | `x < 0.28w` (`:552`) | The lantern itself can't be tapped |
| Camp Mochi | Fixed point (`:557`) | Ignores `catWorldX/Y`; forces `SLEEPING` (`SceneEngine.kt:5059-5065`) | Freezes Mochi in the middle of a walk |

**Fix**
1. Create `app/src/main/java/com/example/scene/SceneLayouts.kt` with `object CafeLayout` and `object CampfireLayout`. Each exposes `fun latte(cw, ch, p): Offset` (and similar) plus `val latteRadiusPx`.
2. Use them in three places: the draw functions, the hit tests at `PixelWorldView.kt:482-563`, and the particle spawns in `SceneEngine.kt`.
3. Move each scene's hit testing into a pure function, `fun hitTestCafe(cw, ch, p, x, y, catPos, time): CafeProp?`. Then it can be unit-tested without Compose.
4. Test Mochi first: if the tap is inside the cat's box, route it to `onTouchCat`. Delete `onTouchCampMochi`, or make it `onTouchCat` plus a blanket message, and only when Mochi is actually on the blanket.
5. Hit-test the passerby at its current `passerX` and only while it is visible. Clamp window hearts to the real window rectangle.
6. Give the pup its own SFX, either a new `playPuppyYip()` or reuse `playBubblePop`.

**Tests (new `SceneLayoutHitTest.kt`, pure JVM)**
- For each prop, a tap at its drawn centre returns that prop.
- No two prop circles contain each other's centres.
- Mochi's spawn point always resolves to the cat.
- The passerby can't be hit while it is off-screen.

### 1.4 Smaller Android fixes

| # | Fix | Where |
|---|---|---|
| a | The croissant never fully disappears. At 3 bites it still draws 6p wide. Hide it when `bites == max`. Refill it 8s later, or on the next tap, with a "Leo brings another" message. | `SceneEngine.kt:4948`, `PixelWorldView.kt:2997` |
| b | Campfire uses hard-coded *indoor* lists, so it shows "trots across the room" and uses indoor y-clamps. Replace those lists with `isCurrentSceneOutdoor`. | `SceneEngine.kt:3028-3034, 3105-3111, 3060` |
| c | Mochi and the characters can walk into the fire pit. Add an exclusion ellipse around the pit and apply it to cat roam targets, `commandCatWalkTo` and `onTouchWalkableGround`. | `SceneEngine.kt:3133-3138, 4060`; pit at `PixelWorldView.kt:3209` |
| d | Two timers are set but never drawn. Make `campfireEmbersTimer` drive a taller flame and an ember burst. Make `cafeLatteTimer` drive a heart-pop. | `SceneEngine.kt:142, 1887` |
| e | The lantern toggle only changes the message. Make it change `ambientDimming` slightly. Only say "for better stargazing" when `isNight`. | `SceneEngine.kt:5052-5057` |
| f | The 4.5s "script active" lock blocks input even though CAMPFIRE has no opening script. Remove CAMPFIRE from that lock. | `SceneEngine.kt:1756, 4103` |
| g | The chalkboard menu overlaps the window frame. Move it left or narrow the window, using `CafeLayout`. | `PixelWorldView.kt:2800, 2878` |
| h | `SCENE_TITLES` has no `RAINY_CAFE` or `SUNROOM`. Add about 12 titles each. | `PolaroidManager.kt:373-434` |
| i | The Android widget always shows "Living Room • Sunny" because it calls `getWidgetData()` with defaults. Pass the real last scene and weather. | `TinyUsWidgetProvider.kt:22` |
| j | The SUNNY particle spawn is now perfectly regular (one every 1.72s, split evenly). **Ask the owner whether this was intended.** If not, add weights and ±30% jitter. | `ParticleSystem.kt:1014-1026` |

---

## Phase 2: iOS bugs (re-check against `TinyWorldStage.swift` first)

| # | Severity | Problem | Fix |
|---|---|---|---|
| 2.1 | **High** | In indoor scenes `drawWeather` clips the *caller's* context, so the characters and Mochi are invisible (`ContentView.swift:912`, then `drawCharacters` at `:811`). | `var layer = c; layer.clip(to: …)` or `c.drawLayer { … }`. |
| 2.2 | Med | Leaves and petals rotate around the canvas origin and swing across the screen (`:933-940`). | Draw inside `drawLayer`: translate to the leaf, rotate, draw at the origin. |
| 2.3 | Med | `Text(adventure.description)` shows the Kotlin `toString()` dump, because Kotlin/Native exports `description` as `description_` (`:1050`). | Rename the Kotlin property to `details` in `DateAdventureModels.kt`. That avoids the Swift name clash for good. |
| 2.4 | Med | The profile is written to `UserDefaults.standard` (`:412-417`), but `IosUserDefaultsStorage` now reads the App Group suite. `ISO8601DateFormatter` defaults to GMT, so in IST the anniversary is saved one day early (`:415`). | Use one `TinyAppGroup` suite constant everywhere (`:444-446` still hard-codes it). Set `formatter.timeZone = .current`. Migrate standard-defaults data once. |
| 2.5 | Med | The volume slider doesn't affect music or chimes (`:112-125`). | Also set `musicPlayer?.volume` and `chimePlayer?.volume` in `update`. |
| 2.6 | Low | "Now playing" can stay stuck after a song ends, because pausing breaks the end-of-song timer (`:179`). | Use `AVAudioPlayerDelegate.audioPlayerDidFinishPlaying`. |
| 2.7 | Low | The widget payload's non-optional `anniversaryDate` makes decoding fall back to `.empty` until the app is reopened. | Add a custom `init(from:)` with `decodeIfPresent`, deriving the date from `updatedAt - daysTogether`. Pluralise "1 day". Compute `timePhase` inside the widget. |
| 2.8 | Low | The night sky uses hours `<6 / >19`, but the caption uses the shared `TimeOfDayPhase` (`:800`). | Use `TimeOfDayPhase.fromHour` everywhere. |
| 2.9 | Low | Partners on different platforms see different daily prompts. iOS uses an epoch day (`:529`); Android uses the relationship day (`PreferencesManager.kt:963`). | Use the relationship day on iOS too. |
| 2.10 | Low | The 7 pm reminder has a fixed prompt body (`:1180`). | Schedule the next 7 reminders individually, each with its own day's prompt. |

---

## Phase 3: Performance

| # | Fix | Where |
|---|---|---|
| 3.1 | Hoist per-frame `listOf(...)` and `Stroke(...)` allocations into `remember`/constants. | `PixelWorldView.kt:2785, 2967, 3026` |
| 3.2 | The brick wall costs about 280 `drawRect` calls per frame. Cache the static cafe and campfire background layers in an `ImageBitmap`, and redraw only when the size or time-of-day phase changes. | `PixelWorldView.kt:2764-2768` |
| 3.3 | Apply the 400-particle cap *before* the early return for indoor scenes. | `ParticleSystem.kt:958-977` |
| 3.4 | iOS photos: set `format.scale = 1`, use ImageIO thumbnails, and decode off the main thread with a cache. | `ContentView.swift:461-475, 1117` |
| 3.5 | iOS: debounce `persist()` (about 400 ms), and only reload the widget timelines when the widget's fields change. | `ContentView.swift:387, 1166-1169` |

---

## Phase 4: Docs and compliance

**README.md**
- `:11-17` lists 6 scenes; there are 12.
- `:35` says Min SDK 24; it is 26.
- `:18` says "no external audio assets"; there are 9 MP3s.
- `:33` says iOS uses shared synthesis; it doesn't.
- `:57` says the shared tests run in CI; they won't until 0.2 lands.

**Other docs**
- `docs/STORE_COMPLIANCE_CHECKLIST.md:32` says "8 scenes".
- Write the missing **Interactive Objects Catalog** (`docs/INTERACTIVE_OBJECTS.md`). The earlier session's notes are not in the repo. Generate it from the `SceneLayouts` objects so it can't drift from the code.

**⚖️ Licensing risk:** this public repo bundles commercial recordings (`wicked_game.mp3`, `golden_brown.mp3`, `cant_take_my_eyes_off_you.mp3`, `died_in_your_arms.mp3`). This is the owner's call: remove them, replace them with original or chiptune tracks, or make sure you have the rights.

---

## Verification checklist

- [ ] `./gradlew testDebugUnitTest :shared:testAndroidHostTest :verifyPrivacySafeguards` passes.
- [ ] Manual check on an Android emulator:
  - [ ] Cafe: tap every prop, confirm each hits its own sprite, and confirm Mochi responds to her own taps.
  - [ ] Campfire: tap the fire, confirm the couple sits back down after about 3.5s, and confirm nobody walks into the pit.
  - [ ] Scene picker into the cafe: no endless hearts.
- [ ] iOS (`ios.yml` dispatch): builds, and characters show in the kitchen, living room, cafe, sunroom and loft.
- [ ] CI green on `ci.yml`, including the shared tests.
