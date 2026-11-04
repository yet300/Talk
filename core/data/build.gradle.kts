// Repository implementations: core:api (remote) + core:database (local), bound to core:domain interfaces.
plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        withHostTest {}
    }

    // The SQLDelight native driver needs the system SQLite; only modules with the SQLDelight plugin get it automatically.
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.all { linkerOpts("-lsqlite3") }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.database)
            implementation(projects.core.api)
            implementation(projects.core.common)
            implementation(libs.ksafe)
        }
        commonTest.dependencies {
            implementation(libs.sqldelight.async.extensions)
            implementation(libs.androidx.paging.testing)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.testJunit)
            implementation(libs.secp256k1.kmp.jniJvm)
            implementation(libs.sqldelight.driver)
        }
        iosTest.dependencies {
            implementation(libs.sqldelight.native)
        }
    }
}
