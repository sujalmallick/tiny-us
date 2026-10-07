# App Store release: signing and TestFlight

The App Store only accepts builds signed with your Apple Developer account. The `Tiny Us iOS Release (TestFlight)` workflow (`.github/workflows/ios-release.yml`) does the signing and the upload on GitHub's Mac. You set up an account and four secrets once, from any computer; no Mac is needed.

Until then, the normal iOS build keeps making the unsigned IPA for sideloading.

## 1. Join the Apple Developer Program (once)

At [developer.apple.com/programs](https://developer.apple.com/programs/), enroll as an individual. It costs 99 USD a year and can take a day or two to approve. Your name becomes the seller name on the App Store.

## 2. Register the app's identifiers

In **Certificates, Identifiers & Profiles > Identifiers** (on developer.apple.com):

1. **App Group:** `+`, then **App Groups**, then the identifier `group.com.tinyus.app.shared`.
2. **App ID for the app:** `+`, then **App IDs > App**, then the bundle ID `com.tinyus.app`. Tick **App Groups** and pick the group from step 1.
3. **App ID for the widget:** the same, with `com.tinyus.app.widget` and the same App Group.

These match the Android app's ID (`com.tinyus.app`). The iOS project still uses the placeholder `com.example.tinyus`, which has to change before the first upload (see "Bundle ID" below).

## 3. Create the app in App Store Connect

At [appstoreconnect.apple.com](https://appstoreconnect.apple.com), go to **Apps > + > New App**: platform iOS, name `Tiny Us: A Cozy Couple World`, primary language English, bundle ID `com.tinyus.app`, SKU `tinyus-ios`. Fill in the listing from `docs/store/appstore.md`.

## 4. Create an App Store Connect API key

In App Store Connect, go to **Users and Access > Integrations > App Store Connect API > Team Keys > +**:

- **Name:** `GitHub TestFlight`.
- **Access:** **Admin**. Xcode needs it to create the distribution certificate and profiles by itself.

Download the key (`AuthKey_XXXXXXXXXX.p8`). Apple lets you download it **only once**, so keep a copy somewhere safe, such as a password manager. Note the **Key ID** (next to the key) and the **Issuer ID** (at the top of the page).

Your **Team ID** is under **Membership details** on developer.apple.com.

## 5. Add four secrets to GitHub

On GitHub, go to the repo's **Settings > Secrets and variables > Actions > New repository secret**:

| Secret | Value |
|---|---|
| `ASC_KEY_ID` | the Key ID |
| `ASC_ISSUER_ID` | the Issuer ID |
| `APPLE_TEAM_ID` | the Team ID |
| `ASC_KEY_P8_BASE64` | the `.p8` file as base64 (below) |

To turn the key into base64 on Windows, run this in PowerShell in the folder with the file:

```
[Convert]::ToBase64String([IO.File]::ReadAllBytes("AuthKey_XXXXXXXXXX.p8")) | Set-Clipboard
```

Then paste it as the secret's value. Secrets are encrypted by GitHub and never shown in logs. Don't put the key in the repo.

## 6. Build and upload

On GitHub, go to **Actions > Tiny Us iOS Release (TestFlight) > Run workflow**. It takes about 30 minutes. The build number is the workflow's run number, so every upload is higher than the last. The version users see is `MARKETING_VERSION` in the Xcode project (now `1.0`).

When Apple has processed the build (usually 10 to 30 minutes after the upload), it shows in App Store Connect under **TestFlight**. Add yourself as an internal tester and install it on the iPad with Apple's **TestFlight** app.

## 7. Submit for review

In App Store Connect, open the version, pick the TestFlight build, add the screenshots (`docs/store/screenshots/appstore_iphone_6_9/` and `appstore_ipad_13/`), and press **Add for Review**. Review usually takes one to three days.

## Bundle ID

The iOS project uses the placeholder `com.example.tinyus`, with `group.com.example.tinyus.shared` for the widget's App Group. Before the first upload, it moves to `com.tinyus.app` (the Android ID), `com.tinyus.app.widget` and `group.com.tinyus.app.shared`. That's a small change in the Xcode project, the entitlements and the App Group name in the code.

The installed test app on the iPad then counts as a different app. Save a backup in the old one (Settings > Backup & Restore) and restore it in the new one to keep your data.
