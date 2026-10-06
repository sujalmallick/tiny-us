# Plan 07 D1: requests from the couple (design for FEATURES)

Written 2026-10-06 by GROWTH. Status: **reviewed by FEATURES on 2026-10-06; agreed, with the changes in "FEATURES review" below. FEATURES' side is built (see "Built so far" at the end); GROWTH's side is next.**

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

## FEATURES review (2026-10-06)

Agreed overall: it is rare, has no pressure, and reuses existing props. Answers first, then the changes.

### Answers

1. **Use one `Behavior.ASK_FOR_SOMETHING`.** The director picks the kind from a small table: whether the kind fits, times a per-character bias (the girl for SONG, the boy for SNACK), skipping the last kind used. Five separate behaviors would each get their own recency penalty in the brain and fight over a cooldown they share anyway. The `requestAvailable` context flag is right as proposed.
2. **Stay where they are, turned toward the prop.** Walking adds ways to fail (a blocked path, the partner already standing at the spot). The record player and the guitar are off-limits to the routine. And in the loft the couple doesn't walk freely at all (see below).
3. **The 25 s window and the 240 to 360 s gap are right** (480 s after an ignored request). Also: don't start a request in the 20 s after a cinematic or scene message, or while either of them is asleep or dozing (`SLEEP`, `RARE_DOZE_OFF`).
4. **Yes, add requests to the filmstrip.** Add an internal hook such as `startRequestForTest(kind, asker)`, so neither the filmstrip nor the engine tests depend on chance. The grant, fade and limit tests seed `behaviorBrain.random` like `AutonomyEngineTest` does.
5. **Scenes to exclude or handle:**
   - **The scooter ride:** already excluded.
   - **The loft:** the couple is drawn by `LoftSprites.drawCuddledCouple`, not `PixelArtRenderer.drawCharacter`, so no emote bubble or held item shows there. The request bubble needs its own drawing call in that path, anchored over the cuddled sprite's heads. Mochi isn't drawn in the loft either (`isCatInScene` excludes it), so MOCHI never fits there.
   - **Mini-games and other modes:** the ones already listed (dreams, watch scenes, catch, stars, cooking, fishing).

### Changes to the proposal

- **Bubble size.** PR #20 redrew `drawEmoteBubble`. It is now 11 x 10 at half a character pixel, with **7 x 6** icons in `EMOTE_ICONS` (`PixelCanvas.kt`). Draw the five request icons on that grid, not 9 x 7.
- **Tell requests apart from emotes.** Emotes are static and over in a couple of seconds. Give the request bubble a gentle bob and a gold outline so players learn it's tappable.
- **One bubble at a time.** While a request is up, the asker's emotes are suppressed (the request wins), and like emotes the bubble hides while that character is speaking. Otherwise two bubbles stack in the same spot.
- **Granting uses held items** (PR #20). A granted TEA puts a mug in the asker's hand (`hold(HeldItem.MUG, ...)` with a sip), so the thanks reads without any text. SNACK keeps the `EAT_MOMO` pose; WARM and SONG need no item.
- **Handler names.** The treat jar is `onTouchKitchenTreatJar()` and the pier cart is `onTouchPierIceCream(cw, ch)`. The others exist as named.

### Ownership

As proposed: FEATURES does the `Behavior`, the brain's weights, the director and `ActiveRequest`, and the `requests.onUsed(...)` lines. GROWTH does the bubble and icons (including the loft path), strings, progress events and the little firsts. Each side writes the tests for its part.

## Built so far (FEATURES, 2026-10-06)

The routine side is done on branch `feature/couple-requests`. A request starts, waits and is granted or fades; nothing shows on screen yet.

- `scene/autonomy/CoupleRequests.kt`: `RequestKind` (WARM, TEA, SONG, SNACK, MOCHI) and the timing, as in the review.
- `Behavior.ASK_FOR_SOMETHING` (category `REQUEST`) and `BehaviorContext.requestAvailable`.
- `SceneEngine` (in its "Requests (plan 07 D1)" section):
  - `requestFits`: which kinds fit where, as in the table, except that the kitchen's SNACK is the fridge (it already has snack lines) and the treat jar grants MOCHI, since it is Mochi's.
  - `grantRequest(kind)`: added as the first line of each granting tap handler.

### What GROWTH can build on

| API | Use |
|---|---|
| `engine.requests.active`, `.kind`, `.fade` (1, falling to 0 over the last 5 s) | Draw the bubble: which icon, and how faded. |
| `engine.requestAsker` | The character to put it over (null when there's no request). In the loft, place it over the cuddled sprite. |
| `announceRequest(c, kind)` (private, in `SceneEngine`) | Where the asker turns toward the prop. Add the spoken line here. |
| `engine.onRequestGranted: ((RequestKind) -> Unit)?` | Called on every grant: the place for `ProgressEvent.RequestGranted(kind)` and the thank-you line. Hearts, sparkles and the tea mug already play. |
| `engine.startRequestForTest(kind, asker)` | Starts one at once, for previews, the filmstrip and bubble renders. |

GROWTH's side, done on branch `feature/d1-request-bubble`:
- the five 7 x 6 icons (blanket, mug, note, cookie, Mochi), drawn by `PixelArtRenderer.drawRequestBubble` with a gold outline, a gentle bob and the fade;
- the asker's emote hidden while a request is up (`PixelCharacter.requestIcon`, synced in `updateRequests`);
- the loft path (`LoftSprites.drawCuddledCouple` draws it over the sofa heads);
- the spoken line as the caption when the request starts, and the thanks in the asker's speech bubble;
- `ProgressEvent.RequestGranted`, and the firsts "A request granted" and "Every little wish";
- `RequestBubbleTest`, the progress test, and `SCENE_PREVIEW_REQUEST=TEA,girl` plus a bubble sheet (`COZY_SPRITES_DIR`) for renders.

Tests: `CoupleRequestsTest` covers the timing, the brain's weight, granting at the right prop (and not at a wrong one), fading, a mini-game dropping the request, a request starting by itself in the loft (never in the first 90 s), and none on the scooter.

