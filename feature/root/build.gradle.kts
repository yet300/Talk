plugins {
    alias(libs.plugins.local.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.tab.main)
            api(projects.feature.auth)
            api(projects.feature.post)
            api(projects.feature.compose)
            api(projects.feature.profile)
            api(projects.feature.settings)
        }
    }
}
