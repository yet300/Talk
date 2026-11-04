package com.yet.plugins

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A feature module: Decompose components, MVIKotlin stores, mappers and Metro bindings, no UI.
 * Targets match core (KMP convention); Data is wired only by the application graph in `sharedLogic`,
 * so features see `:core:domain` and the Decompose/MVIKotlin helpers in `:core:common`.
 */
class FeaturePlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(KotlinMultiplatformPlugin::class.java)
        pluginManager.apply(libs.findPlugin("metro").get().get().pluginId)

        val featureNamespace = "com.yet.talk." + path.removePrefix(":").replace(':', '.').replace('-', '.')

        extensions.configure<KotlinMultiplatformExtension> {
            // :feature:tab:home → com.yet.talk.feature.tab.home (the KMP convention drops the first segment).
            extensions.configure<KotlinMultiplatformAndroidLibraryExtension>("android") {
                namespace = featureNamespace
            }

            compilerOptions {
                optIn.add("com.arkivanov.decompose.ExperimentalDecomposeApi")
                optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
            }

            sourceSets.apply {
                commonMain.dependencies {
                    api(project(":core:domain"))
                    implementation(project(":core:common"))
                    // Components expose Value, ChildStack/Slot/Panels and essenty owners; factories take StoreFactory.
                    api(libs.findBundle("decompose").get())
                    api(libs.findBundle("mvi").get())
                }
            }
        }
    }
}
