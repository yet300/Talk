plugins {
    alias(libs.plugins.local.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.tab.home)
            api(projects.feature.tab.search)
            api(projects.feature.tab.activity)
            api(projects.feature.tab.bookmarks)
            api(projects.feature.profile)
        }
    }
}
