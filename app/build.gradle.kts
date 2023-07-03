plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization")
    kotlin("plugin.parcelize")
    id("dagger.hilt.android.plugin")
    alias(libs.plugins.androidgitversion)
    id("com.github.ben-manes.versions") version "0.42.0"
}

androidGitVersion {
    codeFormat = "MMMNNNPPP"
    commitHashLength = 8
    format = "%tag%%-count%%-commit%%-branch%%-dirty%"
    hideBranches = listOf("master", "lemmy")
    untrackedIsDirty = false
}

android {
    compileSdk = 34
    buildToolsVersion = "30.0.3"

    defaultConfig {
        applicationId = "ltd.ucode.slide"
        minSdk = 23
        targetSdk = 33
        versionCode = 1
        versionName = androidGitVersion.name()

        vectorDrawables.useSupportLibrary = true
    }

    flavorDimensions.add("variant")
    productFlavors {
        create("libre") {
            dimension = "variant"
            buildConfigField("boolean", "isFDroid", "true")
        }
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"))
            resValue("string", "app_name", "Slide")
        }
        named("debug") {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Slide Debug")
        }
    }
    packaging {
        jniLibs {
            excludes += listOf("META-INF/*")
        }
        resources {
            excludes += listOf("META-INF/*")
        }
    }

    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    kapt {
        correctErrorTypes = true
    }

    lint {
        quiet = true
        abortOnError = false
        ignoreWarnings = true

        // Translations are crowd-sourced
        disable += "MissingTranslation"
        disable += "ExtraTranslation"
        disable += "StaticFieldLeak"
        disable += "ClickableViewAccessibility"
        disable += "NotSibling"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    if (project.hasProperty("RELEASE_STORE_FILE")) {
        signingConfigs {
            create("release") {
                storeFile = file(project.properties["RELEASE_STORE_FILE"].toString())
                storePassword = project.properties["RELEASE_STORE_PASSWORD"].toString()
                keyAlias = "slide"
                keyPassword = project.properties["RELEASE_KEY_PASSWORD"].toString()
            }
        }

        buildTypes["release"].signingConfig = signingConfigs["release"]
    } else {
        logger.lifecycle("No signing keys!")
    }

    namespace = "ltd.ucode.slide"
}

dependencies {
    implementation(project(mapOf("path" to ":app:crash")))
    implementation(project(mapOf("path" to ":app:util")))
    implementation(project(mapOf("path" to ":data")))
    implementation(project(mapOf("path" to ":data:lemmy")))
    implementation(project(mapOf("path" to ":data:reddit")))
    implementation(project(mapOf("path" to ":stats")))
    implementation(project(mapOf("path" to ":util")))

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.1")

    implementation("androidx.collection:collection-ktx:1.2.0")
    implementation("androidx.fragment:fragment-ktx:1.6.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("androidx.navigation:navigation-runtime-ktx:2.6.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.6.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.6.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.lifecycle:lifecycle-reactivestreams-ktx:2.6.1")
    implementation("androidx.room:room-ktx:2.5.1")
    implementation("androidx.sqlite:sqlite-ktx:2.3.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha03")

    /** Flavors **/

    /** Custom **/
    implementation(libs.jraw)
    implementation(project(mapOf("path" to ":app:bottompickerv2")))
    implementation(project(mapOf("path" to ":readability")))
    implementation(libs.bundles.commonmark)
    implementation(project(mapOf("path" to ":readability")))
    implementation(libs.jsoup)
    implementation(project(mapOf("path" to ":app:roboto")))

    /** AndroidX **/
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.browser:browser:1.3.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.core:core:1.10.1")
    implementation("androidx.fragment:fragment:1.6.0")
    implementation("androidx.media:media:1.3.1")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.webkit:webkit:1.4.0")
    implementation("com.google.android.material:material:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    /** ExoPlayer **/
    // Application level media player
    // Cannot update beyond this point: extension"s minimum SDK version is 21 in 2.15.0 and above
    val exoPlayerVersion: String by rootProject.extra
    implementation("com.google.android.exoplayer:exoplayer-core:$exoPlayerVersion")
    implementation("com.google.android.exoplayer:exoplayer-dash:$exoPlayerVersion")
    implementation("com.google.android.exoplayer:exoplayer-ui:$exoPlayerVersion")
    implementation("com.google.android.exoplayer:extension-okhttp:$exoPlayerVersion")


    /** Logging **/
    implementation("io.github.oshai:kotlin-logging-jvm:4.0.0")
    implementation(libs.bundles.slf4j)

    /**** Frontend (UI-related) ****/
    // Custom dialogs
    implementation("com.afollestad.material-dialogs:commons:0.9.6.0")

    // Snackbar engagement for rating
    implementation("com.github.ligi.snackengage:snackengage-core:0.29")
    implementation("com.github.ligi.snackengage:snackengage-playrate:0.29")

    // Material design components for pre-Lollipop APIs
    //  NOTE: Replace rey5137:material with AndroidX versions?
    implementation("com.github.rey5137:material:1.3.1")

    // Image loading, caching, and displaying
    //  NOTE: Replace with Glide/Picasso
    implementation("com.github.nostra13:android-universal-image-loader:458df4da2e23ba9ad76c79241a948cdfcccf72ae")

    // Custom image view for photo galleries and large images
    implementation("com.github.davemorrissey:subsampling-scale-image-view:173e421")

    // Image cropping
    implementation("com.github.CanHub:Android-Image-Cropper:3.2.2")

    // Bottom sheet implementation
    //  NOTE: Deprecated in favor of official Google bottom sheets
    implementation("com.cocosw:bottomsheet:1.5.0@aar")

    // Blurring
    implementation("com.github.wasabeef:Blurry:3.0.0")

    // ImageView that supports rounded corners
    implementation("com.makeramen:roundedimageview:2.3.0")

    // Floating action button menu implementation
    implementation("com.nambimobile.widgets:expandable-fab:1.2.1")

    // Draggable sliding up panel
    implementation("com.sothree.slidinguppanel:library:3.4.0")

    // ViewAnimationUtils.createCircularReveal for pre-Lollipop APIs
    implementation("com.github.ozodrukh:CircularReveal:2.0.1@aar")

    // RecyclerView animations
    implementation("com.mikepenz:itemanimators:1.1.0@aar")

    // iOS-like over-scrolling effect
    implementation("io.github.everythingme:overscroll-decor-android:1.1.1")

    // Library information
    implementation("com.mikepenz:aboutlibraries:6.2.3")


    /**** Backend logic ****/

    // Core Java libraries from Google
    implementation("com.google.guava:guava:31.0.1-android")

    // Application restarting
    implementation("com.jakewharton:process-phoenix:2.1.2")

    // KV store based on SQLite
    //  equal to 0.1.0, but we can"t use jcenter
    implementation("com.github.lusfold:AndroidKeyValueStore:620c363")

    // Helper utilities for the java.lang API
    implementation("org.apache.commons:commons-lang3:3.12.0")

    // Algorithms working on strings
    implementation("org.apache.commons:commons-text:1.9")

    // Utilities to assist with developing IO functionality
    implementation("commons-io:commons-io:2.11.0")

    // Simplified bitmap decoding and scaling
    implementation("com.github.suckgamony.RapidDecoder:library:7cdfca47fa")

    // HTTP client
    val okhttpVersion: String by rootProject.extra
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")

    // Convert Java objects into JSON and back
    val retrofitVersion: String by rootProject.extra
    val serializationVersion: String by rootProject.extra
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.squareup.retrofit2:retrofit:${retrofitVersion}")
    implementation("com.nightlynexus.logging-retrofit:logging:0.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${serializationVersion}")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")

    // WebSocket client
    implementation("com.neovisionaries:nv-websocket-client:2.14")

    // Read, write, and create MP4 files
    implementation("org.mp4parser:isoparser:1.9.41")
    implementation("org.mp4parser:muxer:1.9.41")

    // Dependency Injection
    val hiltVersion: String by rootProject.extra
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-common:1.0.0-alpha03")
    kapt("androidx.hilt:hilt-compiler:1.0.0-alpha03")

    // Databinding
    kapt("com.android.databinding:compiler:3.1.4")

    // Markdown
    implementation(libs.markdown)

    // Crash Reporting
    implementation(libs.bundles.acra)


    /** Testing **/
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.mockito:mockito-core:3.12.4")
    testImplementation("org.powermock:powermock-module-junit4:2.0.9")
    testImplementation("com.github.IvanShafran:shared-preferences-mock:1.0")
    testImplementation("org.robolectric:robolectric:4.10.3")

    // To use the androidx.test.core APIs
    androidTestImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test:core:1.5.0")
    // Kotlin extensions for androidx.test.core
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("androidx.core:core-ktx:1.5.0")

    // To use the androidx.test.espresso
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    testImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // To use the JUnit Extension APIs
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.test.ext:junit:1.1.5")
    // Kotlin extensions for androidx.test.ext.junit
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")
    testImplementation("androidx.test.ext:junit-ktx:1.1.5")

    // To use the Truth Extension APIs
    androidTestImplementation("androidx.test.ext:truth:1.5.0")
    testImplementation("androidx.test.ext:truth:1.5.0")

    // To use the androidx.test.runner APIs
    androidTestImplementation("androidx.test:runner:1.5.2")
    testImplementation("androidx.test:runner:1.5.2")

    // To use android test orchestrator
    androidTestUtil("androidx.test:orchestrator:1.4.2")
}
