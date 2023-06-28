plugins {
    id("java-library")
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("com.github.Haptic-Apps:JRAW:9c8a410a06")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")
}
