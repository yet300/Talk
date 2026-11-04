plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.sqlDelight) apply false
    alias(libs.plugins.skie) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.mokkery) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.mokoResources) apply false
    alias(libs.plugins.aboutLibraries) apply false
}