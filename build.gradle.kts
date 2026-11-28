plugins {
    alias(libs.plugins.android.application) apply false
    // No kotlin-android: AGP 9 compiles Kotlin built-in and rejects the KGP plugin.
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.roborazzi) apply false
}
