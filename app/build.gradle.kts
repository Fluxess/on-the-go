import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        // Strip UTF-8 BOM if present (PowerShell Set-Content often adds it).
        val text = keystorePropertiesFile.readText(Charsets.UTF_8).removePrefix("\uFEFF")
        load(text.reader())
    }
}

android {
    namespace = "ru.routealert.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.routealert.app"
        minSdk = 27
        targetSdk = 34
        versionCode = 3
        versionName = "1.0.2"

        val mapKitKey = localProperties.getProperty("YANDEX_MAPKIT_API_KEY", "YOUR_YANDEX_MAPKIT_API_KEY")
        buildConfigField("String", "YANDEX_MAPKIT_API_KEY", "\"$mapKitKey\"")
    }

    signingConfigs {
        create("release") {
            check(keystorePropertiesFile.exists()) {
                "Missing keystore.properties. Copy keystore.properties.example and fill passwords."
            }
            val storeFilePath = keystoreProperties.getProperty("storeFile")
                ?: error("keystore.properties: storeFile is missing")
            val resolved = rootProject.file(storeFilePath)
            check(resolved.isFile) { "Keystore file not found: ${resolved.absolutePath}" }
            storeFile = resolved
            storePassword = keystoreProperties.getProperty("storePassword")
                ?: error("keystore.properties: storePassword is missing")
            keyAlias = keystoreProperties.getProperty("keyAlias")
                ?: error("keystore.properties: keyAlias is missing")
            keyPassword = keystoreProperties.getProperty("keyPassword")
                ?: error("keystore.properties: keyPassword is missing")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation("com.yandex.android:maps.mobile:4.6.1-full")

    implementation("com.google.zxing:core:3.5.3")

    implementation("androidx.work:work-runtime-ktx:2.9.1")

    testImplementation("junit:junit:4.13.2")
}
