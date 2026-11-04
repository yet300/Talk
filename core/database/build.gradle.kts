// Local cache (SQLDelight). Exposes DAOs that core:data uses to implement repositories.
plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.local.sqlDelight)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            api(libs.androidx.paging.common)
            implementation(libs.sqldelight.paging)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.testJunit)
        }
    }
}

sqldelight {
    databases {
        create("TalkDatabase") {
            packageName.set("com.yet.talk.database")
        }
    }
}
