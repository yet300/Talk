// The MCP server for AI agents: a Node program over stdio. JS only on purpose, so it runs the same
// core:nostr and core:api code as the web app (`local.kotlin.multiplatform` would add Android and iOS).
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.metro)
}

kotlin {
    js {
        outputModuleName = "talk-mcp"
        nodejs {
            testTask { useMocha { timeout = "2m" } }
        }
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(projects.core.api)
            implementation(projects.core.domain)
            implementation(libs.ktor.client.js)
        }
        jsTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
