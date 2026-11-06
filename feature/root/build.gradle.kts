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
            api(projects.feature.settings)
            api(projects.feature.editprofile)
        }
    }
}
