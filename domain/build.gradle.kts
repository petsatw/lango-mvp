plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}
kapt {
    correctErrorTypes = true
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
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.agent.jvm)
    testImplementation(libs.truth)
    testImplementation(project(":shared-test"))
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}