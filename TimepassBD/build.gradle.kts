plugins {
    id("com.android.library")
    kotlin("android")
    id("com.lagradost.cloudstream3.gradle")
}

android {
    namespace = "com.bdix.timepassbd"
    compileSdk = 34
    defaultConfig { minSdk = 21 }
    sourceSets["main"].manifest.srcFile("src/main/AndroidManifest.xml")
}

dependencies {
    implementation(project(":common"))
    implementation("org.jsoup:jsoup:1.16.1")
}

cloudstream {
    // Shown in CloudStream
    description = "BDIX directory-style provider for timepassbd.live"
    authors = listOf("xShapod")
    language = "en"
    status = 1 // 1 = working (adjust later if needed)
    tvTypes = listOf("Movie", "TvSeries", "Live")
    iconUrl = "https://www.google.com/s2/favicons?domain=timepassbd.live&sz=128"
}