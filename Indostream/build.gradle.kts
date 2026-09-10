plugins {
    id("com.android.library")
    kotlin("android")
    id("com.lagradost.cloudstream3.gradle")
}

cloudstream {
    setAuthors(listOf("IndostreamDev"))
    setDescription("Ekstensi LK21 & NontonDrama dengan Auto Extractor")
    setName("Indostream")
    setTvTypes(listOf("Movie", "TvSeries"))
}

android {
    namespace = "com.indostream"
    compileSdk = 33
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    val cs3 = "com.github.recloudstream:cloudstream:pre-release"
    compileOnly(cs3)
}
