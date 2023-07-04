plugins {
    id("java-library")
    kotlin("jvm")
    kotlin("plugin.serialization")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(mapOf("path" to ":data")))

    implementation("com.github.Haptic-Apps:JRAW:9c8a410a06")

    implementation("com.squareup.okhttp3:okhttp:${libs.versions.okhttp}")

    implementation("com.squareup.retrofit2:retrofit:${libs.versions.retrofit}")
    implementation("com.nightlynexus.logging-retrofit:logging:0.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${libs.versions.serialization}")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")
    implementation("com.google.code.gson:gson:2.8.9")
}
