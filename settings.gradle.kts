rootProject.name = "Talk"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
project(":androidApp").projectDir = file("app/androidApp")

include(":sharedLogic")
project(":sharedLogic").projectDir = file("shared/logic")
include(":sharedUI")
project(":sharedUI").projectDir = file("shared/ui")

include(":core:common")
include(":core:domain")
include(":core:nostr")
include(":core:api")
include(":core:database")
include(":core:data")
include(":core:resources")

include(":feature:root")
include(":feature:auth")
include(":feature:compose")
include(":feature:post")
include(":feature:profile")
include(":feature:settings")
include(":feature:tab:main")
include(":feature:tab:home")
include(":feature:tab:search")
include(":feature:tab:activity")
include(":feature:tab:bookmarks")
