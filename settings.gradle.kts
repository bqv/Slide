dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            // Classpaths
            val gradle = version("gradle", "8.0.2")
            val kotlin = version("kotlin", "1.8.22")
            val graphql = version("graphql", "6.5.3")
            val ktor = version("ktor", "2.3.1")
            val hilt = version("hilt", "2.46.1")
            library("gradle-android", "com.android.tools.build", "gradle").versionRef(gradle)
            library("gradle-kotlin-gradle", "org.jetbrains.kotlin", "kotlin-gradle-plugin").versionRef(kotlin)
            library("gradle-kotlin-serialization", "org.jetbrains.kotlin", "kotlin-serialization").versionRef(kotlin)
            library("gradle-hilt", "com.google.dagger", "hilt-android-gradle-plugin").versionRef(hilt)
            library("gradle-graphql", "com.expediagroup", "graphql-kotlin-gradle-plugin").versionRef(graphql)

            // Plugins
            plugin("java-library", "org.gradle.java-library").version("")
            plugin("kotlin-dsl", "org.gradle.kotlin.kotlin-dsl").version("")
            plugin("android-application", "com.android.application").version("")
            plugin("android-library", "com.android.library").version("")
            plugin("graphql", "com.expediagroup.graphql").version("")

            plugin("kotlin-android", "org.jetbrains.kotlin.android").versionRef(kotlin)
            plugin("kotlin-kapt", "org.jetbrains.kotlin.kapt").versionRef(kotlin)
            plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm").versionRef(kotlin)
            plugin("kotlin-serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef(kotlin)
            plugin("kotlin-parcelize", "org.jetbrains.kotlin.plugin.parcelize").versionRef(kotlin)

            plugin("androidGitVersion", "com.gladed.androidgitversion").version("0.4.14")
            plugin("hilt", "dagger.hilt.android.plugin").versionRef(hilt)
            plugin("ksp", "com.google.devtools.ksp").version("1.8.22-1.0.11")

            plugin("androidCacheFix", "org.gradle.android.cache-fix").version("2.7.2")
            plugin("gradleDoctor", "com.osacky.doctor").version("0.8.1")
            plugin("dependencyAnalysis", "com.autonomousapps.dependency-analysis").version("1.20.0")

            // Dependencies
            val acra = version("acra", "5.10.1")
            library("acra-sender-http", "ch.acra", "acra-http").versionRef(acra)
            library("acra-sender-mail", "ch.acra", "acra-mail").versionRef(acra)
            library("acra-sender-custom", "ch.acra", "acra-core").versionRef(acra)
            library("acra-interaction-dialog", "ch.acra", "acra-dialog").versionRef(acra)
            library("acra-interaction-notification", "ch.acra", "acra-notification").versionRef(acra)
            library("acra-interaction-toast", "ch.acra", "acra-toast").versionRef(acra)
            library("acra-limiter", "ch.acra", "acra-limiter").versionRef(acra)
            library("acra-scheduler", "ch.acra", "acra-advanced-scheduler").versionRef(acra)
            bundle("acra", listOf(
                "acra-sender-http", "acra-sender-mail", "acra-sender-custom",
                "acra-interaction-dialog", "acra-interaction-notification", "acra-interaction-toast",
                "acra-limiter", "acra-scheduler"))

            library("android-desugar", "com.android.tools:desugar_jdk_libs:1.1.1")

            val room = version("room", "2.5.1")
            library("androidx-core", "androidx.core:core:1.10.1")
            library("androidx-core-ktx", "androidx.core:core-ktx:1.8.0")
            library("androidx-annotation", "androidx.annotation:annotation:1.3.0")
            library("androidx-appcompat", "androidx.appcompat:appcompat:1.3.1")
            library("androidx-recyclerview", "androidx.recyclerview:recyclerview:1.2.1")
            library("androidx-media", "androidx.media:media:1.6.0")
            library("androidx-room-ktx", "androidx.room", "room-ktx").versionRef(room)
            library("androidx-room-compiler", "androidx.room", "room-compiler").versionRef(room)
            library("androidx-room-paging", "androidx.room", "room-paging").versionRef(room)
            library("androidx-sqlite", "androidx.sqlite", "sqlite-ktx").version("2.3.1")
            library("androidx-test-core", "androidx.test:core:1.5.0")
            library("androidx-test-core-ktx", "androidx.test:core-ktx:1.5.0")
            library("androidx-test-junit", "androidx.test.ext:junit:1.1.5")
            library("androidx-test-junit-ktx", "androidx.test.ext:junit-ktx:1.1.5")
            library("androidx-test-espresso", "androidx.test.espresso:espresso-core:3.5.1")
            library("androidx-test-truth", "androidx.test.ext:truth:1.5.0")
            library("androidx-test-runner", "androidx.test:runner:1.5.2")
            library("androidx-test-orchestrator", "androidx.test:orchestrator:1.4.2")

            val autoDsl = version("autoDsl", "2.2.10")
            library("autoDsl-processor", "com.faendir.kotlin.autodsl", "processor").versionRef(autoDsl)
            library("autoDsl-annotations", "com.faendir.kotlin.autodsl", "annotations").versionRef(autoDsl)

            val autoService = version("autoService", "1.1.1")
            library("autoService-ksp", "com.google.auto.service", "auto-service").versionRef(autoService)
            library("autoService-annotations", "com.google.auto.service", "auto-service-annotations").versionRef(autoService)

            library("blurhash", "com.vanniktech:blurhash:0.1.0")

            val commonmark = version("commonmark", "0.21.0") // TODO: update
            library("commonmark-annotations", "org.commonmark", "commonmark").versionRef(commonmark)
            library("commonmark-extension-strikethrough", "org.commonmark", "commonmark-ext-gfm-strikethrough").versionRef(commonmark)
            library("commonmark-extension-tables", "org.commonmark", "commonmark-ext-gfm-tables").versionRef(commonmark)
            bundle("commonmark", listOf("commonmark-annotations", "commonmark-extension-strikethrough", "commonmark-extension-tables"))

            val exoplayer = version("exoPlayer", "2.19.0") // TODO: https://github.com/androidx/media/releases/tag/1.1.0
            library("exoplayer-core", "com.google.android.exoplayer", "exoplayer-core").versionRef(exoplayer)
            library("exoplayer-dash", "com.google.android.exoplayer", "exoplayer-dash").versionRef(exoplayer)
            library("exoplayer-hls", "com.google.android.exoplayer", "exoplayer-hls").versionRef(exoplayer)
            library("exoplayer-rtsp", "com.google.android.exoplayer", "exoplayer-rtsp").versionRef(exoplayer)
            library("exoplayer-transformer", "com.google.android.exoplayer", "exoplayer-transformer").versionRef(exoplayer)
            library("exoplayer-ui", "com.google.android.exoplayer", "exoplayer-ui").versionRef(exoplayer)
            library("exoplayer-okhttp", "com.google.android.exoplayer", "extension-okhttp").versionRef(exoplayer)
            bundle("exoplayer", listOf("exoplayer-core", "exoplayer-dash", "exoplayer-ui", "exoplayer-okhttp"))

            library("github", "org.kohsuke:github-api:1.315")

            val glide = version("glide", "4.15.1")
            library("glide", "com.github.bumptech.glide", "glide").versionRef(glide)
            library("glide-compiler", "com.github.bumptech.glide", "compiler").versionRef(glide)
            library("glide-ksp", "com.github.bumptech.glide", "ksp").versionRef(glide)

            library("graphql-base", "graphql", "graphql").version {
                strictly("[2.3.0, 2.3.2[")
                prefer("2.3.1")
            }
            library("graphql-ktor", "com.expediagroup", "graphql-kotlin-ktor-client").versionRef(graphql)
            library("ktor-okhttp", "io.ktor", "ktor-client-okhttp").versionRef(ktor)
            library("ktor-logging", "io.ktor", "ktor-client-logging").versionRef(ktor)
            bundle("graphql", listOf("graphql-ktor", "ktor-okhttp", "ktor-logging"))

            library("gson", "com.google.code.gson:gson:2.8.9") // TODO: switch to kotlinx-serialization

            library("guava", "com.google.guava:guava:32.0.1-jre")
            // https://github.com/google/guava/wiki/ReleasePolicy#flavors

            val hiltAndroidX = version("hilt-androidx", "1.0.0-alpha03")
            library("hilt-android", "com.google.dagger", "hilt-android").versionRef(hilt)
            library("hilt-compiler", "com.google.dagger", "hilt-android-compiler").versionRef(hilt)
            library("hilt-androidx-common", "androidx.hilt", "hilt-common").versionRef(hiltAndroidX)
            library("hilt-androidx-compiler", "androidx.hilt", "hilt-compiler").versionRef(hiltAndroidX)

            library("jraw", "com.github.Haptic-Apps", "JRAW").version("9c8a410a06") // TODO: drop

            library("jsoup", "org.jsoup:jsoup:1.16.1")

            library("junit", "junit:junit:4.10")

            library("jwt", "com.auth0:java-jwt:3.11.0")

            library("kotlin-logging", "io.github.oshai:kotlin-logging-jvm:4.0.0")

            library("kotlin-test", "org.jetbrains.kotlin", "kotlin-test").versionRef(kotlin)

            val coroutines = version("coroutines", "1.7.1")
            library("kotlinx-coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef(coroutines)
            library("kotlinx-coroutines-android", "org.jetbrains.kotlinx", "kotlinx-coroutines-android").versionRef(coroutines)
            library("kotlinx-datetime", "org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")
            library("kotlinx-serialization", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
            // Soon: https://github.com/Kotlin/kotlinx.serialization/commit/782b9f3be9970e0fd36215a86bf7fdba9f2bfe83

            library("markdown", "org.jetbrains:markdown:0.2.2")
            library("material", "com.google.android.material:material:1.4.0")

            val materialdialogs = version("materialdialogs", "3.3.0")
            library("materialDialogs-core", "com.afollestad.material-dialogs", "core").versionRef(materialdialogs)
            library("materialDialogs-input", "com.afollestad.material-dialogs", "input").versionRef(materialdialogs)
            library("materialDialogs-files", "com.afollestad.material-dialogs", "files").versionRef(materialdialogs)
            library("materialDialogs-color", "com.afollestad.material-dialogs", "color").versionRef(materialdialogs)
            library("materialDialogs-datetime", "com.afollestad.material-dialogs", "datetime").versionRef(materialdialogs)
            library("materialDialogs-bottomsheets", "com.afollestad.material-dialogs", "bottomsheets").versionRef(materialdialogs)
            library("materialDialogs-lifecycle", "com.afollestad.material-dialogs", "lifecycle").versionRef(materialdialogs)
            bundle("materialDialogs", listOf("materialDialogs-core", "materialDialogs-lifecycle",
                "materialDialogs-input", "materialDialogs-files", "materialDialogs-color",
                "materialDialogs-datetime", "materialDialogs-bottomsheets"))

            library("materialProgressBar", "me.zhanghai.android.materialprogressbar:library:1.6.1")

            library("mp4parser-isoparser", "org.mp4parser:isoparser:1.9.41")
            library("mp4parser-muxer", "org.mp4parser:muxer:1.9.41")
            bundle("mp4parser", listOf("mp4parser-isoparser", "mp4parser-muxer"))

            library("newpipe-extractor", "com.github.teamnewpipe:NewPipeExtractor:v0.22.6") // TODO: migrate :videoplugin

            library("okhttp", "com.squareup.okhttp3", "okhttp").version("4.11.0")

            library("retrofit", "com.squareup.retrofit2", "retrofit").version("2.9.0")
            library("retrofit-logging", "com.nightlynexus.logging-retrofit:logging:0.12.0")
            library("retrofit-kotlinx", "com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
            bundle("retrofit", listOf("retrofit", "retrofit-logging", "retrofit-kotlinx"))

            library("robolectric", "org.robolectric:robolectric:4.10.3")

            library("rxjava", "io.reactivex.rxjava2", "rxjava").version("2.2.21")

            library("slf4j-handroid", "com.gitlab.mvysny.slf4j:slf4j-handroid:2.0.4")
            bundle("slf4j", listOf("slf4j-handroid"))

            library("slidingUpPanel", "com.github.hannesa2:AndroidSlidingUpPanel:4.5.0")

            library("tedonactivityresult", "io.github.ParkSangGwon:tedonactivityresult:1.1.4")

            library("testng", "org.testng:testng:6.9.6")
        }
    }
}

rootProject.name = "slide"

include(":app")
include(":app:bottompicker")
include(":app:crash")
include(":app:data")
include(":app:data:lemmy")
include(":app:roboto")
include(":app:util")
include(":app:videoplugin")
include(":network")
include(":network:lemmy")
include(":network:lotide")
include(":network:reddit")
include(":network:stats")
include(":network:ycombinator")
include(":readability")
include(":util")
