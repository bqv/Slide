plugins {
    id(libs.plugins.java.library.get().pluginId)
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.kotlin.kapt.get().pluginId)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlin.reflect)

    implementation(libs.ksp.symbolProcessing)

    implementation(libs.autoService.annotations)
    kapt(libs.autoService.ksp)
}
