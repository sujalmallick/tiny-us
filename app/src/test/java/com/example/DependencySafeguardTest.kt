package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * CI / Build Safeguard Test.
 *
 * Guarantees that Tiny Us strictly maintains its 100% offline-first, telemetry-free architecture.
 * Automatically fails the build if any HTTP client, networking engine, tracking SDK, or
 * unauthorized exact-alarm permission is introduced.
 */
class DependencySafeguardTest {

    private val forbiddenKeywords = listOf(
        "okhttp",
        "retrofit",
        "ktor",
        "volley",
        "firebase",
        "analytics",
        "telemetry",
        "mixpanel",
        "appsflyer",
        "adjust",
        "amplitude",
        "sentry",
        "crashlytics",
        "admob",
        "applovin",
        "facebook",
        "segment"
    )

    private val projectRoot: File by lazy {
        // Find project root by walking up from current execution directory
        var curr = File(".").canonicalFile
        while (curr.parentFile != null && !File(curr, "settings.gradle.kts").exists()) {
            curr = curr.parentFile!!
        }
        curr
    }

    @Test
    fun testNoForbiddenNetworkingOrAnalyticsInAppBuildGradle() {
        val buildFile = File(projectRoot, "app/build.gradle.kts")
        assertTrue("app/build.gradle.kts must exist", buildFile.exists())
        val content = buildFile.readLines()
            .takeWhile { !it.contains("VerifyPrivacyTask") }
            .joinToString("\n")
            .lowercase()

        for (kw in forbiddenKeywords) {
            assertFalse(
                "SECURITY/PRIVACY VIOLATION: Forbidden keyword '$kw' detected in app/build.gradle.kts!",
                content.contains(kw)
            )
        }
    }

    @Test
    fun testNoForbiddenNetworkingOrAnalyticsInSharedBuildGradle() {
        val sharedFile = File(projectRoot, "shared/build.gradle.kts")
        if (sharedFile.exists()) {
            val content = sharedFile.readText().lowercase()
            for (kw in forbiddenKeywords) {
                assertFalse(
                    "SECURITY/PRIVACY VIOLATION: Forbidden keyword '$kw' detected in shared/build.gradle.kts!",
                    content.contains(kw)
                )
            }
        }
    }

    @Test
    fun testNoForbiddenNetworkingOrAnalyticsInLibsVersionsToml() {
        val tomlFile = File(projectRoot, "gradle/libs.versions.toml")
        assertTrue("gradle/libs.versions.toml must exist", tomlFile.exists())
        val content = tomlFile.readText().lowercase()

        for (kw in forbiddenKeywords) {
            assertFalse(
                "SECURITY/PRIVACY VIOLATION: Forbidden keyword '$kw' detected in gradle/libs.versions.toml!",
                content.contains(kw)
            )
        }
    }

    @Test
    fun testNoExactAlarmPermissionInAndroidManifest() {
        val manifestFile = File(projectRoot, "app/src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val content = manifestFile.readText()

        assertFalse(
            "POLICY VIOLATION: SCHEDULE_EXACT_ALARM must not be declared in AndroidManifest.xml!",
            content.contains("android.permission.SCHEDULE_EXACT_ALARM")
        )
        assertFalse(
            "POLICY VIOLATION: USE_EXACT_ALARM must not be declared in AndroidManifest.xml!",
            content.contains("android.permission.USE_EXACT_ALARM")
        )
    }
}
