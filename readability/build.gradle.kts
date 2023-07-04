plugins {
    id(libs.plugins.java.library.get().pluginId)
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    api(libs.jsoup)

    testImplementation(libs.junit)
}

group = "com.wu-man"
version = "1.4-SNAPSHOT"
description = "JReadability"
java.sourceCompatibility = JavaVersion.VERSION_17

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}
