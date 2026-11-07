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
            api(projects.feature.tab.videos)
            api(projects.feature.video)
            api(projects.feature.profile)
            api(projects.feature.post)
        }
    }
}
