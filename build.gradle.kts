plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0" apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false

    /* NEW ↓ — Hilt only registered, not yet applied in any module */
    id("com.google.dagger.hilt.android")                            version "2.56.1" apply false
}