// Nostr protocol: NIP-01 events, signing, bech32, relay pool. See docs/research/nostr-crypto-web.md.
plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)
            implementation(libs.kotlincrypto.sha2)
        }
        // secp256k1-kmp has no JS target, so it is declared per platform; JS uses @noble/curves.
        androidMain.dependencies {
            implementation(libs.secp256k1.kmp)
            implementation(libs.secp256k1.kmp.jniAndroid)
        }
        iosMain.dependencies {
            implementation(libs.secp256k1.kmp)
        }
        jsMain.dependencies {
            implementation(npm("@noble/curves", libs.versions.noble.curves.get()))
        }
    }
}
