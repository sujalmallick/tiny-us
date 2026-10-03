# Plan 02: New Scene "Seaside Pier" + Characters Grandpa Bao & Pip the Seagull

> **Depends on [Plan 01](01_BUGFIX_AND_POLISH_PLAN.md):**
> - Phase 0: CI runs the shared tests, and the scene-count test is name-based.
> - 1.1: the `settleToHome` pose-restore system.
> - 1.2: the `react()` helper.
> - 1.3: the `SceneLayouts` and pure hit-test pattern.
>
> This plan builds on those helpers so it doesn't repeat the bugs found in the cafe and campfire.
>
> **Line numbers** are from the working tree as of 2026-10-03.

## Status (2026-10-04): Android MVP built, all tests green

**Built**
- The scene: enum entry, outdoor environment, loading, home-pose settling, walk bounds, starfield.
- `PierLayout` and its hit-tests.
- Grandpa Bao's fishing cycle with an anti-repeat catch picker. The first catch is always a fish, and Mochi walks over for it.
- Pip's state machine: AWAY, FLYING, PERCHED, SNEAKING, STEALING, ESCAPING. He waits out the rain, steals ice cream, and can be shooed.
- Ice cream (the cones are drawn in hand), the bottle showing the daily prompt, the lighthouse with a night beam, and sea splashes.
- 3 SFX, Polaroid titles and the picker icon.
- Files: `PierModels.kt`, `PierSprites.kt` and `WorldPierScene.kt`.
- Tests: `SeasidePierSceneTest` (10) and `PierLayoutHitTest` (3).

**Not built yet**
- A pier-specific watch-scene cinematic (it uses the generic kiss, then settles home).
- Pier autonomous moments.
- Caching static layers in an ImageBitmap.
- The lighthouse daytime gull flock (it shows sparkles instead).
- The iOS port (iOS parity is deferred).

## 1. Concept

**Scene:** `SEASIDE_PIER`
- **Title:** "Seaside Pier"
- **Subtitle:** "Salty breeze, sunset waves, and one very bold seagull"
- **Setting:** outdoors. A wooden pier stretching over the sea, with a lighthouse on the rocks, an ice-cream cart and a fisherman at the end of the pier. The couple sit on the edge with their feet dangling over the water.

This scene earns a place in the roster for two reasons:
- **Pip** is the app's **first NPC that moves on its own** (it has a state machine and an update tick). Every NPC so far is either static or animated purely by time.
- **The message-in-a-bottle** connects the world to the **Daily Tiny Moments** feature, using the shared `DailyPromptCatalog`.

### Characters

| NPC | Role | Behavior |
|---|---|---|
| **Grandpa Bao** 🎣 | Kind old fisherman at the far end of the pier | Sits on an upturned crate with a rod. Tap him: he casts, waits, then reels in a catch. The catch comes from an `AntiRepeatRandomPicker` (shared): fish, old boot, starfish, a love letter in a jar or seaweed. A fish goes to Mochi with hearts. Dialogue lines are gentle grandpa wisdom about long relationships. |
| **Pip the Seagull** 🐦 | Cheeky recurring troublemaker | Moves through the states FLYING → LANDING → PERCHED (on the railing) → SNEAKING (toward the ice cream) → STEALING → ESCAPING. Tap Pip to shoo it away: feathers fly and it squawks. If Pip reaches an ice cream that is out, it steals a bite and the couple laugh (`JOY_JUMP`). Mochi's eyes track Pip, and she crouches when Pip is close. |

### Props and interactions

| Prop | Tap behavior | Timer or state |
|---|---|---|
| Ice-cream cart (left) | Both get a cone, drawn in the hand while the timer is above 0. Plays a chime. Pip becomes interested. | `pierIceCreamTimer` (8s) |
| Grandpa Bao | Cast, then bite, then reel, then show the catch | `pierFishingPhase` enum + `pierFishingTimer` |
| Pip | Shoo | `pierGullState`, `pierGullX/Y`, `pierGullStateTimer` |
| Bottle bobbing in the water | Opens a scroll showing **today's daily prompt**, `DailyPromptCatalog.getPromptForDay(relationshipDay)`. Respawns after 30s. | `pierBottleVisible`, `pierBottleRespawnTimer` |
| Lighthouse | Night: the beam sweeps and a foghorn sounds. Day: a flock of three gulls takes off. | `pierLighthouseTimer` |
| Water (y 0.44–0.62) | Splash rings and blue droplets | none (particles) |
| Mochi by the bait bucket | Normal `onTouchCat`. **No special handler** (see the lesson from Plan 01, item 1.3). | existing cat state |

### Time of day and weather

- **Sunset is the hero palette:** orange sea and sky gradient, with a sun-path glitter streak on the water.
- **Night:**
  - The lighthouse beam rotates automatically.
  - A starfield, made tappable by adding the scene to `hasStarfield`.
  - The moon reflects on the water.
  - String lights glow on the railing.
- **Rain:**
  - Choppier waves (bigger amplitude).
  - Bao says "fish bite more in the rain".
  - Pip stays PERCHED and tucks its head in.
  - The couple get the umbrella automatically, because the scene is outdoors.
- **Snow:** snow piles on the railing tops.

## 2. Layout (fractions of `cw`/`ch`; `p = pixelScale`)

```
0.00 ┌───────────── sky (drawSkyAndClouds) ───────────── lighthouse ┐
     │                                                     (0.88,0.30)
0.42 ├──────────── horizon ─────────────────────────────────────────┤
     │   sea + waves      bottle (0.30,0.54)                         │
0.60 ├══ railing (planks + posts + string lights) ══════════════════┤
     │ cart(0.13,0.66)   couple sit (0.43/0.53, 0.74)   Bao(0.82,0.70)
     │                         Mochi + bucket (0.66,0.80)            │
0.88 └──────────────── pier boardwalk planks ───────────────────────┘
```

- Walkable area: x 0.08–0.92, y 0.68–0.86. Exclude Bao's crate (an ellipse around 0.82, 0.72) and the cart footprint.
- Pip's perch points: railing at y 0.58, x ∈ {0.24, 0.62, 0.74}. It sneaks along the railing toward the cones.
- Every coordinate above lives in `object PierLayout` in `SceneLayouts.kt`. The draw code, hit tests, particle spawns and walk bounds all read from it.

## 3. Implementation steps

### Step 1: Shared enum
`shared/src/commonMain/kotlin/com/example/scene/SceneType.kt`
- Add `SEASIDE_PIER(title, subtitle, environment = EnvironmentType.SEASIDE_PIER)` at the **end** of `SceneType`. Add `SEASIDE_PIER` to `EnvironmentType` (L70-83).
- Never rename these entries afterwards, because persistence stores `.name`.

### Step 2: SceneEngine state, loading and resets
`app/src/main/java/com/example/scene/SceneEngine.kt`

1. Add `EnvironmentType.SEASIDE_PIER` to `OUTDOOR_ENVIRONMENTS` (L32-40).
2. Add state fields near L118-153, all `mutableFloatStateOf`/`mutableStateOf` with `private set`:
   - `pierIceCreamTimer`, `pierFishingTimer`, `pierLighthouseTimer`, `pierBottleRespawnTimer`
   - `pierFishingPhase: PierFishingPhase` (`IDLE, CASTING, WAITING, REELING, SHOWING`)
   - `pierLastCatch: PierCatch?`
   - `pierBottleVisible`
   - `pierGullState: GullState`, `pierGullX`, `pierGullY`, `pierGullStateTimer`, `pierGullFacingLeft`
   - `internal var pierRng: Random = Random.Default`, which tests replace with `Random(42)`
3. In the `loadScene` reset block (L555-614), reset **every** field above.
4. Add a `loadScene` `when(type)` branch (L637-875):
   - boy at (0.43, 0.74), girl at (0.53, 0.74), pose `SIT_SNUGGLE`, facing the sea
   - Mochi at (0.66, 0.80), `CatState.IDLE`
   - Pip starts `FLYING` from offscreen right
5. Cover the exhaustive `when`s:
   - `isScriptActive` (L1746): `false`, since there's no opening script (see Plan 01, 1.4f)
   - `isSceneOpeningScriptActive` (L4093)
   - the Matchmaker `when` (L2743), and add the scene to the allowlist (L2733-2741)
   - `onTouchWalkableGround` bounds (L4040): `PierLayout.walkBounds`, with exclusions
6. Set `rainGroundY` (L1796): add the scene to the group whose ground line is ≈0.86 (the planks).
7. Timer countdowns (L1887-1897): decrement each new timer.
8. Update dispatch (L1965): `SceneType.SEASIDE_PIER -> updatePierScene(dt, cw, ch)`. It runs:
   - `settleToHome(dt)` (from Plan 01, 1.1)
   - `updateGull(dt)`: the state machine. Transitions are timer-driven, use `pierRng` and clamp to the perch points. It **only reads `pierIceCreamTimer`** to decide whether to start SNEAKING.
   - `updateFishing(dt)`: phase transitions. On SHOWING with `FISH`, Mochi walks to Bao via `commandCatWalkTo`.
   - Bottle respawn.
9. Fix the stale outdoor lists at L3028 and L3105 (Plan 01, 1.4b) so Mochi's roaming uses outdoor rules here.

### Step 3: Tap handlers
In `SceneEngine.kt`, next to the cafe and camp handlers (around L4940-5065). All of them use `react()` (Plan 01, 1.2) so emotes actually show.

| Handler | Effect |
|---|---|
| `onTouchPierIceCream(cw, ch)` | Sets `pierIceCreamTimer = 8f`; `audio.playHeartChime()`; sparkles at `PierLayout.cart`; both characters get HAPPY with the HEART emote. |
| `onTouchGrandpaBao(cw, ch)` | `IDLE` → `CASTING` (plays `playReelClick`); Bao says a line from `BAO_LINES` (about 10 lines). Tapping during WAITING says "Shh… something's nibbling". |
| `onTouchPip(cw, ch)` | If Pip is not FLYING or ESCAPING: state becomes `ESCAPING`; `audio.playSeagullCall()`; feather sparkles. The message changes if Pip was SNEAKING ("Caught red-beaked!"). |
| `onTouchPierBottle(cw, ch)` | If visible: hide it, start the respawn timer, `audio.playPaperFlip()`, and `showMessage("💌 " + DailyPromptCatalog.getPromptForDay(dayIndex).text, 5f)`. Get `dayIndex` from `RelationshipTimeCalculator` the same way Android Daily Moments does (`PreferencesManager.kt:963`). |
| `onTouchLighthouse(cw, ch)` | `pierLighthouseTimer = 4f`. Night: `audio.playFoghorn()` plus a beam burst. Day: spawn 3 flock birds via `birdSystem`, or sparkles if birdSystem isn't wired in. |
| `onTouchSea(cw, ch, x, y)` | Splash particles at the tap point (`spawnSparkles` in blue/white). |

**Theft event** (inside `updateGull`): when Pip reaches the cones while `pierIceCreamTimer > 0`:
1. `pierIceCreamTimer = 0`
2. `react(boy, SURPRISED, JOY_JUMP)` and `react(girl, …)`
3. message: "Pip stole the ice cream!! 🐦🍦"
4. state becomes `ESCAPING`

### Step 4: Rendering
`app/src/main/java/com/example/ui/PixelWorldView.kt`

1. Add `drawSeasidePierScene(scope, cw, ch, p, time, engine, isNight, isSunset, isMorning)` and a `drawEnvironment` branch (around L3281-4091). Draw these layers back to front:
   1. `drawSkyAndClouds` (existing, L4277)
   2. sea gradient by phase, plus 3 sine wave bands (amplitude ×1.8 when `weather == RAIN`), plus sun or moon glitter
   3. the lighthouse, with a rotating beam polygon at night or while `pierLighthouseTimer > 0`
   4. the bottle, bobbing by `sin(time)`
   5. the railing with string lights (glowing at night)
   6. the boardwalk planks
   7. the cart (striped awning)
   8. Bao: crate, rod and line, with the line taut and bending during REELING. Show the catch sprite above his head during SHOWING.
   9. the bait bucket
   10. Pip, if perched or on the railing
2. Put the NPC sprites in `WorldSprites` (`Sprites.kt:66`) as `drawGrandpaBao(scope, cx, groundY, p, time, phase)` and `drawSeagull(scope, x, y, p, time, state, facingLeft)`. Don't draw them inline like the cafe NPCs. That keeps them reusable, for example so Pip can appear in WALK later.
3. **Foreground hook:** Pip flies *in front of* the couple while FLYING or ESCAPING. Add a small post-character hook next to the COZY_LOFT foreground at L1521: `if (env == SEASIDE_PIER && engine.pierGullState.isAirborne) drawSeagull(...)`.
4. Draw the ice-cream cones while `pierIceCreamTimer > 0`. They are a hand prop, drawn after the characters at their hand position (offset from `worldX/Y`), so they appear in front.
5. **Performance:** cache the static layers (planks, railing posts, lighthouse body) in an `ImageBitmap` keyed on `(cw, ch, timeOfDayPhase)`. Make no `Stroke`/`listOf` allocations per frame (Plan 01, 3.1-3.2).
6. Add `EnvironmentType.SEASIDE_PIER` to `hasStarfield` (L1125-1132).
7. Hit tests: add `PierLayout.hitTest(cw, ch, p, x, y, engine): PierProp?` as a pure function and call it from the pre-character block (L482-563).
   - Priority: Pip, then Bao, then the cart, then the bottle, then the lighthouse.
   - **Mochi's box is excluded before props are checked.**
   - The sea is checked in the post-character `when` (L654-1104), so a tap on a character standing near the railing still wins.
8. Add the scene to the exhaustive post-character `when` (L1022 group).

### Step 5: Audio
`app/src/main/java/com/example/engine/AmbientAudio.kt`

Add these in the existing `playSfxNoteStatic` note-sequence style (see `playSteamHiss` L1928):
- `playSeagullCall()`: two fast descending high notes, ×2
- `playFoghorn()`: one long low note around 110 Hz plus a fifth, soft
- `playReelClick()`: 6 rapid `playWoodKnock`-like ticks

Optional: a soft wave-wash ambience loop while outdoors at the pier. Leave it out of the MVP if it needs new infrastructure.

### Step 6: Watch scene and autonomous moments
In `SceneEngine.kt`:
- **Watch-scene cinematic** (add a case before the `else` at L1523), about 6s:
  1. The couple share one cone.
  2. Pip swoops in and steals it.
  3. Both laugh (`JOY_JUMP`).
  4. The scene settles back via `settleToHome`.
- **Autonomous moments** (L3359-3863), 3 entries:
  - "dangling feet" (a small leg swing)
  - "girl points at a boat on the horizon" (draw a tiny sailboat while the moment's timer runs)
  - "Bao waves at the couple"

### Step 7: Other places that list scenes

| Where | What to add |
|---|---|
| `OverlayDialogs.kt:3470-3483` `getSceneIcon` | `SceneType.SEASIDE_PIER -> Icons.Default.Sailing`. icons-extended is already a dependency (`app/build.gradle.kts:63`). |
| `PolaroidManager.kt:373-434` `SCENE_TITLES` | `"SEASIDE_PIER"` with about 12 titles: "Salt & Sunset", "Feet Over the Water", "Pip Strikes Again", "Lighthouse Glow", "Two Cones, One Seagull", … |
| `BirdSystem.kt:497-501, 555-581` | Leave the scene out of `isOutdoorScene`, because Pip replaces ambient birds. Exception: the lighthouse-tap flock in the daytime. |
| `MainScreen.kt:333` | Nothing to add. Deep links work automatically (`--es scene SEASIDE_PIER`). |
| `PreferencesManager.getAllMoments()` + `OverlayDialogs.kt:867-877` | Optional: a daily moment "Ice cream by the sea". |

### Step 8: iOS (separate phase, coordinate first)
iOS has its own `TinyScene` enum (`ContentView.swift:9-43`), and its renderer is being rewritten right now (`TinyWorldStage.swift`). Once that rewrite lands:
1. Add `case pier` with its title, icon and `isIndoor = false`.
2. Add toast lines and a simple Canvas backdrop: sea band, pier planks, lighthouse.
3. Add a static seagull. Pip's state machine is Android-only for now.

**Longer term:** move `SceneType` and per-scene copy into `shared` for iOS to consume, so the two platforms stop drifting apart. Both CAMPFIRE and the 11-vs-12 count drift show this is already happening.

## 4. Tests

**`SharedCoreTest`**
- Add `"SEASIDE_PIER"` to the name list (Plan 01, 0.3).
- `DailyPromptCatalog` is already covered.

**`TinyUsOfflineTest.kt:70-89`**
- Add the new case to the exhaustive `when`.

**New file `app/src/test/java/com/example/SeasidePierSceneTest.kt`** (Robolectric SDK 34, `AmbientAudio().apply { isEnabled = false }`, same setup as `TinyUsOfflineTest.kt:49-59`):
1. `loadScene(SEASIDE_PIER)`:
   - `isCurrentSceneOutdoor` is true
   - home poses are `SIT_SNUGGLE` at 0.43/0.53
   - every `pier*` timer is 0
   - the bottle is visible
2. Ice cream: `onTouchPierIceCream` sets the timer to 8 and `emoteTimer > 0` on both characters. After `update(9s)` the timer is 0 and both characters are back at home.
3. Fishing: `onTouchGrandpaBao` with `pierRng = Random(42)`:
   - `update()` steps through IDLE→CASTING→WAITING→REELING→SHOWING→IDLE
   - the catch is non-null at SHOWING
   - with `FISH`, Mochi gets a walk target near Bao
4. Seagull:
   - With ice cream out and `pierRng` seeded, Pip reaches SNEAKING, then STEALING, then the timer becomes 0 and the couple are in `JOY_JUMP`.
   - `onTouchPip` during SNEAKING switches to ESCAPING and **no** theft happens.
5. Bottle:
   - The tap hides it.
   - The message contains today's prompt text.
   - A second tap does nothing.
   - After `update(31s)` it is visible again.
6. Scene switching: change scene mid-fishing, then come back; all state is reset.

**New file `PierLayoutHitTest.kt`** (pure JVM; mirrors the Plan 01 `SceneLayoutHitTest`):
- A tap at each drawn centre returns that prop, at phone (1080×2400) and tablet (1600×2560) sizes.
- No overlapping centres.
- Mochi's position resolves to the cat, not to the bucket or Bao.
- Pip is untappable while offscreen.
- The walk bounds exclude Bao's crate and the sea.

**Gates:** the privacy check `:verifyPrivacySafeguards` must still pass (Bao's lines must not contain any names or identifiers), and the full suite must be green.

## 5. Rough order and effort

| # | Chunk | Est. |
|---|---|---|
| 1 | Enum, state fields, loadScene, exhaustive `when`s, an empty draw function (it compiles and loads) | 1–2 h |
| 2 | `PierLayout`, the static drawing and cache, sea and lighthouse, time-of-day palettes | 3–4 h |
| 3 | Bao: sprite, fishing state machine, handler and tests | 2–3 h |
| 4 | Pip: sprite, state machine, foreground hook, theft, shoo and tests | 3–4 h |
| 5 | Ice cream, bottle (daily prompt), sea splashes, SFX | 2 h |
| 6 | Watch cinematic, autonomous moments, Polaroid titles, icon | 1–2 h |
| 7 | Manual polish pass on a phone and a tablet emulator | 1–2 h |
| 8 | iOS port (after the TinyWorldStage rewrite lands) | 2–3 h |

**Total:** about 15–22 hours.

## 6. Acceptance criteria

- [ ] The scene appears in the picker and in shuffle, and deep-links via `--es scene SEASIDE_PIER`.
- [ ] Each of the 6 tappable props responds only when tapped on its own sprite. Mochi always responds to her own taps.
- [ ] The couple always return to sitting after any reaction, watch scene or walk.
- [ ] Pip can steal an ice cream and can be shooed. Neither leaves stuck state behind.
- [ ] The bottle shows the same daily prompt that Daily Tiny Moments shows that day.
- [ ] The night, sunset and rain variants all look intentional.
- [ ] No per-frame allocations in the new draw code; the particle count stays bounded.
- [ ] All tests pass, `:verifyPrivacySafeguards` passes and CI is green.
