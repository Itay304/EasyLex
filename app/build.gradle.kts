plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.easylex"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.easylex"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "1.1"

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
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true // השורה שצריך להוסיף
    }
}

dependencies {

    // --- ספריות בסיס של אנדרואיד (AndroidX) ---
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // --- ספריות ארכיטקטורה (MVVM) ---
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    // --- בסיס נתונים מקומי (Room) ---
    implementation("androidx.room:room-runtime:2.6.1")
    implementation(libs.activity)
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // --- שירותי Google ו-Firebase ---
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.mlkit:translate:17.0.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")// --- CameraX ---

    val camerax_version = "1.3.1"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")
    implementation("com.google.guava:guava:31.1-android")

    // --- זיהוי טקסט (OCR) ---
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // --- ספריות לבדיקות ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}