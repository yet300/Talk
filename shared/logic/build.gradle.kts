plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.metro)
    // Packs the resources of every module into the iOS framework (copyFrameworkResourcesToApp in Xcode).
    alias(libs.plugins.mokoResources)
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
            export(projects.feature.auth)
            export(projects.feature.compose)
            export(projects.feature.post)
            export(projects.feature.profile)
            export(projects.feature.settings)
            export(projects.feature.editprofile)
            export(projects.feature.tab.main)
            export(projects.feature.tab.home)
            export(projects.feature.tab.search)
            export(projects.feature.tab.activity)
            export(projects.feature.tab.bookmarks)
            export(projects.feature.tab.videos)
            export(projects.feature.video)
            export(projects.feature.tab.chats)
            export(projects.feature.chat)
            export(projects.feature.channels)
            export(libs.decompose)
            export(libs.essenty.lifecycle)
            export(libs.essenty.stateKeeper)
            export(libs.essenty.backHandler)
            export(libs.moko.resources)
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
            api(projects.feature.auth)
            api(projects.feature.compose)
            api(projects.feature.post)
            api(projects.feature.profile)
            api(projects.feature.settings)
            api(projects.feature.editprofile)
            api(projects.feature.tab.main)
            api(projects.feature.tab.home)
            api(projects.feature.tab.search)
            api(projects.feature.tab.activity)
            api(projects.feature.tab.bookmarks)
            api(projects.feature.tab.videos)
            api(projects.feature.video)
            api(projects.feature.tab.chats)
            api(projects.feature.chat)
            api(projects.feature.channels)
            api(libs.decompose)
            api(libs.essenty.lifecycle)
            api(libs.essenty.stateKeeper)
            api(libs.essenty.backHandler)
            api(libs.moko.resources)
            // Not exported: contribute bindings to the app graph, which provides their platform parts.
            implementation(projects.core.data)
            implementation(projects.core.database)
            implementation(projects.core.api)
            implementation(projects.core.common)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ksafe)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.connectivity.device)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.connectivity.device)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
            implementation(libs.ktor.client.js)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.testJunit)
        }
    }
}

multiplatformResources {
    resourcesPackage.set("com.yet.talk.shared")
}
