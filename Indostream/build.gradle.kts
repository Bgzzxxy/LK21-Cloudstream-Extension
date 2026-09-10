plugins {
    id("com.android.library")
    kotlin("android")
    id("com.lagradost.cloudstream3.gradle")
}

cloudstream {
    authors = listOf("IndostreamDev")
    description = "Ekstensi LK21 & NontonDrama dengan Auto Extractor"
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
    compileOnly("com.github.recloudstream:cloudstream:-SNAPSHOT")
}
