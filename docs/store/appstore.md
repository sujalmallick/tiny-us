# App Store listing: Tiny Us

Draft for App Store Connect (**Apps > Tiny Us > App Store** tab, and **App Information**). Limits are Apple's; counts include spaces. The Google Play text is in `listing.md`.

## App name (30 max)

```
Tiny Us: A Cozy Couple World
```

28 characters. The same name as on Google Play.

## Subtitle (30 max)

```
A cozy pixel world for two
```

26 characters. It shows under the name in search results.

## Promotional text (170 max)

Can be changed at any time without a new build.

```
A little pixel home for the two of you. Name your tiny couple, watch them live their days, and keep your memories. No ads, no account, fully offline.
```

149 characters.

## Keywords (100 max)

Comma-separated, with no spaces after the commas. Words already in the name or subtitle (cozy, couple, world, pixel, two) are indexed anyway, so they are left out.

```
relationship,boyfriend,girlfriend,love,anniversary,pixel art,days together,cute,pet,widget,memories
```

99 characters.

## Description (4000 max)

```
Tiny Us is a little pixel-art world made for the two of you.

Name your tiny couple, pick their looks, and watch them live their days together: wandering the meadow, sharing dumplings at a street food stall, cuddling in a midnight loft over the city lights, and roasting marshmallows under the stars. When you're not tapping, they still have a life of their own. They chat, play with Mochi the cat, find little surprises and get cozy when it rains.

THIRTEEN COZY SCENES
- A Flower For You, in the meadow by your cottage
- Under Our Tree
- Kitchen Secret (someone keeps stealing bites)
- Couch Snooze
- Lantern Stroll
- Just Looking at You
- Street Food Date
- Evening Ride on your scooter
- Midnight Loft
- Cozy Rainy Cafe
- Cottage Sunroom
- Starry Campfire
- Seaside Pier, with one very bold seagull

A WORLD THAT FOLLOWS YOUR DAY
- Morning, afternoon, sunset and night follow your real clock
- Seasons with sunny days, rain, cherry blossoms, autumn leaves and snow
- Little festivals through the year, and a birthday surprise for each of you
- A visitor who comes by on Fridays

THINGS TO DO TOGETHER
- Grow a garden and cook what it gives you
- Go fishing, with different catches each season
- Tiny Games for the two of you
- A collection book of everything you find

MAKE IT YOURS
- Wardrobe and looks for both of you
- Decorate your cottage rooms
- Cozy music for each season

KEEP YOUR MEMORIES
- Tiny Moments: tap the heart to print a Polaroid of the scene
- Love notes, a dream journal and your story together
- A calendar for your anniversary and birthdays, with a days-together counter
- A home-screen widget with your couple in their scene
- Gentle reminders, if you'd like them

PRIVATE BY DESIGN
- Works fully offline: no internet access at all
- No ads, no account, no tracking
- Everything stays on your device
- Optional app lock with a PIN, Face ID or Touch ID
- Encrypted backup to a file you choose, which also opens on Android

Tiny Us is a small, quiet place to come back to, for two.
```

## What's New (version 1.0)

```
The first version of Tiny Us on iPhone and iPad.
```

## App Information

- **Category:** primary **Games > Casual**, secondary **Lifestyle**. Keep it the same as Google Play.
- **Age rating:** answer **None** to every content question (no violence, no mature themes, no gambling, no web access, no user-generated content shared with others). The result should be **4+**.
- **Copyright:** `2026 <your name>`.
- **Support URL (required):** a page with a way to reach you, such as a contact page on the Tiny Us website.
- **Privacy policy URL (required):** where `docs/PRIVACY_POLICY.md` is hosted (the same page as for Google Play).

## App Privacy ("nutrition label")

Answer **No, we do not collect data from this app**. Tiny Us has no network access, so nothing leaves the device. The label then shows **Data Not Collected**.

## Export compliance (encryption)

The app sets `ITSAppUsesNonExemptEncryption` to `false` in `iosApp/Info.plist`, so App Store Connect stops asking about encryption on each upload. The only encryption is the backup file, which uses Apple's own CryptoKit (AES-GCM) and CommonCrypto (PBKDF2), and that is exempt.

## Screenshots

Apple needs one size per device family. Larger sizes are scaled down for older devices:

- **iPhone 6.9-inch:** 1290 x 2796 portrait.
- **iPad 13-inch:** 2048 x 2732 portrait.

They are drawn from the app's own shared UI on the computer, like the Google Play ones:

```
STORE_SHOTS_DIR=docs/store/screenshots ./gradlew :app:testDebugUnitTest --tests "com.example.ui.StoreScreenshotTest.appStore*" --rerun
```

This writes `docs/store/screenshots/appstore_iphone_6_9/` and `appstore_ipad_13/`, eight shots each. Upload 3 to 10 per family, in order.

## Before submitting

- [ ] Every feature named above is in the uploaded build (check the widget, the reminders and the app lock on the TestFlight build).
- [ ] The support and privacy policy URLs open.
- [ ] Screenshots uploaded for iPhone and iPad.
