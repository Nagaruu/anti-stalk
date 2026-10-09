plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.antistalk"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.antistalk"
        minSdk = 26
        targetSdk = 35
        // BUILD_NUMBER comes from CI: tag number for releases (vN), run number otherwise.
        val buildNumber = System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: 1
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"
    }

    signingConfigs {
        getByName("debug") {
            val ks = rootProject.file("keystore/debug.keystore")
            if (ks.exists()) {
                storeFile = ks
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // Play-safe split: "play" build has no self-update code path (see IS_PLAY
    // guards + src/sideload manifest for REQUEST_INSTALL_PACKAGES).
    // Sideload keeps GitHub-Releases updater; Play uses Play update mechanism.
    flavorDimensions += "dist"
    productFlavors {
        create("sideload") {
            dimension = "dist"
            buildConfigField("boolean", "IS_PLAY", "false")
        }
        create("play") {
            dimension = "dist"
            buildConfigField("boolean", "IS_PLAY", "true")
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
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.7")

    testImplementation("junit:junit:4.13.2")
}
