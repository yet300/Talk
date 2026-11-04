package com.yet.convention

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import libs
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private const val JDK_VERSION = 17

internal fun Project.configureKotlinMultiplatform(
    extension: KotlinMultiplatformExtension,
) = extension.apply {
    jvmToolchain(JDK_VERSION)

    extensions.configure<KotlinMultiplatformAndroidLibraryExtension>("android") {
        val moduleName = path.split(":").drop(2).joinToString(".")
        namespace = if (moduleName.isNotEmpty()) "com.yet.$moduleName" else "com.yet.talk"

        compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
        minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()

    }

    js {
        browser()
    }

    //apple targets
    iosArm64()
    iosSimulatorArm64()


    applyDefaultHierarchyTemplate()

    //common dependencies
    sourceSets.apply {
        commonMain {
            dependencies {
                api(libs.findLibrary("kotlinx-coroutines-core").get())
                api(libs.findLibrary("kotlinx-datetime").get())
                api(libs.findLibrary("kotlinx-serialization-json").get())
            }
        }
        commonTest.dependencies {
            implementation(libs.findBundle("testing").get())
        }
    }
}