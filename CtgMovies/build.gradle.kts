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
    // Keep only what you actually use. Jsoup is fine.
    implementation("org.jsoup:jsoup:1.16.1")
    // ❌ Do NOT reference project(":common") – it doesn't exist in new template
}