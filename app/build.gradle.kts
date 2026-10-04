import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

// Release signing comes from an uncommitted keystore.properties at the repo root:
//   storeFile=release.jks  storePassword=…  keyAlias=…  keyPassword=…
// Without it, release builds fall back to the debug key (fine locally, never for the Play Store).
val releaseKeystore = Properties().apply {
  val file = rootProject.file("keystore.properties")
  if (file.exists()) file.inputStream().use { load(it) }
}

// The app bundle is what goes to Google Play: refuse to build it with the debug key.
val hasReleaseKey = !releaseKeystore.isEmpty
tasks.matching { it.name == "bundleRelease" }.configureEach {
  doFirst {
    if (!hasReleaseKey) {
      throw GradleException("No keystore.properties: the release bundle would be signed with the debug key. See docs/store/RELEASE_SIGNING.md.")
    }
  }
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.tinyus.app"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    if (!releaseKeystore.isEmpty) {
      create("release") {
        storeFile = rootProject.file(releaseKeystore.getProperty("storeFile"))
        storePassword = releaseKeystore.getProperty("storePassword")
        keyAlias = releaseKeystore.getProperty("keyAlias")
        keyPassword = releaseKeystore.getProperty("keyPassword")
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
    }
    debug {
      // Uses default Android debug signing configuration
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlin {
    jvmToolchain(17)
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
    }
  }

  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

dependencies {
  implementation(project(":shared"))
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  // Privacy lock: fingerprint/face/device-credential prompt (on-device only, no network).
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.fragment.ktx)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)

  // Local Unit & Robolectric Tests
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)

  // Android Instrumentation Tests
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)

  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.named("preBuild").configure {
  dependsOn(rootProject.tasks.named("verifyPrivacySafeguards"))
  dependsOn(rootProject.tasks.named("verifyNoPersonalData"))
}

