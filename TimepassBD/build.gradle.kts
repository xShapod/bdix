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
    // No :common here either
}