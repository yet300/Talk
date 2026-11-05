// The web app: Compose HTML (Compose runtime over the DOM) on top of sharedLogic, bundled by webpack.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    // Copies the moko resources (strings, images) of every module into the bundle.
    alias(libs.plugins.mokoResources)
}

kotlin {
    js {
        outputModuleName = "webApp"
        browser {
            commonWebpackConfig {
                outputFileName = "webApp.js"
                cssSupport { enabled.set(true) }
            }
        }
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(projects.sharedLogic)
            implementation(libs.compose.runtime)
            implementation(libs.compose.html.core)
            implementation(libs.kotlinx.coroutines.core)
            // Served next to the app as static files (see copySqliteWasm).
            implementation(npm("@sqlite.org/sqlite-wasm", libs.versions.sqlite.wasm.get()))
            // Markdown: marked renders GFM, DOMPurify sanitizes the HTML before it reaches the DOM.
            implementation(npm("marked", libs.versions.marked.get()))
            implementation(npm("dompurify", libs.versions.dompurify.get()))
            implementation(libs.androidx.paging.common)
        }
        all {
            languageSettings.optIn("com.arkivanov.decompose.ExperimentalDecomposeApi")
        }
    }
}

multiplatformResources {
    resourcesPackage.set("com.yet.talk.web")
}

// WebSqlDriverFactory starts /sqlite/talk-sqlite.worker.mjs, which imports the SQLite WASM build next to it.
val copySqliteWasm by tasks.registering(Sync::class) {
    dependsOn(rootProject.tasks.named("kotlinNpmInstall"))
    from(rootProject.layout.buildDirectory.dir("js/node_modules/@sqlite.org/sqlite-wasm/dist")) {
        include("index.mjs", "sqlite3.wasm", "sqlite3-opfs-async-proxy.js")
        rename("index.mjs", "sqlite3.mjs")
    }
    from(rootProject.file("core/database/src/jsMain/resources/sqlite/talk-sqlite.worker.mjs"))
    into(layout.buildDirectory.dir("generated/sqlite/sqlite"))
}

kotlin.sourceSets.named("jsMain") {
    resources.srcDir(copySqliteWasm.map { it.destinationDir.parentFile })
}
