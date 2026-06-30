plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // --- PLUGINS APLICADOS PARA LUMI ---
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.jeremy.lumi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jeremy.lumi"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ==========================================
    // DEPENDENCIAS AÑADIDAS PARA LA ARQUITECTURA
    // ==========================================

    // 1. Navegación en Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // 2. Room Database (Almacenamiento local privado)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // 3. Dagger Hilt (Inyección de Dependencias)
    implementation("com.google.dagger:hilt-android:2.51")
    ksp("com.google.dagger:hilt-android-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // 4. WorkManager (Para las alarmas de inyecciones a meses)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // 5. DataStore (Para guardar la paleta de colores preferida)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // 6. La wea del calendario
    implementation("androidx.compose.material:material-icons-extended")

    // 7. Firebase (Modo Pareja & Sync)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // 8. ZXing (Códigos QR)
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // 9. WheelPicker (Estilo iOS para fechas/horas)
    implementation("com.github.commandiron:WheelPickerCompose:1.1.11")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // 10. MotionLayout y ConstraintLayout para Compose
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0")

    // 11. Heart Switch (Modo Pareja/Intimidad)
    implementation("com.github.popovanton0:heart-switch:1.6.0")

    // 12. UI Premium (Lottie, Coil, Shimmer)
    implementation("com.airbnb.android:lottie-compose:6.4.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.valentinilk.shimmer:compose-shimmer:1.3.0")

    // 13. TensorFlow Lite (Inteligencia Artificial Local)
    implementation("org.tensorflow:tensorflow-lite:2.16.1")

    // 14. Health Connect (Sincronización de Salud)
    // NOTA P3-1: beta02 requiere compileSdk=36 (Android 16 preview), incompatible
    // con la configuración actual (compileSdk=35). Se mantiene alpha07 que compila
    // limpiamente. Actualizar cuando el proyecto migre a compileSdk 36.
    implementation("androidx.health.connect:connect-client:1.1.0-alpha07")
    
    // 15. Jetpack Glance (Widgets)
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")
}