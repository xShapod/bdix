plugins {
    id("com.android.library")
    kotlin("android")
    id("cloudstream") // <- use the Cloudstream Gradle plugin
}

android {
    namespace = "com.bdix.ctgmovies"
    compileSdk = 34
    defaultConfig { minSdk = 21 }
    sourceSets["main"].manifest.srcFile("src/main/AndroidManifest.xml")
}

cloudstream {
    language = "en"
    description = "CTGMovies provider for BDIX-only access."
    authors = listOf("xShapod")
    status = 1 // 1 = working, 0 = beta, etc.
    tvTypes = listOf("Movie", "TvSeries")
    iconUrl = "https://www.google.com/s2/favicons?domain=ctgmovies.com&sz=128"
}

dependencies {
    implementation("org.jsoup:jsoup:1.16.1")
}