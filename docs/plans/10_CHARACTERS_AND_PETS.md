# Plan 10: Characters and pets

The user's batch of 2026-10-07, taken over by FEATURES from IOS. Everything lives in shared code so
Android and iOS get it together (no JVM-only APIs in commonMain). Each section is one PR, in the
user's order. Every section renders first and shows what looks wrong before anything is fixed,
since there's no emulator: the JVM renders in `ScenePreviewTest` are the device check.

Plan 09 I (characters' routines and likes) is skipped for now; this batch comes first.

## Twists, not grinding
Same rule as plan 09: no collect-and-repeat loops. Each section has a small twist.

| Section | The twist |
|---|---|
| A. Holding and using things | The item is in the hand and the pose shows it in use: sipping, reading, watering, stirring, reeling. Tapping a held thing makes them use it, and sometimes they offer it to the other. |
| B. Props and furniture | They sit on the bench, stand at the counter, lean on the railing, and are hidden behind the table where they should be. |
| C. Pets with things | Mochi actually goes to the saucer, the box, the toy; she can be behind the couch or in the box, never two Mochis at once. |
| D. The Friday fox | Every Friday a fox cub comes with a ball to play with Mochi. Miss a Friday and the ball is left behind for you. |
| E. A pets tab | Meet other pets one at a time (bunny, fox cub, hedgehog, duck and ducklings, owl, puppy); each has its own little habits, and you choose who lives with you. |

## A. Holding and using things (MOST IMPORTANT)
Today: `HeldItem` (MUG, BOOK, WATERING_CAN, MISTER, FLOWER, LEAF, LOVE_NOTE, SEASHELL, STAR_PEBBLE,
YARN_BALL, LANTERN, CAKE) is drawn by `drawHeldItem` / `drawHeldAtChest` in `engine/PixelCanvas.kt`.
Only BOOK and WATERING_CAN have "in use" art; the COOK pose draws a hard-coded spoon; the fishing rod is
drawn in the world with no arm holding it; the loft's cuddled couple never shows a held item.

- [x] A-1. Render every held item, carried and in use, standing and sitting, facing both ways (a contact sheet), and list what looks wrong. *(`HeldItemSheetTest`, written to HELD_SHEET_DIR. Wrong: everything "in use" was just held at the chest; the pan and the rod were missing from the hands; the loft handed out a book nobody could see.)*
- [x] A-2. Use poses for each item: mug raised to the mouth, book open in both hands, can tilted and pouring, mister squeezed, lantern held up, flower smelled, note read. *(`PixelArtRenderer.UseStyle`: SIP to the lips, SNIFF under the nose, LISTEN with a shell at the ear, LOOK held up under the eyes for notes, leaves and pebbles, TOSS for the yarn ball; eyes close for a sip, a sniff and a listen. The book, can, mister and lantern already looked right. Same poses sitting, a row lower.)*
- [x] A-3. A pan: held in the kitchen, stirred with the spoon, a little toss now and then. *(`HeldItem.PAN`: handed out at the stove (STIR_POT); held out over the stove with an egg that hops up every few seconds, and on a tap.)*
- [x] A-4. The fishing rod in the hands at the pier: cast, wait, reel, the line from the rod tip. *(`HeldItem.ROD`: in both hands while they fish, facing the water; the reel handle turns while reeling; the world line carries on from `PixelCharacter.rodTip`.)*
- [x] A-5. Held items in the loft's cuddled pose (or the loft book spot no longer hands out a book nobody sees). *(In the loft, browsing the shelf opens the book they read together for a while instead.)*
- [x] A-6. Twist: sometimes one offers the held thing to the other (a sip of tea, a look at the book). *("Want a sip?" / "Here, look at this.": the other has a sip, a sniff, a listen or a look, and hands it back.)*
- [x] A-7. Tests and renders. *(`HeldItemTest`: use styles, sharing and handing back, the pan at the stove, the rod while fishing; `HeldItemSheetTest` contact sheet; the pier render with the rod.)*

## B. Props and furniture
- [x] B-1. Render each scene's props with the couple at every spot; list layering mistakes. *(`ScenePreviewTest.writesSpotSheetsWhenAsked`, SCENE_SPOTS_DIR. Wrong: in the kitchen "sit at the table" sat them on the bare floor mid-room (and so did dinner); the living-room pouf seat sat beside the pouf; at the campfire they stood on top of the log to warm their hands; at the momo stall they stood on the crate, and the saucer and steamer spots stacked them on each other; at the telescope they faced away from the eyepiece.)*
- [x] B-2. Depth for props: characters in front of or behind furniture by their feet, not always on top. *(Done by placement: every spot now puts them in front of or on their prop, so nothing they use needs to be drawn over them. Furniture is still always behind the couple; a general depth sort is left for when a scene needs someone behind a prop.)*
- [x] B-3. Sitting and standing at props: the bench, couch, cafe chairs, campfire log, pier railing, counter. *(They sit on the stools at either end of the kitchen table (dinner too, facing each other), on the pouf, and on the log by the fire; stand beside the crate, at the counter, and at the telescope's eyepiece. The pier bench, cafe table and loft were already right.)*
- [x] B-4. Tests and renders. *(The spot sheets above; `GardenKitchenEngineTest` still checks the dinner seats.)*

## C. Pets with things
Known problems: Mochi is never depth-sorted against furniture; the cardboard box draws a second Mochi;
the milk saucer doesn't make Mochi walk to it; ice-cream cones on the pier sit at fixed offsets.

- [x] C-1. Render Mochi at every object; list what's wrong. *(`ScenePreviewTest.writesMochiSheetWhenAsked`, MOCHI_PREVIEW_DIR: Mochi in every scene, in her box, at her saucer. Wrong: tapping the box drew a second Mochi peeking out while the real one sat elsewhere; the saucer never brought her over. In the other scenes she sits clear of the props, so she isn't drawn over anything.)*
- [x] C-2. Fix the box (one Mochi, in the box), the saucer walk, depth against props. *(`SceneEngine.MochiErrand`: the box sends her walking over to hop in; only her head peeks out now and then, and she hops out beside it after a while. The saucer (a tap, or filling it) has her trot over and lap at it. The box spot moved next to the box.)*
- [x] C-3. Tests and renders. *(`MochiThingsTest`: into the box and out again, over to the saucer, never left in a box after a scene change; the Mochi sheet.)*

## D. The Friday fox
- [ ] D-1. A fox cub with a ball visits on Fridays, in an outdoor scene; plays catch with Mochi; leaves.
- [ ] D-2. The twist: a missed Friday leaves the ball behind.
- [ ] D-3. Tests and renders.

## E. A pets tab
Candidates from the website footer (tiny-us-site `src/components/FooterPlayground.tsx`): bunny, fox cub with
ball, hedgehog that curls up, duck with ducklings, owl; and the puppy from `RoamingDog.tsx`.

- [ ] E-1. Pixel sprites and habits for each.
- [ ] E-2. Meeting them (the unlock, with a twist rather than a grind).
- [ ] E-3. The tab: pick who lives with you instead of Mochi.
- [ ] E-4. Tests and renders.
