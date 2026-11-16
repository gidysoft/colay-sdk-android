import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The key and server URL are never hard-coded: they are read from local.properties (not committed).
//   colay.key=colay_pk_...            (Dashboard → Ads → Tracking setup → Android source)
//   colay.baseUrl=https://colayai.com (use http://10.0.2.2:5050 for a local server from the emulator)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun prop(name: String, def: String) = (localProps.getProperty(name) ?: def).replace("\"", "")

android {
    namespace = "com.colayai.example"
    compileSdk = 35

    defaultConfig {
        // Enter this package name as the "app ID" of the tracking source; the server verifies it.
        applicationId = "com.colayai.example"
        minSdk = 23  // the example uses Compose; the library itself supports minSdk 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "COLAY_KEY", "\"${prop("colay.key", "colay_pk_YOUR_KEY")}\"")
        buildConfigField("String", "COLAY_BASE_URL", "\"${prop("colay.baseUrl", "https://colayai.com")}\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    // In your own app: implementation("com.github.gidysoft:colay-sdk-android:<version>")
    implementation(project(":colay"))

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
