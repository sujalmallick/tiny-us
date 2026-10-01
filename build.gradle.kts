// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.kmp.library) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.compose) apply false
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
    val combined = (tomlText.get() + "\n" + sharedBuildText.get() + "\n" + appDependenciesText.get())
      .lines()
      .filter { !it.contains("verifyPrivacySafeguards") && !it.contains("verifyNoUnauthorizedNetworkingOrAnalytics") }
      .joinToString("\n")
      .lowercase()

    for (kw in forbidden) {
      if (combined.contains(kw)) {
        throw GradleException(
          "SECURITY/PRIVACY VIOLATION: Forbidden networking/analytics keyword '$kw' detected in dependencies! " +
            "Tiny Us strictly enforces an offline-first, telemetry-free architecture."
        )
      }
    }
  }
}

val tomlFile = file("gradle/libs.versions.toml")
val sharedFile = file("shared/build.gradle.kts")
val appFile = file("app/build.gradle.kts")

val privacyTask = tasks.register<VerifyPrivacyTask>("verifyPrivacySafeguards") {
  group = "verification"
  description = "Fails build if unauthorized networking, HTTP client, or analytics dependencies are detected."
  tomlText.set(if (tomlFile.exists()) tomlFile.readText() else "")
  sharedBuildText.set(if (sharedFile.exists()) sharedFile.readText() else "")
  appDependenciesText.set(if (appFile.exists()) appFile.readText() else "")
}

// Backward compatibility alias for existing CI scripts
tasks.register("verifyNoUnauthorizedNetworkingOrAnalytics") {
  group = "verification"
  dependsOn(privacyTask)
}
