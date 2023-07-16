plugins {
    id(libs.plugins.android.application.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.kotlin.kapt.get().pluginId)
    id(libs.plugins.kotlin.serialization.get().pluginId)
    id(libs.plugins.kotlin.parcelize.get().pluginId)
    id(libs.plugins.hilt.get().pluginId)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidGitVersion)
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

    defaultConfig {
        applicationId = "ltd.ucode.slide"
        minSdk = 26
        targetSdk = 33
        versionCode = 1

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
    kotlinOptions {
        jvmTarget = "17"
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
        buildConfig = true
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
    implementation(project(mapOf("path" to ":app:bottompicker")))
    implementation(project(mapOf("path" to ":app:crash")))
    implementation(project(mapOf("path" to ":app:data")))
    implementation(project(mapOf("path" to ":app:data:lemmy")))
    implementation(project(mapOf("path" to ":app:roboto")))
    implementation(project(mapOf("path" to ":app:util")))
    implementation(project(mapOf("path" to ":network")))
    implementation(project(mapOf("path" to ":network:lemmy")))
    implementation(project(mapOf("path" to ":network:reddit")))
    implementation(project(mapOf("path" to ":network:stats")))
    implementation(project(mapOf("path" to ":readability")))
    implementation(project(mapOf("path" to ":util")))

    coreLibraryDesugaring(libs.android.desugar)

    // Misc
    implementation(libs.jraw)
    implementation(libs.bundles.commonmark)
    implementation(libs.jsoup)

    // AndroidX
    implementation(libs.androidx.appcompat)
    implementation("androidx.browser:browser:1.3.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation(libs.androidx.core)
    implementation("androidx.fragment:fragment:1.6.0")
    implementation(libs.androidx.media)
    implementation(libs.androidx.recyclerview)
    implementation("androidx.webkit:webkit:1.4.0")
    implementation(libs.material)
    implementation(libs.kotlinx.coroutines.android)

    // AndroidX Kotlin
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
    implementation(libs.androidx.room.ktx)
    //ksp(libs.androidx.room.compiler)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.sqlite)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation(libs.androidx.security.crypto)
    implementation("androidx.paging:paging-common-ktx:3.1.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Application level media player
    implementation(libs.bundles.exoplayer)

    // Logging
    implementation(libs.kotlin.logging)
    implementation(libs.bundles.slf4j)

    // Custom dialogs
    implementation(libs.bundles.materialDialogs)
    implementation(libs.materialProgressBar)

    // Snackbar engagement for rating
    implementation("com.github.ligi.snackengage:snackengage-core:0.29")
    implementation("com.github.ligi.snackengage:snackengage-playrate:0.29")

    // Material design components for pre-Lollipop APIs
    //  TODO: Replace rey5137:material with AndroidX versions?
    implementation("com.github.rey5137:material:1.3.1")

    // Image loading, caching, and displaying
    //  TODO: Replace with Glide/Picasso
    implementation("com.github.nostra13:android-universal-image-loader:458df4da2e23ba9ad76c79241a948cdfcccf72ae")
    implementation(libs.glide)
    ksp(libs.glide.ksp)

    // Custom image view for photo galleries and large images
    //implementation("com.github.davemorrissey:subsampling-scale-image-view:173e421")
    implementation("com.github.KotatsuApp:subsampling-scale-image-view:1b19231b2f")

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
    //implementation("com.sothree.slidinguppanel:library:3.4.0")
    implementation("com.github.hannesa2:AndroidSlidingUpPanel:4.5.0")

    // ViewAnimationUtils.createCircularReveal for pre-Lollipop APIs
    implementation("com.github.ozodrukh:CircularReveal:2.0.1@aar")

    // RecyclerView animations
    implementation("com.mikepenz:itemanimators:1.1.0@aar")

    // iOS-like over-scrolling effect
    implementation("io.github.everythingme:overscroll-decor-android:1.1.1")

    // Library information
    implementation("com.mikepenz:aboutlibraries:6.2.3")

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
    //implementation("com.github.suckgamony.RapidDecoder:library:7cdfca47fa")
    implementation("com.github.raulhaag.RapidDecoder:library:0.3.1Ax")

    // HTTP client
    implementation(libs.okhttp)

    // Convert Java objects into JSON and back
    implementation(libs.gson)
    implementation(libs.bundles.retrofit)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.datetime)

    // WebSocket client
    implementation("com.neovisionaries:nv-websocket-client:2.14")

    // Read, write, and create MP4 files
    implementation(libs.bundles.mp4parser)

    // Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.androidx.common)
    kapt(libs.hilt.androidx.compiler)

    // Databinding
    kapt("com.android.databinding:compiler:3.1.4")

    // Markdown
    implementation(libs.markdown)

    // Crash Reporting
    implementation(libs.bundles.acra)

    // YouTube
    implementation(libs.newpipe.extractor)

    //// Testing
    androidTestImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test)

    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.mockito:mockito-core:3.12.4")
    testImplementation("org.powermock:powermock-module-junit4:2.0.9")

    androidTestImplementation(libs.junit)
    testImplementation(libs.junit)

    androidTestImplementation(libs.robolectric)

    // To use the androidx.test.core APIs
    androidTestImplementation(libs.androidx.test.core)
    // Kotlin extensions for androidx.test.core
    androidTestImplementation(libs.androidx.test.core.ktx)

    // To use the androidx.test.espresso
    androidTestImplementation(libs.androidx.test.espresso)

    // To use the JUnit Extension APIs
    androidTestImplementation(libs.androidx.test.junit)
    // Kotlin extensions for androidx.test.ext.junit
    androidTestImplementation(libs.androidx.test.junit.ktx)

    // To use the Truth Extension APIs
    androidTestImplementation(libs.androidx.test.truth)

    // To use the androidx.test.runner APIs
    androidTestImplementation(libs.androidx.test.runner)

    // To use android test orchestrator
    androidTestUtil(libs.androidx.test.orchestrator)
}
