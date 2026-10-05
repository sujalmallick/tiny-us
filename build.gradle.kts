// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.kmp.library) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.compose.multiplatform) apply false
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

/**
 * Personal-data guard. Your real names, dates and letters live only in the uncommitted
 * personal_profile.json. If that file exists locally, this fails the build when any of its
 * personal text shows up in a git-tracked file — so it can never slip into the public repo.
 * Nothing personal is stored in the check itself; without the file (e.g. on CI) it is skipped.
 */
abstract class VerifyNoPersonalDataTask : DefaultTask() {
  @get:Input
  abstract val rootPath: Property<String>

  @TaskAction
  fun verify() {
    val root = java.io.File(rootPath.get())
    val personal = java.io.File(root, "personal_profile.json")
    if (!personal.exists()) {
      logger.info("verifyNoPersonalData: no personal_profile.json, skipping")
      return
    }
    val template = java.io.File(root, "personal_profile.template.json").takeIf { it.exists() }?.readText() ?: ""

    // Every string in the personal file, minus anything the public template also contains.
    fun strings(node: Any?): List<String> = when (node) {
      is String -> listOf(node)
      is Map<*, *> -> node.values.flatMap { strings(it) }
      is List<*> -> node.flatMap { strings(it) }
      else -> emptyList()
    }
    val values = strings(groovy.json.JsonSlurper().parse(personal))
      .map { it.trim() }
      .filter { it.length >= 3 && !template.contains(it) && !it.matches(Regex("[0-9:-]+")) }
      .toSet()
    if (values.isEmpty()) return

    val tracked = ProcessBuilder("git", "ls-files", "-z").directory(root).redirectErrorStream(true).start()
      .inputStream.bufferedReader().readText().split(0.toChar()).filter { it.isNotBlank() }
    val binary = setOf("png", "jpg", "jpeg", "webp", "gif", "ico", "jar", "mp3", "ogg", "wav", "ttf", "otf", "zip", "keystore", "jks")
    val leaks = mutableListOf<String>()
    for (path in tracked) {
      if (path.substringAfterLast('.', "").lowercase() in binary) continue
      val file = java.io.File(root, path)
      if (!file.isFile || file.length() > 5_000_000) continue
      val text = file.readText()
      for (value in values) {
        // Short values (names) must match as whole words; longer ones (letters) anywhere.
        val found = if (value.length < 12) Regex("""\b""" + Regex.escape(value) + """\b""").containsMatchIn(text) else text.contains(value)
        if (found) leaks += "$path contains personal text '${value.take(24)}${if (value.length > 24) "..." else ""}'"
      }
    }
    if (leaks.isNotEmpty()) {
      throw GradleException("PERSONAL DATA LEAK: text from personal_profile.json found in tracked files:\n" + leaks.joinToString("\n"))
    }
  }
}

val personalDataTask = tasks.register<VerifyNoPersonalDataTask>("verifyNoPersonalData") {
  group = "verification"
  description = "Fails if text from the local personal_profile.json appears in git-tracked files."
  rootPath.set(rootDir.absolutePath)
}

