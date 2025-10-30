plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Required for Kotlin 2.0+ Compose
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.kernelmanager"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.kernelmanager"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // Optional release signing through properties or environment variables
    signingConfigs {
        create("release") {
            val storeFilePath = (project.findProperty("RELEASE_STORE_FILE") as String?)
                ?: System.getenv("RELEASE_STORE_FILE")
            val storePass = (project.findProperty("RELEASE_STORE_PASSWORD") as String?)
                ?: System.getenv("RELEASE_STORE_PASSWORD")
            val keyAliasProp = (project.findProperty("RELEASE_KEY_ALIAS") as String?)
                ?: System.getenv("RELEASE_KEY_ALIAS")
            val keyPass = (project.findProperty("RELEASE_KEY_PASSWORD") as String?)
                ?: System.getenv("RELEASE_KEY_PASSWORD")

            if (!storeFilePath.isNullOrBlank() && !storePass.isNullOrBlank() && !keyAliasProp.isNullOrBlank() && !keyPass.isNullOrBlank()) {
                storeFile = file(storeFilePath)
                storePassword = storePass
                keyAlias = keyAliasProp
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use release signing if fully configured; otherwise fall back to debug signing
            val rel = signingConfigs.getByName("release")
            val hasReleaseSigning = (rel.storeFile != null) &&
                    !rel.storePassword.isNullOrEmpty() &&
                    !rel.keyAlias.isNullOrEmpty() &&
                    !rel.keyPassword.isNullOrEmpty()
            signingConfig = if (hasReleaseSigning) rel else signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.8.3")

    // Coroutines for background polling
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Root access (libsu) for CPU tuning
    implementation("com.github.topjohnwu:libsu:6.0.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
