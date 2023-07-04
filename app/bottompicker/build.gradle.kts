plugins {
    id("com.android.library")
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
    implementation("androidx.annotation:annotation:1.3.0")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("com.google.android.material:material:1.4.0")

    // Third-party
    var glideVersion = "4.12.0"
    var rxjavaVersion = "2.2.21"
    implementation("com.github.bumptech.glide:glide:$glideVersion")
    annotationProcessor("com.github.bumptech.glide:compiler:$glideVersion")
    implementation("io.github.ParkSangGwon:tedonactivityresult:1.0.10")
    implementation("io.reactivex.rxjava2:rxjava:$rxjavaVersion")
}
