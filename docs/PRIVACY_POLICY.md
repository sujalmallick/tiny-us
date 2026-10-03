# Privacy Policy for Tiny Us

**Effective Date:** October 1, 2026  
**Last Updated:** October 1, 2026  
**Status:** DRAFT (Subject to independent legal review before store publication)

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

**None of this data is ever transmitted to us or any third party.** You retain full ownership and control over your data at all times.

---

## 3. Network Access & In-App Purchases

Tiny Us does not request generic network access for regular gameplay. The sole exception is **Google Play Billing**, which is utilized exclusively when you choose to make an optional one-time digital purchase (such as an optional cosmetic expansion pack or developer tip):

- **Transaction Processing:** In-app purchases are handled directly through Google Play Services and Google LLC. When completing a transaction, Google processes payment details (such as credit card information and billing addresses) under the [Google Play Terms of Service](https://play.google.com/intl/en_us/about/play-terms/) and [Google Privacy Policy](https://policies.google.com/privacy).
- **Strict Scope of INTERNET Permission:** The newly required `android.permission.INTERNET` permission is invoked only during an active purchase or restore-purchases transaction, and is never used for anything else or checked in the background. The app contains zero background network syncs, zero remote analytics, and zero telemetry pings.
- **What Tiny Us Receives:** The app only receives an anonymous, cryptographically signed purchase token and product identifier from Google Play to confirm your entitlement. We never see, receive, or store your credit card or financial details.
- **Offline Entitlement Caching:** Once a purchase is completed and verified, your entitlement is cached locally on your device. You do not need an active internet connection to use previously purchased content.

---

## 4. Device Permissions

Tiny Us requests a minimal set of Android system permissions, each serving a direct local function:

- **Notifications (`POST_NOTIFICATIONS` - Android 13+):** Used exclusively to deliver optional, offline "Tiny Care" check-in reminders (such as water reminders, sleep prompts, or sweet check-ins) scheduled by you.
- **Run at Startup (`RECEIVE_BOOT_COMPLETED`):** Used solely to restore your local inexact alarm schedules if your phone is restarted.
- **Photos / Storage (`WRITE_EXTERNAL_STORAGE` - Android 9 and older only):** Used strictly when you explicitly tap "Save to Photos" to export a Polaroid snapshot to your device's picture gallery. On modern Android versions, this uses standard system photo saving without requiring broad storage permissions.

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

We may update this policy periodically to reflect new offline features or updates to Google Play policies. Any changes will be posted in-app or in public release notes with an updated effective date.

---

## 9. Contact Us

If you have any questions or feedback regarding privacy in Tiny Us, please contact:
- **Support / Developer Email:** support@tinyus.app *(Replace with verified developer email)*
