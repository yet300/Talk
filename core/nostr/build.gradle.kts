// Nostr protocol: NIP-01 events, signing, bech32, relay pool. See docs/research/nostr-crypto-web.md.
plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
}

kotlin {
    android {
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)
            implementation(libs.kotlincrypto.sha2)
        }

        // libsecp256k1 has no JS build: Android and iOS share one actual, JS uses @noble/curves.
        val secp256k1Main = create("secp256k1Main") {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.secp256k1.kmp)
            }
        }
        androidMain {
            dependsOn(secp256k1Main)
            dependencies {
                implementation(libs.secp256k1.kmp.jniAndroid)
            }
        }
        iosMain {
            dependsOn(secp256k1Main)
        }
        jsMain.dependencies {
            implementation(npm("@noble/curves", libs.versions.noble.curves.get()))
        }

        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.testJunit)
            implementation(libs.secp256k1.kmp.jniJvm)
            implementation(libs.ktor.client.okhttp)
        }
    }
}
