import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.garrietta.haven"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.garrietta.haven"
        minSdk = 26
        // Held at 35 on purpose: 36 changes runtime behaviour (predictive
        // back, large-screen orientation rules) that needs a device pass on
        // the Fold before it ships. compileSdk moves freely; this does not.
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
        // Where updates come from, and which CI run built this APK (0 for a
        // local build): a test build is newer when its number is higher.
        buildConfigField("String", "GITHUB_REPO", "\"Garrietta/Haven\"")
        buildConfigField("int", "BUILD_NUMBER", (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0).toString())
    }

    signingConfigs {
        // Release signing comes from the environment (CI repo secrets).
        // When KEYSTORE_PATH is absent (local builds, pull requests), no
        // signing config is applied and release builds stay unsigned
        // instead of failing.
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        debug {
            // Installs beside the release app, so a test build never
            // replaces the signed-in one on the phone.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // Not minified yet: WebRTC (JNI), SnakeYAML (reflection) and the
            // Device Controls service all need keep rules that have to be
            // proven on a device first.
            isMinifyEnabled = false
            if (System.getenv("KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Screenshot tests render real resources (fonts, the launcher
            // mark) through Robolectric.
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                it.maxHeapSize = "2g"
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.icons.core)
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.material3)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.compose.activity)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.graphics.shapes)
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.rtsp)
    implementation(libs.media3.ui)
    implementation(libs.okhttp)
    implementation(libs.snakeyaml)
    implementation(libs.webrtc)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
