plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.bdix.timepassbd"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    sourceSets["main"].manifest.srcFile("src/main/AndroidManifest.xml")
}

dependencies {
    implementation("org.jsoup:jsoup:1.16.1")
    // No project(":common")
}