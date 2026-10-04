# Privacy Policy for Tiny Us

**Effective Date:** October 5, 2026  
**Last Updated:** October 5, 2026

---

## 1. Overview and Core Privacy Commitment

**Tiny Us** ("we", "our", or "the app") is designed from the ground up as a **100% offline-first, private personal space** for couples. Our core belief is that your intimate memories, daily notes, anniversaries, and couple interactions belong solely to you and your partner.

- **We do not run tracking servers, telemetry services, or analytics.**
- **We do not require user accounts, emails, passwords, or logins.**
- **We do not upload, transmit, or synchronize your personal notes, photos, or data to any remote cloud servers.**

---

## 2. Information Handled Locally On Your Device

All content created, customized, or logged within Tiny Us is stored strictly on your local device's internal storage (`/data/data/com.tinyus.app/`):

1. **Couple Customization Data:** Names/nicknames, anniversary date, birthdays, and customized secret notes entered during onboarding or in settings.
2. **Love Notes & Keepsake Journal:** Offline messages written to your partner and milestone memories.
3. **Polaroid Memories:** Pixel-art snapshot cards generated via the in-game camera feature, stored locally in private app storage.
4. **App Preferences & Progress:** Garden growth stage, pet interactions, selected outfits and hairstyles, ambient sound toggles, and notification preferences.
5. **Privacy Lock (optional):** whether the lock is on, a salted hash of your PIN, and whether fingerprint or face unlock is allowed. This is never included in backups.

**None of this data is ever transmitted to us or any third party.** You retain full ownership and control over your data at all times.

---

## 3. No Internet Access, No Purchases

Tiny Us does not use the internet at all. The app does not request Android's `INTERNET` permission, so it cannot send or receive anything over a network.

- There are no in-app purchases, no ads, no analytics, no crash reporting and no telemetry.
- Nothing you create in the app ever leaves your device unless you move it yourself (see section 5).

---

## 4. Device Permissions

Tiny Us requests a minimal set of Android system permissions, each serving a direct local function:

- **Notifications (`POST_NOTIFICATIONS` - Android 13+):** Used exclusively to deliver optional, offline "Tiny Care" check-in reminders (such as water reminders, sleep prompts, or sweet check-ins) scheduled by you.
- **Run at Startup (`RECEIVE_BOOT_COMPLETED`):** Used solely to restore your local inexact alarm schedules if your phone is restarted.
- **Photos / Storage (`WRITE_EXTERNAL_STORAGE` - Android 9 and older only):** Used strictly when you explicitly tap "Save to Photos" to export a Polaroid snapshot to your device's picture gallery. On modern Android versions, this uses standard system photo saving without requiring broad storage permissions.
- **Fingerprint / Face unlock (`USE_BIOMETRIC`):** Used only if you turn on the optional privacy lock and choose to unlock with your fingerprint or face. Android checks your fingerprint or face itself; Tiny Us never sees or stores it. The lock's PIN is stored only as a salted, one-way hash on your device.

---

## 5. Data Backup, Export, and Transfer

Because Tiny Us operates without cloud databases or user accounts:
- **Backup & Restore (in Settings)** saves your data into a single file encrypted with a password you choose (AES-256-GCM, key derived with PBKDF2). The file is written through Android's system file picker to a location you choose; Tiny Us never uploads it and cannot open it without your password.
- **Android device backup:** if you have Android backup turned on, Android itself may copy the app's core settings and journal text (not photos, and not the privacy-lock settings) to your Google account backup. On Android 12 and later this only happens when Android can end-to-end encrypt it with your screen lock. This is a service of your phone's operating system; we never receive or can access it.
- **Phone-to-phone transfer:** when you set up a new phone with Android's direct transfer, your Tiny Us data (including photos) can move with it.
- You have complete control over transferring, sharing, or deleting backup files.
- If you uninstall the application without a backup, all locally stored data is permanently deleted by the Android operating system.

---

## 6. Children's Privacy

Tiny Us is intended for general audiences, specifically couples and adults aged 13 and older (or 16+ depending on jurisdiction). We do not knowingly target, market to, or collect information from children under the age of 13.

---

## 7. Data Retention and Deletion

Because all personal data resides entirely on your device:
- **Instant Deletion:** You can delete all data at any moment by navigating to your Android device's **Settings > Apps > Tiny Us > Storage > Clear Data**, or by simply uninstalling the application.
- We hold no backups, server logs, or copies of your data, so there is no residual data retained on any remote server.

---

## 8. Changes to This Privacy Policy

We may update this policy to reflect new features or changes to Google Play policies. If Tiny Us ever starts using the internet, this policy will say so first. Any changes will be posted in-app or in public release notes with an updated effective date.

---

## 9. Contact Us

If you have any questions or feedback regarding privacy in Tiny Us, please contact:
- **Support / Developer Email:** [CONTACT EMAIL - to be filled in before publishing]
