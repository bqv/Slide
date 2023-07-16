plugins {
    id(libs.plugins.java.library.get().pluginId)
    id(libs.plugins.scala.library.get().pluginId)
    //id(libs.plugins.scala.android.get().pluginId)
}

dependencies {
    implementation(libs.scala.core)
    api(libs.jsoup)

    testImplementation(libs.scala.test.core)
    testImplementation(libs.scala.test.junit)
    //testImplementation(libs.junit)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}
