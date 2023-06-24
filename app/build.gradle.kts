plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
    kotlin("plugin.parcelize")
    id("com.gladed.androidgitversion") version "0.4.14"
    id("com.github.ben-manes.versions") version "0.42.0"
}

android {
    compileSdk = 34
    buildToolsVersion = "30.0.3"

    defaultConfig {
        applicationId = "ltd.ucode.slide"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = androidGitVersion.name()

        multiDexEnabled = true
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

    lint {
        quiet = true
        abortOnError = false
        ignoreWarnings = true
    }

    lintOptions {
        // Translations are crowd-sourced
        disable("MissingTranslation")
        disable("ExtraTranslation")
        disable("StaticFieldLeak")
        disable("ClickableViewAccessibility")
        disable("NotSibling")
    }

    testOptions {
        unitTests {
            //includeAndroidResources = true
        }
    }

    buildFeatures {
        viewBinding = true
    }

    namespace = "ltd.ucode.slide"
}

dependencies {
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha03")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.1")

    implementation("androidx.core:core-ktx:1.10.1")
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

    /** Flavors **/

    /** Custom **/
    implementation("com.github.Haptic-Apps:JRAW:9c8a410a06")
    implementation("com.github.Haptic-Apps:TedBottomPicker:496623c9b6")
    val commonmarkVersion: String by rootProject.extra
    implementation("com.github.Haptic-Apps.commonmark-java:commonmark:$commonmarkVersion")
    implementation("com.github.Haptic-Apps.commonmark-java:commonmark-ext-gfm-strikethrough:$commonmarkVersion")
    implementation("com.github.Haptic-Apps.commonmark-java:commonmark-ext-gfm-tables:$commonmarkVersion")
    implementation("com.github.Haptic-Apps:JReadability:bb291880a5")
    implementation("com.github.Haptic-Apps.Android-RobotoTextView:robototextview:f6d0eb5ac7")

    /** AndroidX **/
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.browser:browser:1.3.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.core:core:1.10.1")
    implementation("androidx.fragment:fragment:1.6.0")
    implementation("androidx.media:media:1.3.1")
    implementation("androidx.multidex:multidex:2.0.1")
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


    /** Third-party **/

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
    implementation("com.nostra13.universalimageloader:universal-image-loader:1.9.5")

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
    implementation("com.squareup.okhttp3:okhttp:4.10.0")

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


    /** Testing **/
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.mockito:mockito-core:3.+")
    testImplementation("org.powermock:powermock-module-junit4:2.0.9")
    testImplementation("com.github.IvanShafran:shared-preferences-mock:1.0")
}
