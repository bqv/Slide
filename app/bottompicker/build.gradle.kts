plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
}

android {
    namespace = "gun0912.tedbottompicker"
    compileSdk = 30

    defaultConfig {
        minSdk = 16

        version = "1.0"
    }
    lint {
        abortOnError = false
    }
    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Android support
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)

    // Third-party
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.tedonactivityresult)
    implementation(libs.rxjava)
}
