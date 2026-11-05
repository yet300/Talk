import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Android-only on purpose: iOS renders with SwiftUI and web with React, so the shared KMP
// convention (which adds iOS and JS targets) is not applied here; only the Compose convention is.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.local.compose.multiplatform)
}

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.yet.talk.sharedUI"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
            optIn.add("com.arkivanov.decompose.ExperimentalDecomposeApi")
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    sourceSets {
        // Android-only module: the UI lives in androidMain so it can use Android resources directly.
        androidMain.dependencies {
            api(projects.sharedLogic)
            implementation(libs.decompose.compose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        commonTest.dependencies {
            implementation(libs.bundles.testing)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
