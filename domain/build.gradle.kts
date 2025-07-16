plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.domain"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
            kotlin.srcDirs("src/main/java")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }
}

dependencies {
    // No direct dependencies on other modules for now, as per clean architecture principles
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.json)
}