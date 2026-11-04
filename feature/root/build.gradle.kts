plugins {
    alias(libs.plugins.local.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.tab.main)
        }
    }
}
