plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.skypulse.enhancer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.skypulse.enhancer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
}