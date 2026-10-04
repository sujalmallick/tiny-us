# Plan 06: Release kit, living world, widget, accessibility

Written 2026-10-04 and checked against the code on 2026-10-05 (`master` at `e9cc5f2`).

**State of the code:**
- `master` holds the pixel-art overhaul (PR #2) and FEATURES' autonomous behavior (PR #3, see `05_AUTONOMOUS_BEHAVIOR.md`). Both feature branches are deleted.
- `applicationId` `com.tinyus.app`, `versionCode` 1, `versionName` "1.0", `targetSdk` 36.
- **There is no release key.** `keystore.properties` doesn't exist, so release builds fall back to the debug key (`app/build.gradle.kts`). The 13.2 MB "release" APK can be installed for testing but not uploaded to Google Play.
- The app has no internet access and no billing: no `INTERNET` permission, no billing library. Permissions after the merge: `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `WRITE_EXTERNAL_STORAGE` (Android 9 and older), and `USE_BIOMETRIC`/`USE_FINGERPRINT` from the app-lock library.
- Store docs already exist but were written for a version with in-app purchases: `docs/PRIVACY_POLICY.md` (draft), `docs/PLAY_CONSOLE_DATA_SAFETY.md` and `docs/STORE_COMPLIANCE_CHECKLIST.md`.
- The home-screen widget (`TinyUsWidgetProvider`) shows only text: day counter, names, a status line, scene and weather. Tapping it opens the app, and it refreshes every 30 min and on `updateAllWidgets`.
- The calendar (`SpecialCalendarManager`) knows the anniversary and both birthdays, but nothing in the scenes reacts to them. There is no moon-phase code. The dream overlays draw their own crescent moons.
- No phone, and no emulator. All visual checks use JVM renders (`ScenePreviewTest`, `AutonomyFilmstripTest`, Compose views drawn to a bitmap).

**Ground rules:**
- One branch per section, cut from `master`. Merges go through a PR the user opens.
- One Gradle build at a time; message "build free" between sessions.
- Ask the user before pushing or merging. Never commit `keystore.properties`, keystores, `personal_profile.json` or the `SS/` folder.
- No emoji (`NoEmojiPolicyTest`).
- Changes to the couple's behavior go through FEATURES' autonomy system (`scene/autonomy/`); agree on them with FEATURES first.

---

## E. Play Store release kit (first: needed for the public release)

The Play Console steps themselves (developer account, upload, forms) are the user's. We prepare every file and answer.

### E0. Signing and the app bundle (blocks upload)
- [x] Step-by-step guide in `docs/store/RELEASE_SIGNING.md`; `bundleRelease` now refuses to run with the debug key.
- [ ] The user creates the upload key with `keytool` and writes `keystore.properties` (passwords stay with the user; both files are git-ignored). Back them up somewhere safe: a lost upload key means contacting Google to reset it.
- [ ] Enrol in Play App Signing when creating the app in the Play Console.
- [ ] Build the app bundle with `:app:bundleRelease` (Play takes `.aab`, not `.apk`) and confirm it's signed with the upload key.
- [ ] Decide the first version number (now `1` / "1.0").

### E1. Store screenshots
The heart button's Tiny Moment polaroids are for players and need a running app. Store screenshots need the whole screen, buttons included, at exact sizes, so they're rendered on the JVM.
- [x] A `StoreScreenshotTest` (runs only when `STORE_SHOTS_DIR` is set) that draws the full app screen (scene + HUD) at phone size 1080x2400 and tablet sizes 1600x2560 (7") and 2560x1600 (10").
- [x] 6-8 scenes and times that show the app best: meadow by day, loft at night, campfire, rainy cafe, seaside pier at sunset, a weather season, the kitchen. Let the autonomy run a few seconds first so the couple is doing something.
- [x] One Tiny Moment polaroid card (`PolaroidManager.renderPolaroidCard`) to show the heart feature.
- [ ] Optional caption band above each shot, in the pixel font. *(Not done; the user decides.)*
- [ ] The user picks the final set from `docs/store/screenshots/` (phone 1080x2400, 7" 1200x1920, 10" 1600x2560). *(The engine is run forward first and the screen drawn once: about 5 s a shot, instead of minutes.)*

### E2. Store listing text
- [x] App name and short description (80 characters at most).
- [x] Full description (4000 at most): the scenes, seasons and weather, the couple living their own little life, Tiny Moments, fully offline, no ads, no account.
- [x] Saved in `docs/store/listing.md` for the user to paste.

### E3. Graphics
- [x] Feature graphic 1024x500: a wide pixel-art banner (the couple on the meadow, title in the pixel font), rendered on the JVM.
- [x] Hi-res icon 512x512, made from the adaptive launcher icon's layers. Both in `docs/store/graphics/`, rendered by `StoreGraphicsTest`.

### E4. Bring the store docs up to date
- [x] `PRIVACY_POLICY.md`: remove the in-app purchase and `INTERNET` section; add the app lock (biometrics stay on the device); set the effective date; drop "DRAFT". **The contact email is still a placeholder for the user to fill in** (also in `TERMS_OF_SERVICE.md`).
- [x] `PLAY_CONSOLE_DATA_SAFETY.md`: no purchase history and no device identifiers. The answer becomes "no data collected or shared".
- [x] `STORE_COMPLIANCE_CHECKLIST.md`: remove the billing section; content rating "digital purchases" becomes No; list the permissions as they are now.
- [x] `TERMS_OF_SERVICE.md`: the purchases section replaced (the app is free). *Open for the user: the liability cap still refers to "in-app purchases", and the repo has no LICENSE although it's public while the terms forbid redistribution.*
- [ ] Host the privacy policy at a public URL (e.g. GitHub Pages). This is the user's step.
- **Check:** the user reviews E0-E4 and submits in the Play Console.

## F. Text and translation

- [ ] F1. Move the last ~31 built UI strings into `strings.xml` (with `tools/i18n`), including the widget's ("Day", "Mood:", "Quiet peaceful moments together.").
- [ ] F2. Pseudo-locale render check: no clipped or overlapping text in long languages.
- [ ] F3. *(User's call)* A first translation, e.g. Hindi (`values-hi`).

## G. A world that follows the real calendar

### G1. Real moon phases
- [x] Work out tonight's phase from the date (29.53-day cycle, no internet needed).
- [x] Draw it as pixel art (new, crescent, half, gibbous, full) in the outdoor night sky (`drawMilkyWayNightSky`) and the loft window (`drawWindowSkyline`). Leave the dream overlays as they are.
- [x] Unit test: known dates give the right phase. *(`MoonPhaseTest`: solar eclipses fall on new moons, lunar eclipses on full moons. `SCENE_PREVIEW_MOON=0.25` previews any phase.)*

### G2. Special days
- [ ] Anniversary and both birthdays (from `SpecialCalendarManager` and the profile): small decorations in the scene, plus a line from the couple through the autonomy system (agree with FEATURES).
- [ ] Fixed-date days: New Year, Valentine's Day, Christmas.
- [ ] *(User picks)* Festivals with changing dates, e.g. Diwali or Holi, from a table of dates for the next few years. Verify every date before adding it.
- [ ] Debug override to preview any special day; renders of each.

### G3. Small polish
- [x] The loft's night skyline: give the towers a slightly lighter outline so they keep their shape after dark.
- [ ] *(Optional)* A setting or debug toggle to speed up the sun and moon, so they can be watched moving.

## H. Home-screen widget upgrade

Already there: text info, tap to open the app, 30-minute refresh, `updateAllWidgets`.
- [ ] H1. A small pixel picture of the couple's current scene, drawn offscreen with the low-res renderer and set as the widget image.
- [ ] H2. The picture follows the time of day and the weather; call `updateAllWidgets` when the scene changes.
- [ ] H3. Check the small and large widget sizes; the text uses the pixel font where RemoteViews allows.
- [ ] H4. Renders of the widget at each size, and a test that the picture is made without errors.

## I. Accessibility

- [ ] I1. Review the 37 `contentDescription = null` across 12 UI files: label the icon buttons, leave the purely decorative ones.
- [ ] I2. Large font sizes (1.3x and 2x): render the main screens and dialogs and fix any clipping.
- [ ] I3. Re-check the 48dp touch targets and colour contrast after the pixel UI changes.
- [ ] I4. *(Optional)* Labels for the main things you can tap in a scene, so screen-reader users can find them.

---

## Order

1. **E** (release kit; E0 signing first, since nothing can be uploaded without it).
2. **G1 + G3** (moon phases, night skyline). Quick and visible.
3. **F1-F2** (last strings).
4. **G2** (special days). Needs the user's festival picks and FEATURES' agreement.
5. **H** (widget). The biggest job.
6. **I** (accessibility). Before the public release goes out.

After each section: full tests and lint, renders for the user, commit, then ask before pushing.
