import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
            implementation(libs.haze)
            implementation(libs.haze.glass)
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
