# Plan 11: visitors

Written 2026-10-07 by GROWTH for the user, who picked two visitors; built by FEATURES the same day at the user's request. The old man is "Billionaire" (the user also wrote "Billion"; one string to change if that's the name).

## Ground rules

- **Rare and short.** At most one visitor a day, in a scene that fits. They stay a minute or two, then wander off. Nobody waits for them, and nothing is missed.
- **About the couple.** Each visit makes a small couple moment and leaves a keepsake. No dialogue trees, quests or friendship meters.
- **A twist each** ([twists, not grinding]): something you don't expect.
- They're built like the Friday fox (plan 10, D): a small store for when each last came, a visit state machine in the engine, their own World* drawing file, entries in the collection book, and renders and tests.

## 1. The street painter

- **Where and when:** in the meadow or on the evening walk, by day, about once a week.
- **The visit:** an easel is set up at the edge of the scene, and the painter dabs at it while looking at the couple. Tapping the easel shows the work in progress. After a minute the painter turns the canvas round: a tiny pixel painting of the two of them, in their current outfits, in that scene.
- **The twist:** the painting is of that exact moment (the weather, the time of day, what they wear, Mochi if she's there). It's drawn from the live scene at the moment it's finished, not a fixed picture. If they were holding hands or hugging, the painting shows that.
- **Keepsake:** the painting hangs on the loft wall (the newest one; earlier ones stay in the collection book). It counts as a gift for both of them.
- **Text:** a line or two from the painter ("Hold still... there. That's you two."). No name needed; "the painter".

## 2. The old couple on the pier bench: Billionaire and The Great

- **Who:** an old man the user named **Billionaire** and an old woman named **The Great**. They're a long-married couple: the couple's future, in a way.
- **Where and when:** on the pier bench, at sunset or on a calm evening, a few times a week.
- **The visit:** they sit side by side and quietly mirror what the couple does a moment later: when the couple holds hands, so do they; a shared snack, a hug, a head on the shoulder. Tapping them gets a short line, alternating between the two:
  - Billionaire: "Richest man on this pier. Ask me why." (he looks at The Great)
  - The Great: "Fifty years. He still steals my fries."
  - Billionaire: "I proposed right there. She said 'maybe'."
  - The Great: "Don't wait for a reason. Hold her hand."
- **The twist:** the mirroring. And on the couple's anniversary (from the special calendar), the old couple is always there and leaves the bench with a little note under a pebble: "For the two of you, from the two of us."
- **Keepsake:** the note from the anniversary visit (kept in Our Story). Otherwise just the moment.
- **Art:** both at character size, grey-haired. He wears a cardigan and a flat cap (a joke on his name); she wears a shawl and big glasses. Their sitting poses can reuse the couple's sitting and holding-hands poses with an older palette.

## Build checklist

- [x] V-1. A `Visitors` model and store: when each visitor last came, and the visit rules (once a day at most, scene and time of day). *(`data/Visitors.kt`: `Visitors.mayCome`, `VisitorStore`. The painter comes at most every 6 days, the old couple every 2; one visitor a day; they turn up about 6 s into a scene that fits.)*
- [x] V-2. The painter: easel, painting progress, the reveal, the painting rendered from the live scene, and the loft wall. *(`PainterVisit` in `scene/Visits.kt`, drawn in `WorldVisitors.kt`. He walks in, sets up, dabs for 45 s (tap: "Hold still... there."), turns the canvas round. The `Painting` keeps that moment (scene, weather, light, outfits, holding hands or hugging, Mochi) and `drawPainting` paints it with their real looks; the newest hangs in the loft; all of them in `PaintingsDialog`.)*
- [x] V-3. The old couple: sprites, sitting on the pier bench, the mirroring (follow the couple's poses with a short delay), the lines, the anniversary note. *(`OldCoupleVisit`: they stroll in arm in arm, sit on their own bench and echo the couple 1.5 s later (hands joined, snuggle, hug, a snack). The user asked for the main girl's skin tone on both and faces and clothes of their own: Billionaire fair-skinned in a navy three-piece suit with a red bow tie, a gold watch chain, white brows, a moustache and a flat cap; The Great in a long plum dress with pearls, a brooch and a cream shawl, her grey hair in a bun (no glasses, at the user's request). Tap him or her for their own lines; The Great says "Elegance is the only beauty that never fades." On the anniversary they always come and leave a note under a pebble.)*
- [x] V-4. Collection book entries (the painting, "Billionaire and The Great"). *(And the note, hidden. The painting row opens the paintings; the note row opens the note.)*
- [x] V-5. Tests (the visit rules, the mirroring, the anniversary note) and renders. *(`VisitorsTest`, `VisitorsEngineTest`, `VisitorsPreviewTest`.)*
