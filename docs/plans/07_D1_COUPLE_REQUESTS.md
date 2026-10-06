# Plan 07 D1: requests from the couple (design for FEATURES)

Written 2026-10-06 by GROWTH. Status: **proposal, waiting for FEATURES**. Nothing is built yet.

## The idea

Now and then, one of them asks for something that fits the moment: a blanket when it's cold, tea in the loft, a song, a snack, Mochi. A small bubble with an icon appears over the one asking. If the player taps the thing it's about, the request is granted. They react warmly, and it counts toward the little first "A request granted". If nobody taps it, the bubble fades and nothing else happens.

## Ground rules (from plan 07)

- **Never pressure.** No timer is shown and nothing is lost. A faded request is not mentioned again, and nothing counts misses.
- **Rare.** At most one request at a time, a few minutes apart, and none in the first 90 seconds of a scene.
- **Only while the routine runs.** No requests during dreams, watch scenes, the living room's sleep, or any mini-game (catch, stars, cooking, fishing).
- **Granting reuses what's already there.** Every request is answered by tapping an existing prop, so nothing needs new hit areas and nothing opens a dialog.
- All new text goes in shared `composeResources` strings as `Res.string.request_*`, with no emoji.

## How it fits the routine (plan 05)

Proposal: a new `Behavior.ASK_FOR_SOMETHING`, in a new `BehaviorCategory.REQUEST`.

| | Proposal |
|---|---|
| Base weight | 0, raised to about 3 only when a request kind fits the scene and context (see the table below) |
| Cooldown | Its own couple-wide cooldown: none in the first 90 s, then 240 to 360 s between requests. After an ignored one, 480 s. It's separate from the rare-event cooldown, so requests don't crowd out the rare moments. |
| Perform | The asker turns toward the camera (or the prop), with the `TALK` pose. The bubble then stays for up to 25 s, while the routine carries on (they can still wander). |
| User taps | `notifyUserInteraction()` pauses the routine as now. It doesn't cancel the request; only granting or fading ends it. |

The director keeps a small `ActiveRequest(kind, asker, secondsLeft)`. The brain only needs one extra context flag: `requestAvailable`, true when some kind fits right now.

### Granting

Each tap handler that grants a kind gets one line, such as `requests.onUsed(RequestKind.TEA)`. If the active request has that kind, it's granted:

- both characters show a heart emote;
- the asker says a thank-you line;
- `ProgressEvent.RequestGranted(kind)` is recorded;
- the bubble pops with a few sparkles.

Otherwise the handler behaves exactly as it does today.

## Request kinds

| Kind | When it fits | Who asks | Bubble icon | Granted by tapping | Reaction |
|---|---|---|---|---|---|
| WARM | Evening or night in the loft or living room; night at the campfire | either | folded blanket | Loft: the sofa (`onTouchLoftSofa`). Living room: the couch throw (`onTouchCouchThrow`). Campfire: the fire (`onTouchCampfire`), to sit closer. | They share it: SIT_SNUGGLE, "Much better." |
| TEA | Loft or kitchen, not late at night | either | cup with steam | Loft: the table (`onTouchLoftTable`). Kitchen: the stove (the kettle, `onTouchTeakettle`, or the pot, `onTouchPot`). | Steam puff; "Just how I like it." |
| SONG | Loft and campfire, evening or night | girl more often | music note | Loft: the record player (`onTouchLoftRecordPlayer`). Campfire: the guitar (`onTouchCampGuitar`). | Music notes, a little sway |
| SNACK | Kitchen, cafe, pier | boy more often | cookie | Kitchen: the treat jar. Cafe: the pastry. Pier: the ice-cream cart. | EAT_MOMO pose; "Shared, of course." |
| MOCHI | Any scene where Mochi is visible and awake | either | cat face | Mochi (`onTouchCat` or the scene's Mochi handler) | Mochi trots over; both pet her |

Notes:

- **Umbrella is out.** The boy already raises the umbrella by himself in the rain, so a request for it would never make sense.
- **Snow outdoors is out too.** No outdoor scene has a blanket to tap, and the routine already huddles them in bad weather (`SHELTER_CLOSE`).
- The record player and the guitar stay off-limits to the **routine**, as plan 05 says. Here only the **player** taps them, so no real music starts by itself. Granting SONG just plays what those props already play.

## Bubble

- It reuses the emote bubble frame (`drawEmoteBubble`), with five new 9x7 icons drawn in the same style. The tail points at the asker.
- A short line appears once, as speech, when the request starts (for example, "Could we have some tea?"), so screen readers announce it through the scene message.
- In the last 5 s the bubble fades. Its gentle bob stops instead of blinking or shrinking.

## Progress

- `ProgressEvent.RequestGranted(kind)` adds to `Counter.REQUESTS` and to `Seen.REQUEST_KINDS`.
- New little firsts:
  - `first_request`, "A request granted". Hint: "Sometimes one of them asks for something small."
  - `all_requests`, "Every little wish". Hint: "Warmth, tea, a song, a snack, and Mochi." It could unlock a reward later, for example a knitted blanket for the loft sofa, but none is proposed yet.

## Who does what (proposal)

| | Owner |
|---|---|
| `Behavior`, the brain's weights and context, director timing, `ActiveRequest` | FEATURES |
| `requests.onUsed(...)` lines in the tap handlers | FEATURES (or GROWTH, if FEATURES prefers) |
| Bubble icons and drawing, strings, progress events and firsts | GROWTH |
| Tests: brain weights (BehaviorBrainTest); grant, fade and limits in the real engine; a filmstrip with a request; a render of each bubble | Both, each for their part |

## Questions for FEATURES

1. One new `Behavior` with a kind chosen inside the director, or one `Behavior` per kind (five entries, each with its own weight)?
2. Should the asker walk to the prop first (so the request points at it), or stay where they are?
3. Is a 25 s window and a 240 to 360 s gap right for how often the routine runs?
4. Can requests be added to the filmstrip test so we can see one happen?
5. Any scenes where the routine is too busy for this (the scooter ride is already excluded)?
