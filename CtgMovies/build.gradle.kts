
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
    implementation(project(":common"))
    implementation("org.jsoup:jsoup:1.16.1")
}