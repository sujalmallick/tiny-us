# Plan 09: Birthdays, seasons and real couple life

Written 2026-10-06 by FEATURES, from the user's brief. It covers:
- the cozy parts of Stardew Valley, reworked for two people;
- a **mandatory birthday surprise**;
- simpler Tiny Games;
- small interactions for problems real couples have;
- the gaps between what www.tinyus.online promises and what the app does.

Status: **approved 2026-10-06; the user wants all of it built.** The work goes section by section (A, B, C...). Each section gets its own branch and PR when its checklist below is done.

The user's answers to the decisions at the end:
- The order is as suggested.
- Where the plan asks a question, FEATURES' proposal is used:
  - birthday hints are shown to whoever opens the app, with neutral wording;
  - festivals come in the middle of each real season;
  - notifications are opt-in;
  - characters' likes are included.

## Ground rules

- **Cozy, never pressure** (as in plan 07). No energy bars, money, shops, friendship meters, deadlines, streaks or failing. A missed festival or an unwatered plot costs nothing. It just waits, or comes back next year.
- **One shared phone.** Nothing syncs (`FeatureFlags.PARTNER_SYNC` is off), so the couple shares one device. The core couple mechanic is "pass the phone":
  - each partner makes a choice or writes something privately;
  - the screen hides it and asks for the other partner;
  - then both are revealed together.
  
  Sealed things (letters, wishes, gifts) rely on trust: "This is for Sprout. No peeking."
- **Offline and private.** No new permissions. Notifications reuse the existing offline Tiny Care alarms and are always opt-in.
- **Reuse before building.** Every section below names what it builds on.
- **New UI goes in `shared/commonMain`** so iOS gets it too (plan 08). New text goes in `composeResources` strings, with no emoji.
- **One branch per section,** each with renders and tests, merged by a PR the user opens.

## Twists, not grinding (user direction, 2026-10-07)

"Don't make this a farming simulator, we need twists." So:
- **No grind loops.** No crop counts or stock to manage, no daily chores, no timers to babysit, no "catch 30 of X" goals. A collection is a scrapbook, not a completion bar.
- **Every loop ends in a couple moment or a reveal.** Something one of them chose in secret, a surprise in the world, a note from the other. Not a number going up.
- **Each section has at least one twist,** listed in "The twist in each section" below. A section isn't done without it.

## Not taken from Stardew

Energy, gold and shops, friendship hearts to grind, crops that die, tools to upgrade, combat, and a calendar that punishes you for missing a day. Tiny Us already avoids all of these.

---

## A. Birthday surprise (mandatory, do first)

**Today:**
- Birthdays are free-text `yyyy-mm-dd` fields in Settings > Our World (`prefs.boyfriendBirthday` and `prefs.girlfriendBirthday`). Onboarding doesn't ask for them.
- On the day, the scene gets balloons and a garland (`SpecialDay.BOY_BIRTHDAY` and `GIRL_BIRTHDAY`), and each of them says a greeting line. That's all.

**The idea:** when a birthday comes, the app throws a small surprise party that the *other* partner helped prepare.

- [ ] **A1. Birthdays are easy to enter.**
  - An optional step in onboarding: "When are your birthdays?", with a skip.
  - A date picker in Settings instead of free text. The year is optional (month and day are enough).
  - Old strings migrate as they are.
- [ ] **A2. A sealed birthday letter.**
  - From two weeks before a birthday, a soft hint appears in the special calendar and on the mailbox: "A birthday is coming. Want to leave a sealed letter?"
  - The partner writes it. It's stored sealed, and hidden from Love Notes and Our Story until the day.
  - Optional; if nobody writes one, the party still happens with a line from the character.
- [ ] **A3. The surprise, on the first open of the day.**
  - The home scene opens with the lights off. A caption says "Hmm, why is it so dark?", and a tap turns the lamp on.
  - "Surprise!": a banner with the birthday person's name, confetti, Mochi in a party hat, and the partner's character holding a cake. The cake is a new held item, `HeldItem.CAKE`, drawn on top of PR #20.
  - Tap the candles to blow them out, one tap each. A "Make a wish" field is optional and saved privately to Our Story.
  - A gift box wobbles. Tapping it opens the sealed letter from A2, if there is one.
  - The party lasts the whole day:
    - balloons in every scene;
    - a birthday frame on polaroids;
    - the birthday person's character wears the party hat;
    - a "Happy birthday" line when you tap either of them.
- [ ] **A4. Keepsakes.**
  - A party hat for the wardrobe, unlocked for good.
  - A "Birthday" entry in Our Story with the polaroid, the wish (shown privately) and the letter.
  - If the birth year is known, "Sprout turned 25". No age is ever shown without a year.
- [ ] **A5. Morning reminder** (opt-in, on the existing offline alarm path that survives a reboot): "Something is waiting for you in Tiny Us today." It never says whose birthday it is, so the surprise holds.
- [ ] **A6. Edge cases:**
  - Both birthdays on the same day: one double party.
  - 29 February: celebrated on 28 February, as `SpecialDays` does now.
  - The app isn't opened on the day: the party waits until the next open, within 3 days ("A belated surprise!").
  - Only one birthday entered: that one still works.
- [ ] **A7. Tests and renders:** the date logic (midnight, time zones, 29 Feb, same day); the letter stays hidden until the day; a filmstrip of the surprise; the cake and party hat renders.

## B. Simpler Tiny Games (user request)

**Today:**
- `TwoPersonMiniGameDialog` has four tabs: Would You Rather (3 questions), Who Knows Who (1), Sweet Preferences (1) and Memory Trivia.
- Memory Trivia has **0 questions**, so it falls back to the first question.
- Each partner picks, then Reveal.

**The idea:** one game at a time, no tabs, rounds of five, and the result plays out in the world.

- [ ] **B1. One entry, one flow.**
  - Start from the game board in the home, or from Settings.
  - Pick one of three games, then a round of 5 questions.
  - "Bean, pick" (the screen hides it), then "Sprout, pick", then Reveal.
- [ ] **B2. The three games:**
  - **This or That**: Would You Rather and Sweet Preferences merged.
  - **Guess Me**: one answers about themselves; the other guesses.
  - **Our Story Quiz**: replaces the empty Memory Trivia. Questions are built from the couple's own data, so it is never empty:
    - "Where did we take this polaroid?"
    - "What did we cook first?"
    - "Which month did we meet?"
    - "What did Mochi find on the pier?"
    
    It's hidden until there are enough memories.
- [ ] **B3. The result plays in the world.** A match makes both characters jump with hearts. A miss gets a playful shrug ("Opposites attract"). The end of a round shows "You matched 3 of 5", and the best round is kept. Nothing is lost.
- [ ] **B4. Bigger decks:** 60 or more questions per game in strings, drawn without repeats (`AntiRepeatRandomPicker`).

## C. Small fixes to real couple problems (user request)

Each is a tiny ritual in the world for a real friction. None of them is therapy, and none keeps score of who is "right".

- [ ] **C1. "What should we eat?" (the Dinner Decider).**
  - The problem: "I don't mind, you pick."
  - Eight options are shown (food, a film, or something to do; each partner can add their own).
  - Each partner secretly vetoes up to 2. The rest go on a little wheel in the kitchen, and the couple spins it together.
  - Picking "Cook it" leads into the cooking game.
- [ ] **C2. The Make-Up Bench (cooling down after a small fight).**
  - Start from the heart menu with "We need a moment".
  - The two sit at either end of a bench under a small grey cloud.
  - Each writes privately, from a couple of gentle prompts: "I felt..." and "What would help is...". Nothing shows until both are done.
  - Both are revealed side by side. Then they choose a hug, a cup of tea (the TEA held item), or "talk later" (a gentle reminder).
  - The cloud clears to a rainbow and the bench slides them together. It's saved only if they choose to keep it.
- [ ] **C3. The Thank-You Jar (feeling unappreciated, the mental load).**
  - Two taps drop a thank-you into a jar on the kitchen shelf: a chip ("made coffee", "did the dishes", "listened") or a line of text.
  - The jar fills with coloured pebbles. A full jar (20) becomes a keepsake, Mochi celebrates, and a new jar starts.
  - On rainy days, one of them may read an old thank-you out loud.
- [ ] **C4. "Open when..." letters (missing each other, bad days, being apart).**
  - Sealed letters for a moment, not a date: "Open when you miss me", "...when you can't sleep", "...when we've argued", "...on a bad day".
  - Each one waits in the mailbox until its reader decides it's time.
  - It uses the same sealed-letter store as A2.
- [ ] **C5. Phones Down (time together without screens).**
  - Start a together timer for 15, 30 or 60 minutes.
  - The app dims to a night-light view of the couple on the sofa.
  - At the end there's a bloom in the garden and an Our Story line: "30 minutes, just us".
  - Leaving early is fine; nothing fails.
- [ ] **C6. Dates that matter.** Optional reminders for the anniversary and "month-iversaries", through the same opt-in alarms as A5. The special calendar already has the dates.

## D. Seasons with festivals (from Stardew's festivals)

**Today:**
- `SeasonalWeather.seasonOf(month, country)` knows the real season, flipped for the southern hemisphere, and weights the weather by it.
- `SpecialDays` covers birthdays, the anniversary, New Year, Valentine's, Holi, Diwali and Christmas.

**The idea:** one small festival per season, held for a 3-day window in the middle of the season. Each is something the two do *together*, and each leaves a keepsake. It's announced by a poster in the scene two days before. Miss it and it simply comes back next year.

- [ ] **D1. Spring: Blossom Picnic** (meadow). A blanket and basket under the blossoms. Each partner secretly picks a flower for the other's crown, then the crowns are revealed together. Keepsake: a flower-crown polaroid.
- [ ] **D2. Summer: Lantern Night** (pier, at dusk).
  - Each writes a private wish on a paper lantern, and they let them go together.
  - The wishes are sealed for a year. At next summer's Lantern Night, the lanterns come back: "Last summer you wished..."
- [ ] **D3. Autumn: Harvest Fair** (meadow and kitchen). The year's harvest (section E) becomes a harvest pie, cooked together. A pumpkin gets a face picked by each of them. Keepsake: the pie in the recipe book and a carved pumpkin on the porch until winter.
- [ ] **D4. Winter: Gift Exchange** (living room, around a little tree).
  - Each secretly picks a gift for the other from the keepsake box, the things they found this year, and adds a note.
  - The wrapped boxes sit under the tree until they open them together. Mochi steals a ribbon.
- [ ] **D5. Festival plumbing:**
  - a `Festival` model with season and window;
  - posters and decorations;
  - an Our Story entry per festival per year;
  - a first, "Every festival".
  
  Also extend the Diwali and Holi dates past 2030 (the table ends there).

## E. Garden to kitchen (from Stardew's farming loop)

**Today:**
- `GardenPlots` has 3 plots and 5 flowers, picked at random. A plot blooms after 3 days of watering, and nothing wilts.
- `CookingGame` has 5 recipes, all open from the start.
- Harvests don't feed cooking.

- [x] **E1. Choose the seed,** with seasonal crops added: strawberries and peas in spring, tomatoes and basil in summer, pumpkins and apples in autumn. In winter, herbs grow in **sunroom pots**, so the sunroom becomes the winter greenhouse. Flowers stay.
- [x] **E2. A plot each and one shared.**
  - Bean's plot, Sprout's plot, and a shared plot that wants both of them that day. A "who's watering?" tap on your own character shows "Bean watered. Waiting for Sprout".
  - It's a small reason for both to come back, not a duty: nothing wilts.
- [x] **E3. A pantry.** Harvests go to an ingredient pantry. New recipes need them (tomato soup, strawberry pancakes, pumpkin pie, herb tea). The 5 starting recipes stay free and open.
- [x] **E4. Sharing the meal.** After cooking, the dish is served and the couple sits at the kitchen table to eat together. One "dinner talk" question appears, from the Daily Moments deck. That's the payoff of the loop.
- [ ] **E5. Recipes by mail.** Harvesting a new crop brings its recipe card in the next morning's mail (section G).

## F. Fishing by season, weather and time (from Stardew's fishing)

**Today:** `FishingGame` rolls MINNOW, CARP, SEASHELL, OLD_BOOT, BOTTLE and GOLDEN_FISH by weight only. Grandpa Bao's own catches (`PierCatch`) are just for show.

- [x] **F1. What's biting depends on the moment:**
  - a moon jelly and a glow squid at night;
  - a rain trout in the rain;
  - an ice cod in winter;
  - a blossom koi in spring;
  - a golden fish most likely at sunset.
  
  The common catches stay everywhere.
- [x] **F2. Bao teaches.** The first "Fish together" starts a three-line lesson from Bao and one practice bite with a wider window. Afterwards, his letters hint at what's biting this season.
- [ ] **F3.** Bao's catches and the couple's go into one fish page in the collection book (H).

## G. Morning mail (from Stardew's mailbox)

**Today:**
- The mailbox is only on the night path and just opens `LoveNotesDialog`.
- `LoveNoteItem` has no recipient and no read state.

- [ ] **G1. A mailbox by the cottage** (meadow and home scenes), as the website shows. A little flag goes up when something is unread.
- [ ] **G2. Love notes get a recipient and a read state.** A note "for Sprout" shows as a sealed envelope until "I'm Sprout, open it". The sealed letters from A2 and C4 live here too.
- [ ] **G3. At most one small letter a day, from a catalog:**
  - Mochi, in paw prints, about what Mochi got up to;
  - Bao, with fishing tips and what's biting;
  - Leo, with the cafe's special;
  - recipe cards (E5), festival invitations (D) and birthday hints (A2).
  
  It never uses streak language, and missing days just means fewer letters waiting.
- [ ] **G4.** Kept letters get their own page in Our Story.

## H. A collection book (from Stardew's collections)

**Today:**
- The keepsake box counts discoveries (6 kinds), bouquets, dishes and catches.
- The only "N of M" displays are the recipe book, the firsts and the shelf. There's no grid of everything.

- [ ] **H1. Collection pages:**
  - discoveries;
  - fish and sea things (F);
  - crops and blooms (E);
  - dishes;
  - festival keepsakes (D);
  - gifts given.
  
  Each page reads "found 12 of 30". Unfound items are silhouettes with a gentle hint.
- [ ] **H2.** Each item remembers when and where it was first found, and by whom: "Sprout found this first, on the pier, in the rain". That's a little shared history, not a score.
- [ ] **H3.** Seasonal items only appear in their season, which is a reason to visit all year.

## I. Characters with routines and likes (from Stardew's villagers)

Lighter than Stardew: no friendship meters.

- [ ] **I1. Daily routines:**
  - **Leo:** the cafe sign reads Open from 7 to 21. After hours he wipes the counter or reads, and the couple can still sit.
  - **Bao:** fishes from dawn and naps at night (this exists); tea at 4 pm.
  - **Pip:** most daring around noon.
- [ ] **I2. Likes:**
  - Each has one or two favourite gifts from the keepsake box: Bao loves tea and the old boot joke; Leo, the harvest pie; Pip, any fish.
  - Giving one gets a unique line, then a thank-you letter in the next morning's mail (G).

## J. What the website promises (www.tinyus.online) and what the app does

| The website says | In the app today | Plan |
|---|---|---|
| Love notes "wait in the mailbox by the cottage until they are opened" | The mailbox is only on the night path, and notes have no read state | G1, G2 |
| "Leave secret letters for your person to find" | Notes have an author but no recipient, and are never sealed | G2, A2, C4 |
| Daily Moments: "one gentle prompt a day" | 10 prompts, so they repeat every 10 days | Grow the deck to 120 or more, by season (do with B4) |
| Tiny Date Adventures: "little ideas for stepping outside together" | 7 activities | 30 or more, some seasonal, tied to festivals (D) |
| "Small milestones that unlock cozy rewards" | 31 firsts, 8 with rewards | More rewards come from A4, D and H |
| Mochi asleep in the loft at night; the garden says "welcome back" | Both exist (`drawLoftSleepingCat`, `GardenGrowth.welcomeBackMessage`) | Nothing to do |
| "13 Little scenes" | 13 `SceneType`s | Matches |

These are on the site and legal-pages side, not the app:
- The privacy policy covers Android only, while the site says "Coming soon to Android and iPhone".
- The terms cap liability at "in-app purchases" while saying there are none.
- The contact email is still a placeholder.

## Order (suggested)

1. **A, the birthday surprise** (mandatory), with **G2** (sealed letters and read state), which it needs.
2. **B, Tiny Games simplified**, and the website parity items in J (the mailbox by the cottage, more prompts and dates).
3. **C, couple life:** the Dinner Decider, Thank-You Jar and "Open when..." letters first; the Make-Up Bench and Phones Down next.
4. **E, garden to kitchen**, then **F, seasonal fishing**, then **H, the collection book**.
5. **D, the festivals.** They build on E, H and G.
6. **G3 and I,** the daily mail and the characters' routines and likes.

**Who does what** (as before; to agree):
- FEATURES: anything in the world (cinematics, the routine, held items, props, scenes).
- GROWTH: progress, collections, strings, dialogs and firsts.
- IOS: keeps shared UI in `commonMain`.

## The twist in each section

| Section | The twist |
|---|---|
| A. Birthday | The partner's sealed letter is hidden in the gift box; the party starts in the dark. *(Done.)* |
| B. Tiny Games | Our Story Quiz asks about the couple's own life; a miss gets "Opposites attract!". *(Done.)* |
| C. Couple life | The Dinner Decider spins only what survived both secret vetoes. The full Thank-You Jar is opened and one old thank-you is read aloud. "Open when..." letters choose their own moment. |
| D. Festivals | Each partner secretly picks the other's flower crown. Lantern wishes come back a year later ("Last summer you wished..."). Gifts are chosen in secret and unwrapped together. (No Harvest Fair: dropped by the user.) |
| E. Garden to kitchen (GROWTH) | *Merged as #31. The user chose to keep it as it is, with no twists added.* |
| F. Fishing (GROWTH) | Something lovely for her: a message in a bottle (a sealed note dropped at the pier comes up on the other's line on a later day), and little treasures he sometimes reels up for her (a pearl, a heart-shaped shell, a sea-glass heart), kept on the shelf. |
| G. Mail | *Dropped by the user (2026-10-07).* |
| H. Collection book | Each item says who found it first, where and when. A few hidden entries only appear when found ("???"). |
| I. Characters | Each has a small secret that a gift unlocks: Bao's old love letter, Leo's sketchbook, where Pip hides his treasure. |

## Build checklist

This is the working list; it is ticked as things land. The screens IOS moved to `commonMain` (`SettingsBottomSheet`, `MainScreen`, `OurStoryDialog`, `WardrobeDialog`, `ProgressStore`) landed in #24 and #25, so nothing is blocked any more.

### A. Birthday surprise (branch `feature/plan09-a-birthday`)
- [x] A-1. A `SealedLetter` model and store in shared data:
  - fields: id, recipient (BOY/GIRL), kind (BIRTHDAY / OPEN_WHEN), occasion text, body, author, createdAt, opensOn (a date, for birthdays) and openedAt;
  - JSON in `tiny_us_prefs`, so backups carry it;
  - not shown in Love Notes or Our Story until opened.
- [x] A-2. `Birthdays` logic, pure and tested:
  - the next birthday for each partner, and days until it;
  - whether today is a birthday, or within the 3-day "belated" window if the app wasn't opened on the day;
  - both on the same day; 29 February; the age only when a birth year is known;
  - whether the letter hint should show (14 days before);
  - the party already held this year.
- [x] A-3. `HeldItem.CAKE`, with art: carried and at the chest, candles lit or blown out.
- [x] A-4. Party hat art: a wardrobe accessory (index 10). *Mochi wears a pink and yellow party ruff in the collar slot instead of a hat, since her head moves with every pose.*
- [x] A-5. The surprise cinematic in `SceneEngine` (a `BirthdaySurprise` state machine), started from `greetSpecialDay` on a birthday, so `MainScreen` isn't touched:
  - DARK: the room is dim, and the caption "Hmm, why is it so dark?";
  - LIGHTS: a tap turns the lamp on;
  - SURPRISE: confetti and a name banner, the partner holds the cake, Mochi in a hat;
  - CANDLES: tap each candle to blow it out;
  - WISH: an optional wish;
  - GIFT: the box wobbles, and a tap opens the sealed letter;
  - then a day-long party mode.
- [x] A-6. Party mode for the day:
  - balloons in every scene (exists), a name banner, the birthday person in the party hat;
  - a "Happy birthday" line when either character is tapped;
  - a birthday frame on polaroids. *Done as a birthday title on the card ("Happy birthday, Sprout"); the photo already shows the banner and hats.*
- [x] A-7. Overlays in shared UI, called from the engine's state:
  - the wish field (optional; saved privately);
  - the letter reader for the sealed letter.
- [x] A-8. Progress:
  - the first, "A birthday surprise", which unlocks the party hat;
  - a birthday entry in Our Story (`StoryTimeline`) with the year, the wish (private) and the letter.
- [x] A-9. Onboarding gets an optional "Your birthdays" step with date pickers (month and day, year optional) and a skip. `OnboardingDialog` is already common.
- [x] A-10. Settings > Our World: date pickers replace the free-text fields, and old values migrate as they are. Plus two opt-in toggles: "Birthday letter reminders" and "Morning surprise reminder".
- [x] A-11. The letter hint, 14 days before:
  - a line on the special calendar and a flag on the mailbox: "A birthday is coming. Want to leave a sealed letter?";
  - the writer dialog for a sealed letter;
  - *Shown in Settings > Our World and in the special calendar. The mailbox flag comes with G-1.*
- [x] A-12. The morning reminder on the day (Android, opt-in, through `TinyCareScheduler`'s alarm path, which survives a reboot): "Something is waiting for you in Tiny Us today." It never names whose birthday it is.
- [x] A-13. Tests:
  - the date logic;
  - the letter stays hidden until the day;
  - the surprise runs through every state, and a tap at each step works;
  - the party is held once a year;
  - backup includes the letters.

  Renders: a filmstrip of the surprise, and the cake and hat sprites.

### B. Simpler Tiny Games (branch `feature/plan09-b-tiny-games`)
- [x] B-1. A shared `TinyGames` model: three games (THIS_OR_THAT, GUESS_ME, STORY_QUIZ), rounds of 5, pass-the-phone turns, a reveal, the best round kept.
- [x] B-2. Question decks in strings: 60 or more each for This or That and Guess Me, drawn without repeats.
- [x] B-3. A Story Quiz generator from the couple's own data (polaroids, dishes, anniversary month, discoveries, the first fish). It's hidden until it can make 5 questions.
- [x] B-4. One dialog in shared UI: pick a game, then each turn ("Bean, pick", hidden, "Sprout, pick"), then the reveal. It replaces `TwoPersonMiniGameDialog`.
- [x] B-5. Reactions in the world: a match makes both jump with hearts, a miss gets a shrug and "Opposites attract". The end of a round shows "You matched N of 5".
- [x] B-6. Started from the game board prop, and from Settings. *Both already opened the games; they now open Tiny Games.*
- [x] B-7. Website parity (J): the Daily Moments deck grows from 10 to 120 or more, by season; Date Adventures grow from 7 to 30 or more. *130 prompts (70 everyday, 15 for each season, one day in three) and 32 adventures; saved lists get the new ones.*
- [x] B-8. Tests: turns hide answers, the quiz is never empty, no repeats within a round. Renders of the dialog.

### C. Couple life (branch `feature/plan09-c-couple-life`)
- [x] C-1. Dinner Decider:
  - a model with categories (eat, watch, do), 8 options, each partner's custom options, and secret vetoes (2 each), then a spin;
  - the dialog;
  - the wheel and its result in the kitchen; *(the wheel spins in the dialog, and the couple say the pick out loud in the world)*
  - "Cook it" leads into the cooking game.
- [x] C-2. Thank-You Jar:
  - a model (chips plus a line of text, who wrote it, when);
  - the jar on the kitchen shelf, filling with pebbles;
  - a full jar of 20 becomes a keepsake and Mochi celebrates;
  - a reading on a rainy day. *(Once a day, in the kitchen, when it rains.)*
- [x] C-3. "Open when..." letters, on the sealed-letter store from A-1:
  - occasions plus a custom one;
  - they wait, sealed, in an "Open when..." list in Love Notes until their reader opens one (G's mailbox is dropped).
- [x] C-4. Make-Up Bench:
  - the bench scene, with a cloud and a rainbow;
  - both partners write privately, then both are revealed;
  - a choice of hug, tea or "talk later" (a gentle reminder);
  - saved only if they choose to keep it.
- [x] C-5. Phones Down:
  - a together timer of 15, 30 or 60 minutes, showing the night-light sofa view;
  - a garden bloom and an Our Story line at the end;
  - leaving early is fine.
- [x] C-6. Opt-in reminders for anniversaries and month-iversaries. *(Shared `care/DateMornings.kt` and `CoupleMornings`, merged with the birthday morning; toggles in Settings > Our World. Android done; iOS switches over after this lands.)*
- [x] C-7. Tests and renders for each. *(`CoupleLifeTest`, `CoupleLifeEngineTest`, `CoupleLifePreviewTest` (`COUPLE_LIFE_PREVIEW_DIR`), and `SCENE_PREVIEW_COUPLE=jar|bench|rainbow`.)*

### D. Festivals (branch `feature/plan09-d-festivals`)
- [x] D-1. A `Festival` model: one per season, a 3-day window in mid-season (hemisphere-aware through `SeasonalWeather`), a poster 2 days before, and years recorded. *(14th to 16th of the season's middle month. Also a note on each festival's first morning at 9:00, always on (the user didn't want an off switch), through `CoupleMornings`.)*
- [x] D-2. Blossom Picnic: secret flower picks, then the crown reveal, then a polaroid. *(The crowns are worn all day; the screen suggests a polaroid.)*
- [x] D-3. Lantern Night: private wishes that come back next year, and the lantern release at the pier.
- ~~D-4. Harvest Fair~~ *Dropped by the user (2026-10-07); autumn has no festival, and "Every festival" means the other three.*
- [x] D-5. Gift Exchange: each picks a gift from the keepsakes and adds a note; the boxes sit under the tree until opened together.
- [x] D-6. Decorations, Our Story entries, the "Every festival" first, and Diwali and Holi dates past 2030. *(Diwali 2031-2035 from drikpanchang, Holi from vedpanchang; plus a "first festival" first.)*
- [x] D-7. Tests and renders. *(`FestivalsTest`, `FestivalNotesTest`, `FestivalEngineTest`, `FestivalPreviewTest` (`FESTIVAL_PREVIEW_DIR`), `SCENE_PREVIEW_FESTIVAL=...`.)*

### E. Garden to kitchen (GROWTH, merged as #31)
- [x] E-1. A seed picker, seasonal crops, and the sunroom pots as the winter greenhouse. *(`Seeds` in `GardenPlots.kt`: flowers outside winter; strawberries and peas in spring, tomatoes and basil in summer, pumpkins and apples in autumn; nothing in the meadow in winter; mint and basil in the two sunroom pots all year. The picker is `SeedPickerCard` in `ui/CozyCards.kt`.)*
- [x] E-2. Plots for Bean, for Sprout and a shared one, with "who's watering?". *(Plot 0 is his (blue flag), 1 is shared (gold heart), 2 is hers (pink flag). The shared plot grows once both have watered that day; `WhoWatersCard` asks who it is, and the caption says "Bean watered. Waiting for Sprout".)*
- [x] E-3. A pantry, plus recipes that use crops (the 5 starting recipes stay free). *(Harvests go to `ProgressState.pantry`; six garden recipes: tomato soup, strawberry pancakes, pumpkin pie, mint tea, apple crumble, pea soup. "Cook together" opens `RecipePickerCard`; a locked recipe says what it needs. Firsts: "Our first harvest", "From our garden". The chef's apron still means the five starters.)*
- [x] E-4. Sharing the meal at the table, with a dinner-talk question. *(After a dish is served they walk to the table and sit; a Daily Moments question follows as the caption, "Over dinner: ...".)*
- [x] E-5. Tests and renders. *(`GardenKitchenTest`, `GardenKitchenEngineTest`; `SCENE_PREVIEW_COZY=crops`, `STORE_SHOTS_COZY=picker|seeds|who`.)* E5 (recipes by mail) was dropped with G.

### F. Seasonal fishing (GROWTH)
- [x] F-1. Catches by season, weather and time; new fish art. *(`FishingConditions` and `FishingCatch.weightIn` in `FishingGame.kt`: a moon jelly and a glowing squid at night, a rain trout in rain, an ice cod in winter, a blossom koi in spring, and the golden fish three times as likely at sunset. The common catches stay everywhere. Art in `CozySprites.CATCHES`.)*
- [x] F-2. Bao's lesson and practice bite. *(The first "Fish together" plays three lines from Bao, then a practice cast: a bite after 2 s with a 2.8 s window. Later casts sometimes start with Bao's hint for the moment. His letters were dropped with G.)*
- [x] F-5. Something lovely for her (the user's twist): a message in a bottle (either of them writes a short note at the pier and tosses it; from the next day the next bite brings it up, sealed, for the other to open; `LetterKind.BOTTLE` in `SealedLetters.kt`, `ui/BottleDialogs.kt`), and little treasures he sometimes reels up and gives her on the spot (a pearl, a heart-shaped shell, a sea-glass heart), which go on the shelf.
- [ ] F-3. Bao's catches go into the fish page.
- [x] F-4. Tests. *(`CozyGamesTest`: each catch in its moment, the weights, the practice bite; `CozyGamesEngineTest`: the lesson, the practice bite, the conditions and hints.)*

### G. Morning mail: dropped
The user decided on 2026-10-07 that G isn't needed. That also drops:
- E-5 (recipes by mail);
- Bao's letters in F-2;
- the thank-you letter in I-2 (a thank-you line in the scene instead).

The sealed letters from A and C keep their own homes:
- the birthday gift box (A);
- the "Open when..." list (C-3);
- the bottle on the line (F, as `LetterKind.BOTTLE`).

### H. Collection book
- [ ] H-1. Collection pages with "found N of M", silhouettes and hints.
- [ ] H-2. When, where and who found each item first.
- [ ] H-3. Seasonal items.
- [ ] H-4. Tests.

### I. Characters' routines and likes
- [ ] I-1. Routines for Leo, Bao and Pip by the clock.
- [ ] I-2. Favourite gifts, a unique line for each, and their small secret unlocked (a thank-you line in the scene; G is dropped).
- [ ] I-3. Tests.

## Decisions for the user

1. Is the order above right, with the birthday surprise first and Tiny Games second?
2. Birthday letter hints: shown to whoever opens the app, or only after tapping "I'm Bean" or "I'm Sprout"? Honour system either way, since it's one phone.
3. The Make-Up Bench: is "We need a moment" the right wording? Should it be in the heart menu or only in Settings?
4. Festivals: in the middle of each real season (proposed), or fixed calendar dates?
5. Notifications (A5, C6): opt-in toggles in Settings > Our World. Fine?
6. Characters' likes and gifts (I2): include them, or skip to keep the scope small?
