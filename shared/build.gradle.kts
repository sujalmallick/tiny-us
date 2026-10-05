plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        // kotlinx-datetime 0.7 uses kotlin.time.Clock/Instant, still marked experimental in Kotlin 2.2.
        optIn.add("kotlin.time.ExperimentalTime")
    }

    android {
        namespace = "com.example.shared"
        compileSdk = 35
        minSdk = 26

        // Runs commonTest on the JVM too, so shared logic is tested on any dev machine (not only macOS).
        withHostTest {}

        // Compose resources (shared text) are packaged as Android resources too.
        androidResources { enable = true }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(libs.kotlinx.datetime)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)

            // Compose Multiplatform: lets iOS run the same UI code as Android (plan 08).
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            api(compose.components.resources)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.resources {
    // The Android app reads shared text too, so the generated Res class must be public.
    publicResClass = true
    packageOfResClass = "com.example.resources"
    generateResClass = always
}
