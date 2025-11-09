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

    // TestPlugins expects each module to provide its own manifest
    sourceSets["main"].manifest.srcFile("src/main/AndroidManifest.xml")
}

dependencies {
    implementation(project(":common"))      // provided by TestPlugins
    implementation("org.jsoup:jsoup:1.16.1")
}