// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false

    // Make sure that you have the Google services Gradle plugin dependency
    id("com.google.gms.google-services") version "4.4.2" apply false

    // Add the dependency for the Crashlytics Gradle plugin
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
}

buildscript {
    dependencies {
        classpath(libs.gradle) // O una versión superior
        classpath(libs.hilt.android.gradle.plugin)
        classpath(libs.androidx.navigation.safe.args.gradle.plugin) // Versión actualizada

        classpath("com.google.gms:google-services:4.4.2") // Google Services plugin
        classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.2") // Firebase Crashlytics plugin
    }
}