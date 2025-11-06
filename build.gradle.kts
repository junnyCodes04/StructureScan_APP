// Top-level build.gradle.kts (Project level)

// Declare plugins used by modules, but don't apply them here
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    // ✅ Add the Google Services Gradle plugin (needed for Firebase)
    id("com.google.gms.google-services") version "4.4.2" apply false

    // (Optional) Crashlytics plugin if you'll use Firebase Crashlytics later
     id("com.google.firebase.crashlytics") version "3.0.2" apply false
}