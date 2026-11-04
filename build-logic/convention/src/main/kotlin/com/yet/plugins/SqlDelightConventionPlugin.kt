package com.yet.plugins

import app.cash.sqldelight.gradle.SqlDelightExtension
import libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * SQLDelight for KMP modules (Android, iOS, JS).
 * The JS driver is asynchronous, so every database is generated with async APIs.
 * The consuming web app must serve `sql-wasm.wasm` and the sqljs worker next to its bundle.
 */
class SqlDelightConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply(libs.findPlugin("sqlDelight").get().get().pluginId)
        }

        extensions.configure<SqlDelightExtension> {
            databases.configureEach {
                generateAsync.set(true)
            }
        }

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.apply {
                commonMain.dependencies {
                    implementation(libs.findLibrary("sqldelight-coroutines-extensions").get())
                    implementation(libs.findLibrary("sqldelight-async-extensions").get())
                }

                commonTest.dependencies {
                    implementation(libs.findBundle("testing").get())
                }

                androidMain.dependencies {
                    implementation(libs.findLibrary("sqldelight-android").get())
                }

                // Host (JVM) unit tests run against an in-memory JDBC SQLite driver.
                // Lazy: the source set exists only once the module calls `withHostTest`.
                matching { it.name == "androidHostTest" }.configureEach {
                    dependencies {
                        implementation(libs.findLibrary("sqldelight-driver").get())
                    }
                }

                nativeMain.dependencies {
                    implementation(libs.findLibrary("sqldelight-native").get())
                }

                jsMain.dependencies {
                    implementation(libs.findLibrary("sqldelight-webWorkerDriver").get())
                    implementation(npm("@cashapp/sqldelight-sqljs-worker", libs.findVersion("sqldelight").get().requiredVersion))
                    implementation(npm("sql.js", libs.findVersion("sqljs").get().requiredVersion))
                }
            }
        }
    }
}
