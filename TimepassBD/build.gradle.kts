plugins {
    id("com.android.library")
    kotlin("android")
    id("cloudstream")
}

android {
    namespace = "com.bdix.timepassbd"
    compileSdk = 34
    defaultConfig { minSdk = 21 }
    sourceSets["main"].manifest.srcFile("src/main/AndroidManifest.xml")
}

cloudstream {
    language = "en"
    description = "TimepassBD provider for BDIX-only servers (directory + HLS)."
    authors = listOf("xShapod")
    status = 1
    tvTypes = listOf("Movie", "TvSeries", "Live")
    iconUrl = "https://www.google.com/s2/favicons?domain=timepassbd.live&sz=128"
}

dependencies {
    implementation("org.jsoup:jsoup:1.16.1")
}