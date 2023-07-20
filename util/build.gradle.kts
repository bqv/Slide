plugins {
    id(libs.plugins.java.library.get().pluginId)
    id(libs.plugins.kotlin.jvm.get().pluginId)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api(project(mapOf("path" to ":util:ksp")))

    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.datetime)

    implementation(libs.markdown)
    implementation(libs.jsoup)
}
