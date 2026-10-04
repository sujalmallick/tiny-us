# Plan 05: Context-aware autonomous behavior

**Goal:** when nobody is tapping, the couple still makes small, believable decisions. They wander, use props, talk, cuddle, play with Mochi, notice things, and react to the weather and the time of day.

**Status:** built on branch `autonomy/behavior-system`.

## How it works

```text
idle → observe context → candidate behaviors → base weights → context modifiers
     → cooldowns + recency penalty → weighted random pick → walk to target
     → perform → react → rest (2.5–6.5 s) → decide again
```

| Piece | Where | What it does |
|---|---|---|
| `BehaviorBrain` | `scene/autonomy/BehaviorBrain.kt` | Pure Kotlin and unit-tested. Holds the base weights per `Behavior`; context modifiers for partner and Mochi distance, weather, time of day, indoor/outdoor, home distance, what's available, scooter and loft; cooldowns; and a recency penalty (×0.12 / 0.35 / 0.6 / 0.85 over the last four). The pick is weighted random, so the heaviest option is likely but never certain. |
| `AutonomyAgent` | `scene/autonomy/AutonomyAgent.kt` | One per character: the IDLE → WALKING → PERFORMING routine, its memory, and its current target. |
| `SceneSpots` | `scene/autonomy/SceneSpots.kt` | Safe interaction points per scene (see below), each with a facing direction, a pose and a dwell time. |
| `Discovery` | `scene/autonomy/Discovery.kt` and `ui/WorldDiscovery.kt` | Finds: a wildflower, red leaf, folded note, Mochi's toy, a seashell or a star pebble. The first appears after about a minute, then every 45–90 s; unnoticed ones fade after 50 s. |
| Director | `SceneEngine.kt`, "Autonomous behavior" section | Fills the context, asks the brain, walks the character with `moveTo` (no teleporting), plays prop effects, and draws the partner into joint behaviors. |

Interaction points are only props that are **safe to self-trigger**. Nothing opens a dialog, changes the scene or starts real music. The mailbox, journal, photo frame, calendar, wardrobe and record player are never touched.

## Priority

1. **User taps and drags.** These call `notifyUserInteraction()`, which halts any autonomous walk and pauses autonomy for 5 s.
2. **Watch-scene cinematics, dream mode and the sleeping living room.** Autonomy stops, then waits 3 s after the cinematic ends.
3. **Opening scripts.** Autonomy starts once the script ends and both characters have settled. Home spots are captured at that moment.
4. **Couple, environment, weather and ambient behaviors.** These are all chosen by the brain.

The scene idle loops use `idleLoopMayPose()`, so they don't override a character that autonomy owns. A character counts as owned while mid-activity or away from home. This includes the loft's position snap and the WALK scene's facing lock. In the cafe, sunroom, campfire and pier, `settleToHome` steps aside the same way.

## Scenes

- **Roam, then drift home:** after an outing or two, `GO_HOME` carries a +45 weight.
- **Loft:** mostly in place, with the occasional trip to the window, the shelf or the fairy lights.
- **Scooter ride:** stays seated. Only in-place behaviors are allowed (look around, look at the sky, talk, shooting star).
- **Scene moments:** the existing hand-made moments are still used, picked by the brain as `SCENE_MOMENT` when both characters are at home.

## Rare events

These share a cooldown: none for the first 75 s, then at least 150–210 s apart.

- A flower gift (outdoors).
- A shooting star (outdoors at night).
- Dozing off together (at night, when close).
- A happy dance (by day, not in rain).
- Mochi zoomies.

## Text

All new lines are `scene_auto_*` entries in `strings.xml`, looked up through `GameText`. None contain emoji.

## Tests

- `BehaviorBrainTest`: weights, context, anti-repeat, rarity, scooter and loft rules.
- `AutonomyEngineTest`: the couple stays busy, never teleports, steps aside for taps and cinematics, drifts home, picks up discoveries, stays put on the scooter, talks, and huddles in the rain.
- `AutonomyFilmstripTest`: opt-in. With `AUTONOMY_PREVIEW_DIR` set, it writes contact sheets of each scene over about 45 s, as a visual check without the emulator.
- Tests that check one scene's own mechanics set `engine.autonomyEnabled = false`.
