// Shared strings, colours, fonts and images (moko-resources) plus platform helpers for them.
plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.mokoResources)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.moko.resources)
        }
    }
}

multiplatformResources {
    resourcesPackage.set("com.yet.talk.resources")
}
