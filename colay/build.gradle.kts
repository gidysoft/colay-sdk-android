plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    `maven-publish`
}

android {
    namespace = "com.colayai.sdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "SDK_VERSION", "\"0.1.2\"")
    }

    buildFeatures { buildConfig = true }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }

    testOptions { unitTests.isReturnDefaultValues = true }

    // JitPack / Maven publishing: the release variant is published together with sources
    publishing {
        singleVariant("release") { withSourcesJar() }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.gidysoft"
                artifactId = "colay-sdk-android"
                version = "0.1.2"
                pom {
                    name.set("Colay AI Android SDK")
                    description.set("Colay AI conversion tracking: install attribution, screens, events, purchases and consent management.")
                    url.set("https://github.com/gidysoft/colay-sdk-android")
                    licenses { license { name.set("MIT"); url.set("https://opensource.org/licenses/MIT") } }
                }
            }
        }
    }
}

dependencies {
    // Play Store install referrer (which link or ad led to the install)
    implementation("com.android.installreferrer:installreferrer:2.2")
    // The advertising ID is optional: read via reflection if the app adds play-services-ads-identifier.

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
