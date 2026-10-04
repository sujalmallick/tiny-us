# Plan 03: Pixel-Art Overhaul (consistent pixel look and cozy "game" feel)

**Goal.** Every frame of Tiny Us should look and move like real pixel art:
- one pixel size everywhere,
- no smooth or anti-aliased edges,
- a coherent colour palette,
- framing made for tall phones,
- motion that feels like a cozy game.

**Scope.** Android first. iOS follows using the same rules (see "iOS" at the end).

---

## 1. Why it doesn't look like pixel art today

| Problem | Where it comes from (measured in the code) |
|---|---|
| **Mixed pixel sizes** | The world is drawn at full screen resolution with `pixelScale = (width / 115).coerceIn(3, 5)`, computed separately in **27 places**. Characters use `pixelScale * 1.38` (6 places), so scenery blocks are about 5 px and character blocks about 6.9 px. Particles, glows and UI each use their own sizes. |
| **Smooth shapes** | About **296** `drawCircle` / `drawOval` / `drawArc` / `drawLine` / `drawRoundRect` / gradient calls outside the three redrawn scenes. All are anti-aliased at screen resolution. |
| **Muddy colours** | **3,357** colour literals (**1,322 unique**). Night, sunset and dimming are see-through overlays that invent colours outside any palette. |
| **Tiny characters on tall phones** | Characters are about 5% of screen height. Many scenes are half sky or empty floor on 20:9 screens. |
| **Floaty motion** | Positions tween smoothly at sub-pixel precision, and particles move at screen-pixel speed. |
| **Non-pixel UI** | Material components, the system font and emoji (being reworked by the UI session's "Cozy Frame" pass). |

The grid-snapping in `WorldSpecialScenes.kt` (`gridP`, `px/pOval/pLine`) patches this for three scenes only. This plan replaces it with a fix at the renderer level that covers the whole app.

---

## 2. Principles (the rules every phase follows)

1. **One game pixel.** The world is drawn at a small virtual resolution and enlarged by a **whole number** with no blurring (nearest-neighbour).
2. **No partial pixels.** The final frame has no anti-aliased edges and no semi-transparent pixels.
3. **One palette.** The final frame uses only colours from the Tiny Us palette. Gradients use dithering, not blends.
4. **Light by palette, not overlays.** Time of day and weather shift the palette instead of tinting with alpha.
5. **Stepped motion.** Sprites animate on a frame clock of about 10 fps, and everything moves in whole game pixels.
6. **Readable at a glance.** The couple are big enough to feel present, about 10–12% of screen height.

---

## 3. Phases

Each phase ships behind `FeatureFlags.PIXEL_RENDERER` until it's solid, so the old and new looks can be compared and switched back.

### Phase 0: Groundwork (small)

- **One scale source.** Add `engine/WorldViewport.kt`:
  - `scale` is a whole number: `max(2, round(screenWidthPx / 216))`.
  - `width = screenWidthPx / scale`, `height = screenHeightPx / scale` (rounded down).
  - Helpers convert screen coordinates to game coordinates and back.
  - Replace the 27 local `pixelScale` computations with it.
- **Golden screenshot tests.** Commit the Robolectric `@GraphicsMode(NATIVE)` renderer used during the scene redraw as `ScenePreviewTest`. It renders every scene at day, sunset and night to PNG. Reviewers compare images, and later phases add pixel-purity assertions (Phase 2).
- **Performance baseline.** Measure frame time per scene on the Pixel 8 Pro emulator and on one low-end device, so later phases can be checked against it.

*Acceptance:* no visual change; every pixel scale comes from `WorldViewport`; golden PNGs are generated in CI as build artifacts.

### Phase 1: Low-resolution world renderer (the biggest single win)

- **Draw small, enlarge sharply.**
  - In `PixelWorldView`, draw the world into a reused offscreen `ImageBitmap` of `WorldViewport.width × height` (about 216 × 480 game pixels on a phone).
  - Then `drawImage` it to the screen at `scale×` with `FilterQuality.None`.
  - Any leftover screen pixels are split evenly between top and bottom (letterbox), filled with the scene's edge colour.
- **p = 1.** All existing drawing code already works in `p` units. Inside the offscreen pass `p` becomes 1 game pixel, so most of the 20k lines of drawing code keep working unchanged.
- **Characters at the same scale.** Remove the `× 1.38` character scale. Characters become 18 × 26 game pixels. If they read too small after the Phase 3 framing, the fix is redrawing them at a larger grid (Phase 6), not scaling.
- **Particles and the engine work in game pixels.**
  - `SceneEngine`, `ParticleSystem` and the layouts (`SceneLayouts.kt`) receive the game width and height instead of screen pixels.
  - Speeds currently tuned in screen pixels get divided by `scale` once.
- **Input.** Tap and drag offsets are divided by `scale` before hit-testing, so all `*Layout.hitTest` code keeps working in game pixels.
- **Compose overlays.** Speech bubbles, the message box and the dream overlay convert game coordinates to screen coordinates with `WorldViewport` (they keep crisp text).
- **Retire the patch.** Remove `gridP`/`px` snapping from `WorldSpecialScenes.kt` once everything is drawn at game resolution.

*Acceptance:*
- On every scene the block size is identical for scenery, characters and particles (checked by eye and by golden PNG).
- All tap targets still hit (`SceneLayoutHitTest`, `PierLayoutHitTest`, `SeatedScenesTest` stay green).
- Frame time is the same or better, since far fewer pixels are drawn.

*Risk:* text drawn inside the world canvas (the stall signboard, the milestone sign) becomes chunky. Draw those with a pixel font (Phase 5) or as tiny pixel glyphs.

### Phase 2: Pixel purity (no fuzzy edges, one palette)

- **Palette.** Define `TinyPalette`: 48 colours (warm wood, brick, foliage greens, sky blues, skin and hair ramps, night indigo, accents), with ramps of 3–4 shades per material. It lives in `shared/commonMain` so iOS uses the same palette.
  - Skin and hair ramps must cover every `AvatarPalette` swatch (Make Us), and the avatar palettes are rebuilt from these ramps.
- **Quantize pass.** After the offscreen world is drawn, one pass maps every pixel to its nearest palette colour and snaps alpha to 0 or 1. This removes all anti-aliasing and off-palette colours **without touching the 3,357 colour literals**.
  - Android 13+: an AGSL `RuntimeShader` on the GPU, using a 32³ lookup table.
  - Android 8–12: a CPU pass over `IntArray` pixels with the same table. About 100k pixels per frame, well under 1 ms.
- **Dithering.** The same pass applies ordered (Bayer 4×4) dithering between two palette neighbours for gradients: sky bands, fire glow, lamp light, fog on glass.
- **Time-of-day lighting.** Replace the see-through dark and warm overlays (`ambientDimming`, night tints) with **palette shifts**: day, sunset and night lookup tables, each a remap of the base palette. Lamps, candles and the campfire become "light mask" shapes that switch the night table back to the day table inside their radius, giving the classic stepped pixel light.

*Acceptance:*
- A new `PixelPurityTest` renders each scene and asserts that every pixel is opaque and in `TinyPalette`.
- Night and sunset look like palette shifts, with no grey wash.

### Phase 3: Framing for tall phones (composition)

- **Per-scene camera.** Each `SceneType` gets a frame: a game-space rectangle of interest plus a zoom of 1× or 2×, so the couple are about 10–12% of screen height. Tall phones get more background layers, not more empty floor.
- **Layered depth.** Split backgrounds into far, mid, near and foreground layers, with a subtle 1–2 game-pixel parallax when swiping or on idle "breathing".
- **Scene-by-scene review.** Re-lay-out each scene to fill the 9:20 frame with purpose: the meadow, under the tree, kitchen, couch, lantern walk, looking, momo stall, evening ride, cozy loft, rainy cafe, sunroom, campfire and seaside pier.
- **Safe areas.** Keep the top bar and heart button over sky or ceiling, never over the couple.

*Acceptance:* in golden PNGs for 3 aspect ratios (16:9, 19.5:9, 20:9) the couple and the scene's main props sit in the central safe area, and no scene is more than about 35% empty sky or floor.

### Phase 4: Motion and game feel

- **Sprite frame clock.** Character poses, blinks, steps and Mochi's tail advance on a shared 10 fps tick (`SpriteClock`). Positions round to whole game pixels.
- **Walk cycles.** Proper 4–6 frame walk cycles instead of a sliding bounce.
- **Particles.** Whole-pixel motion with readable speeds (coordinate with FEATURES' weather slowdown). Rain becomes 1×3-pixel streaks, snow 1–2-pixel flakes with drift, petals 2×2 pixels with a flutter frame.
- **Transitions.** A pixel-dissolve or iris-wipe scene change (dither-pattern mask) instead of a fade. A short squash-and-stretch on taps.
- **Feedback.** Light haptics on key taps; gentle UI sounds already exist.

*Acceptance:* recorded clips show stepped, readable motion and no sub-pixel shimmer.

### Phase 5: Pixel UI (with the UI session's Cozy Frame work)

- **Pixel font.** Bundle an OFL-licensed pixel font in `res/font` (candidates: Pixelify Sans, Silkscreen, Tiny5; pick by legibility at small sizes and the glyphs needed). It's used for titles, buttons and in-world text, while long body text keeps a clear sans for readability and accessibility.
- **Frames and buttons.** 9-slice pixel borders (wood, paper, glass styles), plus pixel toggles and sliders, built on `TinyChrome` from `ui-chrome-pass`.
- **Icons.** One 16×16 pixel icon set replacing Material icons and emoji (the no-emoji policy already exists on `ui-chrome-pass`).
- **Accessibility.** Contrast checks on the palette, content descriptions kept, and text scaling respected; pixel text falls back to the regular font above 1.3× font scale.

*Acceptance:* no Material default visuals remain on the main screen or in dialogs; the accessibility checks still pass.

### Phase 6 (optional): Hand-drawn sprites

Code-drawn art has a ceiling. For the final level of quality:
- **Workflow.** Aseprite sheets go into `res/drawable-nodpi` as PNG atlases, with a small loader and nearest-neighbour sampling.
- **First targets:** the couple (idle, walk, sit, hug, kiss, sleep; 4 directions where needed), Mochi, then hero props (tent, campfire, café counter).
- **Make Us compatibility.** Sprites use reserved palette indices for skin, hair and outfit, recoloured at load time from `AvatarAppearance`, so every couple still looks like themselves.
- **Art direction.** A one-page style guide (palette, light direction top-left, outline rules, character proportions) so all art matches.

*Acceptance:* sprites replace the code-drawn equivalents scene by scene, and golden and purity tests stay green.

---

## 4. Testing and quality gates

| Gate | Tool |
|---|---|
| Visual regressions | `ScenePreviewTest` golden PNGs (every scene × day/sunset/night × 3 aspect ratios) uploaded by CI for review |
| Pixel purity | `PixelPurityTest`: every pixel is opaque and in `TinyPalette` |
| Interaction | the existing `*LayoutHitTest`, `SeatedScenesTest` and offline tests, run in game coordinates |
| Performance | frame time at or under 8 ms on a mid-range device; offscreen bitmap and lookup table reused (no allocations per frame) |
| Accessibility | contrast of UI palette pairs ≥ 4.5:1 for text |

---

## 5. Order, branches and effort

| Phase | Depends on | Rough effort |
|---|---|---|
| 0 Groundwork | – | 0.5 day |
| 1 Low-res renderer | 0 | 2–3 days |
| 2 Palette, quantize, dithering, lighting | 1 | 2–3 days |
| 3 Framing for tall phones | 1 | 3–4 days (scene by scene) |
| 4 Motion | 1 | 2 days |
| 5 Pixel UI | merge of `ui-chrome-pass` | 2–3 days |
| 6 Hand-drawn sprites | 2, art time | ongoing |

- **Before Phase 1,** merge `ui-chrome-pass` and `ios/parity-first-pass` into `android-public-release` (see the merge plan) so the renderer change lands on one up-to-date line.
- **One branch per phase** (`pixel/phase-1-renderer`, …), each merged after its gates pass.
- **Show the user** a side-by-side of one scene (the campfire) after Phase 1 and after Phase 2 before rolling out to all scenes.

## 6. Risks

- **Small-text legibility.** In-world signs need pixel glyphs or an overlay layer (Phase 1 note).
- **Tuning drift.** Particle and walk speeds tuned in screen pixels need one-time re-tuning in game pixels.
- **Old Android performance.** The CPU quantize pass on Android 8–12 must stay under budget; fallback is quantizing only every other frame for static layers.
- **Merge friction.** Several sessions edit `PixelWorldView` and `SceneEngine`. Phase 1 touches both broadly, so it should run when no other session is editing them.

## 7. iOS

The iOS app draws its own world in SwiftUI. It adopts the same rules after Android Phase 2, reusing `TinyPalette` and `WorldViewport` constants from `shared`:
- render to a small `CGImage` with interpolation off,
- apply the same palette and dither lookup tables (Metal or Core Image),
- use the same frame clock.
