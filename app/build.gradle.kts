plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.example.adminobr"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.adminobr"
        minSdk = 24
        targetSdk = 34
        versionCode = 12
        versionName = "1.0.12"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

}

dependencies {

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Material Design
    implementation(libs.material)

    // AndroidX Components
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.ui.text.android)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.core.i18n)
    implementation(libs.androidx.datastore.core.android)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidthings)

    // Filament y Protobuf
    implementation(libs.filament.android)
    implementation(libs.protolite.well.known.types)

    // Retrofit y Gson para solicitudes de red
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.kotlinx.coroutines.core) // Asegúrate de tener la última versión

    // Otros
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.work.runtime.ktx) // Versión actual
    implementation(libs.jbcrypt)
    implementation(libs.androidx.gridlayout)

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation ("com.google.android.flexbox:flexbox:3.0.0")
    implementation ("androidx.gridlayout:gridlayout:1.0.0")


}

