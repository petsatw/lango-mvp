plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.testing"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
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
    implementation(project(":domain"))
    implementation(libs.junit)
    implementation(libs.mockk)
    implementation(libs.mockk.agent.jvm)
    implementation(libs.kotlinx.coroutines.test)
}