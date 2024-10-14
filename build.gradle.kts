// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
}

buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.6.1") // O una versión superior
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.44")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.8.2") // Versión actualizada
    }
}
