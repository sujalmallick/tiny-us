# Plan 07: Little firsts, cozy mini-games, deeper interaction

Written 2026-10-05. The user picked: little firsts with rewards (2), the five mini-games (3), and requests, Mochi and gifts (4).

## Ground rules

- **Cozy, never pressure.** The code already promises "no streaks, no levels, no XP" (`DateAdventureModels.kt`, `DataModels.kt`). Nothing can be lost, nothing fails, nothing expires, no timers that punish, no shop or currency.
- **Rewards add, never take away.** Every outfit, accessory and room theme that exists today stays free. Rewards are *new* items that unlock.
- **Offline and private,** as now.
- **Progress lives in `tiny_us_prefs`,** so Android backup and the in-app Backup & Restore carry it (other prefs files aren't backed up).
- **Every new line goes into `strings.xml`** through `GameText`, with no emoji.
- **The couple's routine (scene/autonomy) is FEATURES' work.** Anything that adds behaviours (the couple's requests) is agreed with FEATURES first.
- One branch per section, merged by a PR the user opens. Renders and tests for each section, as in plan 06.

## What exists to build on

| Today | Where | Kept or persisted? |
|---|---|---|
| Catching snowflakes, petals and leaves; snowman stages | `SceneEngine.onCatchWeather`, `growSnowman` | No, reset when the weather changes |
| Three constellations to tap | `onTouchConstellation`, `WorldSkyDrawing` | No |
| Kitchen props (stove, fridge, board, table, treat jar) | `SceneEngine` 4796-4968 | Messages and poses only |
| Grandpa Bao fishes at the pier (`PierFishingPhase`, `PierCatch`) | `PierModels.kt`, `updatePierFishing` | No; the catches are his |
| Garden grows with days opened; keepsake blooms | `GardenGrowth`, `PreferencesManager` | Yes (days, bloom dates) |
| Mochi: petting cycles moods; treat jar | `onTouchCat`, `updateKitchenTreat` | No affection state |
| Discoveries (wildflower, red leaf, note, toy, seashell, star pebble) | `scene/autonomy/Discovery.kt` | No, cleared on pickup |
| Wardrobe: 11 + 5 outfits, 4 accessories; 4 room themes | `WardrobeDialog`, `RoomTheme` | Choice saved; nothing locked |
| Our Story milestones (day counts only) | `StoryTimeline.milestones` | Derived, not stored |

---

## A. Foundation: progress and the keepsake box

One small, testable progress store that everything else writes to.
- [x] A1. `Progress` (pure Kotlin model plus a store in `tiny_us_prefs`): counters (rainbows wished, full moons seen, snowmen built, recipes cooked...), the set of scenes visited and seasons seen, best scores, the "little firsts" earned (with dates), and unlocked rewards.
- [x] A2. **Keepsake box:** things the couple finds or catches are kept (discoveries, seashells and fish, bouquets, dishes), with counts. Gifts (D3) come from here.
- [x] A3. Events from the existing interactions feed it: rainbow wish, constellation, snowman finished, discovery picked up, Tiny Moment taken, scene visited, weather seen, and the day count.
- [x] A4. Included in Backup & Restore (it's in `tiny_us_prefs`); a restore test.

## B. "Little firsts" milestones and rewards

- [x] B1. The list (19 to start; the mini-game ones come with their games), for example:
  - first rainbow wish, first full moon together, first snowman, first star connected;
  - visited every scene, saw all four seasons, every kind of weather;
  - 10 / 50 Tiny Moments, first love note, first dream written;
  - 7 / 30 / 100 / 365 days together;
  - first dish cooked, first fish caught, first bouquet, first gift;
  - Mochi's first slow blink, a request granted.
- [x] B2. When one is earned, a soft toast plus both characters' heart emote, then an entry in Our Story (`StoryKind.MILESTONE`, from the stored dates).
- [x] B3. A "Little firsts" page (a chip in Our Story) (from the heart menu or Our Story): earned ones in colour with their date; unearned ones as a gentle hint ("Somewhere, after rain...").
- [x] B4. **Rewards:** *(all eight done: rainbow scarf, snowman beanie, star hoodie, starry-night theme, Mochi-ear headband, chef's apron (every recipe), fisher's bucket hat (first fish), flower crown (first bouquet))* about 10 new items that some firsts unlock. For example: a rainbow scarf (first rainbow), a star-pattern hoodie (all constellations), a snowman beanie (first snowman), a chef apron (5 dishes), a fisher's cap (first fish), a flower crown (first bouquet), a Mochi-ear headband (Mochi's fondness), a "starry night" room theme (100 days). Each needs new pixel art. Locked items show in the wardrobe as a silhouette with how to earn them.
- [x] B5. Tests: each first fires once; rewards unlock; nothing ever re-locks.

## C. Mini-games

Each is short, optional, can't be lost, and ends with a cozy result that goes into the keepsake box and the firsts. Started by tapping the obvious prop in its scene.

- [x] C1. **Catch together** (outdoor, any falling weather): tap the basket to start a 30-second round. Drag the basket under falling petals, snow or leaves; rare golden ones count extra; your best score is kept. *Reuses the weather particles.*
- [x] C2. **Stargazing puzzle** *(6 constellations: the 3 old ones plus Mochi's Whiskers, the Little Scooter, the Paper Kite)* (night, outdoor): tap a constellation to start; its stars show dimly, and you tap them in order (a line draws between each). Done: the constellation glows and is named. All three, then new ones (5 to 8 total, data-driven). *Reuses the star field and overlay.*
- [x] C3. **Cooking** *(a "Cook together" button in the kitchen opens the recipe card; the dish appears on the table; the recipe book is in Keepsakes)* (kitchen): a recipe card (e.g. pancakes, soup, dumplings, cookies, tea) shows 3 to 5 ingredients; tap them in order on the counter and fridge. The dish appears on the table and goes into a **recipe book**. Mistakes just wiggle; no failing.
- [x] C4. **Fishing** *(a "Fish together" button at the pier; a tap anywhere while the bobber is under; the line goes back in until they stop)* (pier): the couple takes a rod next to Grandpa Bao. Tap when the bobber dips, with a forgiving timing window. Catches are fish, seashells, an old boot, or a message in a bottle, each kept with a count. *Reuses `PierFishingPhase` and the bobber art.*
- [x] C5. **Garden care** *(three plots at the front of the meadow: tap to plant, water and pick; rain waters them too)* (meadow or sunroom): plant a seed (pick a flower), water it once a day, and it grows over a few days into a flower. Pick flowers to make a **bouquet** (a keepsake, and it can be given as a gift). *Grows alongside `GardenGrowth`; skipping days only slows it, never kills it.*
- [x] C6. Tests: each game's rules (pure Kotlin), plus renders of each game in play.

## D. Deeper interaction

- [ ] D1. **Requests from the couple** *(agree with FEATURES first)*: now and then one of them asks for something that fits the moment: a blanket when it's cold or snowing, tea in the loft, the umbrella in rain, a snack in the kitchen, a song by the record player. A small speech bubble with an icon; tap the thing to grant it. They react warmly, and it counts toward "a request granted". At most one at a time, rare, and it fades if ignored (no penalty).
- [x] D2. **Mochi's fondness:** *(done, including the Mochi-ear headband)* feeding (treat jar), petting, playing (a yarn ball or feather toy) and Mochi-toy discoveries slowly raise fondness, which never goes down. At thresholds: Mochi follows you more, sleeps by the couple, a slow blink (a first), and unlocks the Mochi-ear headband. A small heart meter on Mochi's tap.
- [x] D3. **Gifts:** *(the Keepsakes page in Our Story; gifts stand on a shelf under the kitchen clock)* open the keepsake box and give a found thing (a bouquet, seashell, star pebble, dish) from one partner to the other. A small give-and-receive animation; the gift then appears in the home (a shelf in the loft or living room, or the windowsill) and is remembered in Our Story.
- [ ] D4. Tests and renders.

---

## Order (suggested)

1. **A** (foundation). Everything else needs it.
2. **B** (little firsts, the page and Our Story). Rewards art (B4) can follow.
3. **C1 + C2** (catch, stargazing): they reuse the most, so they're quick.
4. **D2** (Mochi) and **D3** (gifts).
5. **C3** (cooking), **C4** (fishing), **C5** (garden): each needs new art.
6. **D1** (requests), once FEATURES agrees.
7. **B4** rewards art.

## Decisions for the user

- [x] Where the "Little firsts" page lives: in Our Story.
- [x] Rewards: the list in B4.
- [x] Mini-games: only when you start them.
