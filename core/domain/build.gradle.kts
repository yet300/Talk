// Entities, repository interfaces and use cases. Pure Kotlin: no Nostr, database or UI types.
plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            // PagingData is the paging contract of the repositories (KMP: Android, iOS, JS).
            api(libs.androidx.paging.common)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.testJunit)
        }
    }
}
