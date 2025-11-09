plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.bdix.ctgmovies"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    sourceSets["main"].manifest.srcFile("src/main/AndroidManifest.xml")
}

dependencies {
    // No :common here — TestPlugins template doesn't use it
    // Keep deps minimal; CloudStream APIs are provided by the template build
}