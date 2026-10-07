# Tiny Us — iOS Installation Guide (From Windows)

Since Tiny Us is built with Kotlin Multiplatform (KMP), GitHub Actions compiles the shared framework and iOS app on macOS runners in the cloud, generating a ready-to-install `.ipa` package without needing a Mac on your local desk.

## Widget App Group setup

The iOS widget reads a local snapshot through the App Group `group.com.tinyus.app.shared`. For a signed device build, register that App Group with the Apple Developer team, enable it for both the app and `com.example.tinyus.widget` identifiers, and keep the matching value in both entitlement files, `TinyAppGroup.suiteName` (`iosApp/iosApp/TinyWidgetPayload.swift`) and `IosUserDefaultsStorage.APP_GROUP_SUITE` (shared Kotlin). The widget shows its built-in Tiny Us preview until the app and extension share that group.

The iOS bundle identifiers in this public project are example identifiers. Replace them with identifiers registered to your signing team before distributing a signed build.

---

## Method 1: Sideloadly (Recommended — Fastest & Easiest on Windows)

**Requirements**:
- Windows PC
- iPhone connected via USB cable
- Your free Apple ID (no paid developer account required)
- [Sideloadly for Windows](https://sideloadly.io/) (Free)
- iTunes and iCloud for Windows (installed directly from Apple, NOT the Microsoft Store)

### Step-by-Step Instructions:

1. **Download the IPA**:
   - Go to your GitHub repository -> **Actions** tab.
   - Click on the latest **Tiny Us iOS Build & IPA Package** run.
   - Under **Artifacts** at the bottom of the page, download `TinyUs-iOS-unsigned.zip` and extract `TinyUs-iOS-unsigned.ipa`.

2. **Open Sideloadly**:
   - Launch Sideloadly on your Windows PC.
   - Plug your iPhone into your PC with a USB cable. Tap **"Trust This Computer"** on your iPhone if prompted.
   - In Sideloadly, you should see your device detected under "iDevice".

3. **Install the App**:
   - Drag and drop `TinyUs-iOS-unsigned.ipa` onto the IPA icon in Sideloadly.
   - Enter your Apple ID email.
   - Click **Start**. Sideloadly will ask for your Apple ID password to sign the app with a personal free developer certificate directly from Apple's servers.
   - Wait ~30 seconds until Sideloadly displays **"Done"**.

4. **Trust Certificate on iPhone (First Time Only)**:
   - On your iPhone, go to: **Settings -> General -> VPN & Device Management**.
   - Under *Developer App*, tap your Apple ID.
   - Tap **"Trust [your Apple ID]"**.
   - If prompted on iOS 16+, enable **Developer Mode** in **Settings -> Privacy & Security -> Developer Mode** (requires a quick reboot).
   - Launch **Tiny Us** from your iPhone home screen! Enjoy!

---

## Method 2: AltStore (Wireless Auto-Refreshed Sideloading)

If you prefer wireless resigning over local Wi-Fi:
1. Install [AltServer](https://altstore.io/) on Windows.
2. Install AltStore onto your iPhone following the AltStore on-screen guide.
3. Download `TinyUs-iOS-unsigned.ipa` on your iPhone (via Safari from GitHub Actions) and open it in the AltStore app to install.

---

## Method 3: TestFlight (Apple Developer Program — $99/yr)

If you have an official Apple Developer account:
1. Create an App ID in Apple Developer portal (`com.tinyus.app`, and `com.tinyus.app.widget` for the widget).
2. Add Fastlane or Apple App Store Connect API keys to GitHub Secrets.
3. GitHub Actions can upload the build directly to TestFlight, allowing you and your partner to install and update wirelessly from the TestFlight app on iOS.
