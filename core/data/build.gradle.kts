// Repository implementations: core:api (remote) + core:database (local), bound to core:domain interfaces.
plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.database)
            implementation(projects.core.api)
            implementation(projects.core.common)
        }
    }
}
