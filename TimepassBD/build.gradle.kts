plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.bdix.timepassbd"
    compileSdk = 34
    defaultConfig { minSdk = 21 }
    sourceSets["main"].manifest.srcFile("src/main/AndroidManifest.xml")
}

dependencies {
    implementation(project(":common")) // CloudStream common
    // Jsoup already comes via common; you can omit extra jsoup dependency
}