plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("com.google.dagger.hilt.android")
}
kapt {
    correctErrorTypes = true
}

android {
    namespace = "com.example.lango_mvp_android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.lango_mvp_android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperties["robolectric.logging.enabled"] = "true"
            it.jvmArgs(
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
                "--add-opens=java.prefs/java.util.prefs=ALL-UNNAMED",
                "-Dnet.bytebuddy.experimental=true"
            )
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.agent.jvm)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.androidx.test.core)
    testImplementation(project(":shared-test"))

    //Project modules
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":speech"))

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
