plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.metro)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedLogic"
            isStatic = true
            // Types Swift works with directly; everything else stays internal to the framework.
            export(projects.core.domain)
            export(projects.core.resources)
            export(projects.feature.root)
            export(projects.feature.tab.main)
            export(projects.feature.tab.home)
            export(libs.decompose)
            export(libs.essenty.lifecycle)
            export(libs.essenty.stateKeeper)
            export(libs.essenty.backHandler)
            export(libs.moko.resources)
        }
    }
    js {
        outputModuleName = "sharedLogic"
        binaries.library()
        generateTypeScriptDefinitions()
        compilerOptions {
            target = "es2015"
            optIn.add("kotlin.js.ExperimentalJsExport")
        }
    }
    android {
        // The convention derives "com.yet.talk" for root-level modules; the app already owns it.
        namespace = "com.yet.talk.sharedLogic"
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(projects.core.resources)
            api(projects.feature.root)
            api(projects.feature.tab.main)
            api(projects.feature.tab.home)
            api(libs.decompose)
            api(libs.essenty.lifecycle)
            api(libs.essenty.stateKeeper)
            api(libs.essenty.backHandler)
            // Not exported: contributes repository bindings to the app graph.
            implementation(projects.core.data)
            implementation(projects.core.common)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.testJunit)
        }
    }
}
