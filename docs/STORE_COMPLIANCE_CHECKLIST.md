# Google Play Store Compliance Checklist for Tiny Us

**Target App:** Tiny Us (`com.tinyus.app`)  
**Document Status:** Pre-Launch Compliance Verification Guide  
**Disclaimer:** This checklist is for developer planning and readiness. It does not constitute formal legal counsel.

---

## 1. Content Rating & IARC Questionnaire

When filling out the International Age Rating Coalition (IARC) questionnaire in Play Console (**Policy > Content rating**):

- [ ] **Category Selection:** Select **"Utility, Productivity, Communication, or Other"** or **"Lifestyle / Entertainment"**.
- [ ] **Violence:** Select **"No"** (Zero violent depictions or combat).
- [ ] **Sexuality / Romance:**
  - *Does the app contain nudity or sexual content?* **No**.
  - *Does the app depict romantic affection?* **Yes** (Pixel-art hand-holding, hugging, sweet cheek kisses, sharing food). This does **not** trigger mature or 18+ ratings under IARC/ESRB/PEGI guidelines; it qualifies under general family/everyone guidelines (PEGI 3, ESRB Everyone, IARC 3+).
- [ ] **Language:** Select **"No"** (Completely clean dialogue; no profanity or vulgarity).
- [ ] **Controlled Substances:** Select **"No"** (No depictions of alcohol, tobacco, or drugs).
- [ ] **User-to-User Interaction:** Select **"No"** (Zero public chat rooms, zero multiplayer matchmaking, zero social sharing feeds).
- [ ] **Physical Location Sharing:** Select **"No"** (Zero GPS or location sharing).
- [ ] **Digital Goods Purchases:** Select **"Yes"** (The app offers optional digital in-app purchases).

---

## 2. In-App Purchase (IAP) & Billing Compliance

Google Play has strict enforcement regarding in-app purchases under the **Monetization and Ads** policy.

- [ ] **Google Play Billing Exclusivity:** All digital goods must be sold exclusively through Google Play Billing (no external links to PayPal, Stripe, or web checkouts).
- [ ] **No Gating Previously Free Core Features:** The core free experience must remain rich and complete:
  - All existing 8 scenes remain free.
  - The offline keepsake journal, love notes mailbox, and pet cat remain free.
  - Only genuine bonus packs (e.g., bonus celestial scenes, extra seasonal wardrobe sets) are gated.
- [ ] **Clear and Honest Pricing UI:**
  - The purchase modal must state the exact price (dynamically fetched from Google Play, formatted with the user's local currency symbol).
  - Explicitly label purchases as **"One-time purchase"** or **"Permanent unlock"** (no misleading subscription terminology).
  - State clearly what items are included in the pack before the user clicks buy.
- [ ] **Prominent "Restore Purchases" Option:**
  - Provide a clear, easily accessible "Restore Purchases" button in the Settings overlay or shop dialog.
  - Tapping this queries `BillingClient.queryPurchasesAsync()` and restores cached entitlements without charging the user again.
- [ ] **Refund Policy Disclosure:**
  - Do not claim "All sales are final with zero refunds." Google Play mandates that developers honor Google Play's standard 48-hour refund policy.
  - State clearly in the shop UI and Terms of Service: *"Purchases are processed by Google Play and eligible for refund under standard Google Play refund policies."*

---

## 3. Permissions Justifications & Declaration

Google Play requires declarations for specific runtime and special permissions:

- [ ] **`POST_NOTIFICATIONS` (Android 13+):**
  - Declared in AndroidManifest.xml.
  - Requested in context when the user first toggles on "Tiny Care Check-ins" or sets a reminder time, not immediately on first app boot.
- [ ] **`SCHEDULE_EXACT_ALARM`:**
  - Used strictly for user-facing timely notifications (Tiny Care water/sleep check-ins).
  - Under Google Play's exact alarm policy, apps that provide user-set reminders are an acceptable use case.
- [ ] **`INTERNET` Permission:**
  - Added automatically by the Google Play Billing Library.
  - In your Data Safety form, explicitly clarify that internet access is used solely for Google Play transaction verification, and never for app data telemetry or ad networks.
- [ ] **No Location or Sensitive Permissions:**
  - Confirm the manifest has zero requests for `ACCESS_FINE_LOCATION`, `READ_CONTACTS`, `CAMERA`, or `RECORD_AUDIO`.

---

## 4. Play Console Developer Setup (Prerequisites Outside Code)

These actions must be performed directly in your Google Play Console account before billing can be tested end-to-end:

- [ ] **Google Play Developer Account:** Account registration and developer identity verification completed.
- [ ] **Google Payments Merchant Profile:** Set up in Play Console (**Settings > Developer account > Payment settings**) to accept payments and set payout bank details.
- [ ] **Upload Initial Signed App Bundle (AAB):**
  - You cannot create in-app products in Play Console until an APK/AAB containing the `com.android.billingclient` dependency and `com.android.vending.BILLING` permission has been uploaded to at least the **Internal Testing** or **Closed Testing** track.
- [ ] **Define In-App Products (SKUs):**
  - In Play Console under **Monetization > In-app products**, define your product IDs matching your code constants (e.g., `tinyus_pack_celestial`, `tinyus_pack_wardrobe_seasonal`, `tinyus_tip_dev`).
  - Set status to **Active** with your chosen base pricing.
- [ ] **Configure License Testing Accounts:**
  - Add your test Google accounts under **Settings > License testing**.
  - Set "License test response" to `RESPOND_NORMALLY`.
  - Testers on this list can complete test purchases using test credit cards without being charged real money.

---

## 5. Privacy Policy Hosting

- [ ] **Public Privacy Policy URL:**
  - Google Play mandates a publicly accessible, live URL for your Privacy Policy before you can publish to Production.
  - Recommended: Host the drafted [PRIVACY_POLICY.md](file:///d:/workspace/Andriod%20projects/tiny-us%20-%20public/docs/PRIVACY_POLICY.md) on GitHub Pages, a project website, or a public Notion/gist link.
  - Enter the live URL in Play Console under **Policy > App content > Privacy policy**.
