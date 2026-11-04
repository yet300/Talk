// Remote data sources on top of core:nostr: posts and reactions over relays, Blossom uploads, NIP-05 lookups.
plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.nostr)
            implementation(libs.ktor.client.core)
        }
    }
}
