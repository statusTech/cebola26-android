plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.oitickets.cebola26"
    compileSdkPreview = "VanillaIceCream" // Ajuste para SDK 35/36 Preview se necessário, ou mantenha o seu original abaixo se estiver funcionando
    // Se o seu 'compileSdk { ... }' original estava funcionando, pode manter.
    // Caso dê erro de 'release(36)', troque por compileSdk = 35
    compileSdk = 35

    defaultConfig {
        applicationId = "com.oitickets.cebola26"
        minSdk = 31
        targetSdk = 35 // Ajustado para 35 (Estável atual) ou mantenha 36 se estiver usando preview
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
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- Dependências Originais (Mantidas) ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Firebase (Mantidos os seus alias, mas adicionei o BOM para garantir versões estáveis)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // --- NOVAS DEPENDÊNCIAS (Adicionadas para o Projeto) ---

    // 1. CameraX (Para capturar a foto e análise)
    val camerax_version = "1.4.0-alpha03" // Ou "1.3.1" estável
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")
    implementation("androidx.camera:camera-mlkit-vision:$camerax_version")
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.0")

    // 2. ML Kit (Para detectar o rosto e verificar qualidade)
    implementation("com.google.android.gms:play-services-mlkit-face-detection:17.1.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // 4. Coil (Para carregar imagens via URL/Bitmap no Compose de forma fácil)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // CORREÇÃO DO ERRO DA CÂMERA (ListenableFuture)
    implementation("com.google.guava:guava:31.1-android")

    // Recomendado para usar .await() no lugar de addListener (opcional, mas limpa o código)
    implementation("androidx.concurrent:concurrent-futures-ktx:1.1.0")


    // --- Testes ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}