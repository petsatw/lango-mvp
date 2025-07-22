plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
}
kapt {
    correctErrorTypes = true
}

android {
    namespace = "com.example.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
            kotlin.srcDirs("src/main/java")
        }
        getByName("test") {
            java.srcDirs("src/test/java")
            kotlin.srcDirs("src/test/java")
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperties["robolectric.logging.enabled"] = "true"
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnit()
    testLogging { events("standard_out", "standard_error") }
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.kotlinx.serialization.json)
    

    // Test dependencies
    testImplementation(libs.junit)
    testImplementation(libs.robolectric) // for FS-1 and FS-2
    testImplementation(libs.assertj.core)
    testImplementation(libs.kotlinx.coroutines.test)
    
    testImplementation(libs.mockito.core)
    testImplementation(libs.androidx.test.core)
    testImplementation(project(":domain"))
    testImplementation(project(":shared-test"))
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.agent.jvm)
    testImplementation("com.google.jimfs:jimfs:1.3.0")
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}