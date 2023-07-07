// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }

    dependencies {
        classpath(libs.gradle.android)
        classpath(libs.gradle.kotlin.gradle)
        classpath(libs.gradle.kotlin.serialization)
        classpath(libs.gradle.hilt)
        classpath(libs.gradle.graphql)

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

plugins {
    alias(libs.plugins.dependencyAnalysis)
    //alias(libs.plugins.gradleDoctor)
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}
