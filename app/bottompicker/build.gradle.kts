plugins {
    id(libs.plugins.android.library.get().pluginId)
}

android {
    namespace = "gun0912.tedbottompicker"
    compileSdk = 28

    defaultConfig {
        minSdk = 16

        version = "1.0"
    }
    lint {
        abortOnError = false
    }
    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
