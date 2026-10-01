plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
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

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debug")
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

abstract class VerifyPrivacyTask : DefaultTask() {
  @get:Input
  abstract val tomlText: Property<String>

  @get:Input
  abstract val sharedBuildText: Property<String>

  @get:Input
  abstract val appDependenciesText: Property<String>

  @TaskAction
  fun verify() {
    val forbidden = listOf(
      "ok" + "http", "retro" + "fit", "kt" + "or", "vol" + "ley", "fire" + "base", "analy" + "tics", "tele" + "metry",
      "mix" + "panel", "apps" + "flyer", "ad" + "just", "ampli" + "tude", "sen" + "try", "crash" + "lytics",
      "ad" + "mob", "app" + "lovin", "face" + "book", "seg" + "ment"
    )
    val combined = (tomlText.get() + "\n" + sharedBuildText.get() + "\n" + appDependenciesText.get()).lowercase()
    for (kw in forbidden) {
      if (combined.contains(kw)) {
        throw GradleException(
          "SECURITY/PRIVACY VIOLATION: Forbidden networking/analytics keyword '$kw' detected! " +
            "Tiny Us strictly enforces an offline-first, telemetry-free architecture."
        )
      }
    }
  }
}

val tomlFile = rootProject.layout.projectDirectory.file("gradle/libs.versions.toml").asFile
val sharedFile = rootProject.layout.projectDirectory.file("shared/build.gradle.kts").asFile
val appFile = project.buildFile

tasks.register<VerifyPrivacyTask>("verifyNoUnauthorizedNetworkingOrAnalytics") {
  group = "verification"
  description = "Fails build if unauthorized networking, HTTP client, or analytics dependencies are detected."
  tomlText.set(if (tomlFile.exists()) tomlFile.readText() else "")
  sharedBuildText.set(if (sharedFile.exists()) sharedFile.readText() else "")
  appDependenciesText.set(
    if (appFile.exists()) {
      appFile.readLines()
        .takeWhile { !it.contains("VerifyPrivacyTask") }
        .joinToString("\n")
    } else ""
  )
}

tasks.named("preBuild").configure {
  dependsOn("verifyNoUnauthorizedNetworkingOrAnalytics")
}

