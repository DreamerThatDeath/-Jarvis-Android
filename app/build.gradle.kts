
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.dreamerthatdeath.jarvis"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dreamerthatdeath.jarvis"
        minSdk = 26
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        buildConfig = true
    }
}
