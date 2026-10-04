# Google Play Store Compliance Checklist for Tiny Us

**Target App:** Tiny Us (`com.tinyus.app`)  
**Last checked against the code:** October 5, 2026  
**Disclaimer:** This checklist is for developer planning and readiness. It does not constitute formal legal counsel.

Tiny Us is free, fully offline, and has no in-app purchases, ads or analytics. If any of that changes, redo sections 1, 3 and the Data safety form (`PLAY_CONSOLE_DATA_SAFETY.md`) first.

---

## 1. Content Rating & IARC Questionnaire

When filling out the International Age Rating Coalition (IARC) questionnaire in Play Console (**Policy > Content rating**):

- [ ] **Category Selection:** Select **"Game"** if you list Tiny Us under Games (e.g. Casual or Simulation), or **"All other app types"** if you list it as an app (e.g. Lifestyle). Pick the same as the store category.
- [ ] **Violence:** Select **"No"** (Zero violent depictions or combat).
- [ ] **Sexuality / Romance:**
  - *Does the app contain nudity or sexual content?* **No**.
  - *Does the app depict romantic affection?* **Yes** (Pixel-art hand-holding, hugging, sweet cheek kisses, sharing food). This does **not** trigger mature or 18+ ratings under IARC/ESRB/PEGI guidelines; it qualifies under general family/everyone guidelines (PEGI 3, ESRB Everyone, IARC 3+).
- [ ] **Language:** Select **"No"** (Completely clean dialogue; no profanity or vulgarity).
- [ ] **Controlled Substances:** Select **"No"** (No depictions of alcohol, tobacco, or drugs).
- [ ] **User-to-User Interaction:** Select **"No"** (Zero public chat rooms, zero multiplayer matchmaking, zero social sharing feeds).
- [ ] **Physical Location Sharing:** Select **"No"** (Zero GPS or location sharing).
- [ ] **Digital Goods Purchases:** Select **"No"** (no in-app purchases).

---

## 2. App Content Declarations (Policy > App content)

- [ ] **Ads:** "No, my app does not contain ads".
- [ ] **Target audience:** 13 and older, matching section 6 of the privacy policy. Don't select under-13 age groups (that would bring in the Families policy).
- [ ] **Data safety:** "No data collected or shared" (see `PLAY_CONSOLE_DATA_SAFETY.md`).
- [ ] **Government apps, financial features, health:** No.

---

## 3. Permissions Justifications & Declaration

The merged manifest asks for exactly these permissions (checked October 5, 2026):

- [ ] **`POST_NOTIFICATIONS` (Android 13+):**
  - Declared in AndroidManifest.xml.
  - Requested only when the user turns on "Tiny Care Check-ins" in Settings, not on first launch.
- [ ] **`RECEIVE_BOOT_COMPLETED`:** restores the Tiny Care reminder after a restart.
- [ ] **Inexact Alarms Only (Zero `SCHEDULE_EXACT_ALARM`):**
  - Tiny Care check-ins use battery-friendly, inexact `setAndAllowWhileIdle()` (`TinyCareScheduler`).
  - The app doesn't request `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM`, so no exact-alarm declaration is needed.
- [ ] **`WRITE_EXTERNAL_STORAGE` (Android 9 and older only, `maxSdkVersion="28"`):** saving a polaroid to the gallery.
- [ ] **`USE_BIOMETRIC` / `USE_FINGERPRINT`:** added by the AndroidX biometric library for the optional privacy lock. These are normal permissions with no Play declaration.
- [ ] **No `INTERNET`:** the app has no network access at all.
- [ ] **No Location or Sensitive Permissions:** no `ACCESS_FINE_LOCATION`, `READ_CONTACTS`, `CAMERA` or `RECORD_AUDIO`.
- Re-check after adding any library: `app/build/intermediates/merged_manifests/release/*/AndroidManifest.xml` shows the final list.

---

## 4. Play Console Developer Setup (Prerequisites Outside Code)

- [ ] **Google Play Developer Account:** registration and identity verification completed.
- [ ] **Upload key and app bundle:** see `docs/store/RELEASE_SIGNING.md`. Keep Play App Signing on.
- [ ] **Testing before production:** new personal developer accounts must run a closed test (at the time of writing, at least 12 testers opted in for 14 days in a row) before they can apply for production access. Check the current rule in the Play Console.
- [ ] **Store listing:** text in `docs/store/listing.md`, screenshots, feature graphic and icon (plan 06, section E).

---

## 5. Privacy Policy Hosting

- [ ] **Public Privacy Policy URL:**
  - Google Play requires a public, live URL for your privacy policy before you can publish.
  - Fill in the contact email in `docs/PRIVACY_POLICY.md`, then host it on GitHub Pages, a project website, or a public gist.
  - Enter the live URL in Play Console under **Policy > App content > Privacy policy**.
