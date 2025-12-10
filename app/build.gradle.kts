plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.trackify"
    compileSdk = 34   // خليه 34 أو 35، تأكدي إنه موجود عندك في SDK Manager

    defaultConfig {
        applicationId = "com.example.trackify"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // مشروعكم جافا → هذا ممتاز
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.activity:activity:1.9.3")

    implementation("com.google.android.gms:play-services-auth:20.7.0")
    // ========== Firebase (باستخدام BoM) ==========
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

    // Auth (إيميل/باسوورد + Google Sign-In مع Firebase)
    implementation("com.google.firebase:firebase-auth")

    // Realtime Database (اللي شهد أضافته)
    implementation("com.google.firebase:firebase-database")

    // Google Sign-In (مهم لزر Google)
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // ========== Test ==========
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
