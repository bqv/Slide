plugins {
    id(libs.plugins.java.library.get().pluginId)
    id(libs.plugins.kotlin.jvm.get().pluginId)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api(project(mapOf("path" to ":network:common")))
    api(project(mapOf("path" to ":network:lemmy")))
    api(project(mapOf("path" to ":network:reddit")))
    api(project(mapOf("path" to ":network:stats")))
    implementation(project(mapOf("path" to ":util")))
}
