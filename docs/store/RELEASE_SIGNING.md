# Release signing and the app bundle

Google Play only accepts an app bundle (`.aab`) signed with your own **upload key**. Without one, release builds fall back to the debug key: fine for installing on your own phone, but Play rejects them. `:app:bundleRelease` now refuses to run without the key.

You create the key yourself, so the passwords never leave your hands. Neither file is ever committed: `keystore.properties`, `*.jks` and `*.keystore` are in `.gitignore`.

## 1. Create the upload key (once)

In a terminal at the repo root:

```
"C:\Program Files\OpenLogic\jdk-17.0.9.9-hotspot\bin\keytool.exe" -genkeypair -v -keystore release.jks -alias tinyus-upload -keyalg RSA -keysize 2048 -validity 10000
```

It asks for a keystore password, your name and organisation details, and a key password (pressing Enter makes it the same as the keystore password). This creates `release.jks` at the repo root.

## 2. Tell Gradle about it

Create `keystore.properties` at the repo root (next to `settings.gradle.kts`):

```
storeFile=release.jks
storePassword=<your keystore password>
keyAlias=tinyus-upload
keyPassword=<your key password>
```

## 3. Back both files up

Copy `release.jks` and `keystore.properties` somewhere safe outside the project, such as a password manager or an encrypted drive. With Play App Signing (step 4), a lost upload key can be reset through Google support, but that takes days.

## 4. Play App Signing

When you create the app in the Play Console, keep **Play App Signing** on (the default). Google holds the key that signs what users download; your upload key only proves the uploads come from you.

## 5. Build and check the bundle

```
./gradlew :app:bundleRelease
```

The bundle is written to `app/build/outputs/bundle/release/app-release.aab`. Check it's signed with your upload key, not the debug key:

```
"C:\Program Files\OpenLogic\jdk-17.0.9.9-hotspot\bin\keytool.exe" -printcert -jarfile app/build/outputs/bundle/release/app-release.aab
```

The owner line should show the name you entered in step 1, not "CN=Android Debug".

## 6. Version numbers

Every upload needs a higher `versionCode` (in `app/build.gradle.kts`, now `1`). `versionName` ("1.0") is what users see.
