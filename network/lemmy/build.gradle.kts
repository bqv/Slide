plugins {
    id(libs.plugins.java.library.get().pluginId)
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.kotlin.serialization.get().pluginId)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(mapOf("path" to ":network")))
    implementation(project(mapOf("path" to ":util")))

    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlin.logging)
    implementation(libs.jraw)

    implementation(libs.okhttp)

    implementation(libs.bundles.retrofit)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.datetime)

    implementation(libs.markdown)
    implementation(libs.jsoup)

    testImplementation(libs.testng)
    testImplementation(libs.gson)
}
