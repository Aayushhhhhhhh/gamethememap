plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.overworld"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.overworld.drive"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Geoapify key: pass -PGEOAPIFY_KEY=... or set the GEOAPIFY_KEY env var (GitHub secret in CI).
        val geoKey = (project.findProperty("GEOAPIFY_KEY") as String?)
            ?: System.getenv("GEOAPIFY_KEY")
            ?: ""
        buildConfigField("String", "GEOAPIFY_KEY", "\"$geoKey\"")
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Map engine (open source) - tiles come from OpenFreeMap.
    implementation("org.maplibre.gl:android-sdk:11.5.2")

    // Android Auto (car screen)
    implementation("androidx.car.app:app:1.4.0")
}
