plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.projeindir.kotest"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.projeindir.kotest"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "0.2-slot"
    }

    // Sabit anahtar: her GitHub derlemesi aynı imzayla çıkar, üstüne kurulum yapılabilir
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
