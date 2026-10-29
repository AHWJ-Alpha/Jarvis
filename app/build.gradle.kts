plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.jarvis.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jarvis.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 17
        versionName = "0.0.17"
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

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.compose.ui:ui:1.7.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.4")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.4")
    // Use the Play services-backed recognizer so the OCR pipeline is not packaged
    // into the APK. The bundled ML Kit artifact ships an ELF that is not 16 KB
    // page-size aligned on Android 15+ devices.
    implementation("com.google.android.gms:play-services-mlkit-text-recognition-chinese:16.0.1")

    // Jarvis does not use Compose path effects. Excluding this optional native
    // library avoids shipping the unaligned graphics-path ELF on 16 KB devices.
    configurations.configureEach {
        exclude(group = "androidx.graphics", module = "graphics-path")
    }
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.4")
}
