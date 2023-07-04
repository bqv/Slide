dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("android", "8.0.2")
            version("kotlin", "1.8.22")
            version("serialization", "1.5.0")
            version("retrofit", "2.9.0")
            version("exoPlayer", "2.14.2")
            version("commonmark", "0ebc0749c7")
            version("graphql", "6.5.3")
            version("ktor", "2.3.1")
            version("hilt", "2.46.1")
            version("okhttp", "4.11.0")

            library("gradle-android", "com.android.tools.build", "gradle").versionRef("android")
            library("gradle-kotlin-gradle", "org.jetbrains.kotlin", "kotlin-gradle-plugin").versionRef("kotlin")
            library("gradle-kotlin-serialization", "org.jetbrains.kotlin", "kotlin-serialization").versionRef("kotlin")
            library("gradle-hilt", "com.google.dagger", "hilt-android-gradle-plugin").versionRef("hilt")
            library("gradle-graphql", "com.expediagroup", "graphql-kotlin-gradle-plugin").versionRef("graphql")

            library("graphql-base", "graphql", "graphql").version {
                strictly("[2.3.0, 2.3.2[")
                prefer("2.3.1")
            }
            library("graphql-ktor", "com.expediagroup", "graphql-kotlin-ktor-client").versionRef("graphql")
            library("ktor-okhttp", "io.ktor", "ktor-client-okhttp").versionRef("ktor")
            library("ktor-logging", "io.ktor", "ktor-client-logging").versionRef("ktor")
            bundle("graphql", listOf("graphql-ktor", "ktor-okhttp", "ktor-logging"))

            version("acra", "5.10.1")
            library("acra-sender-http", "ch.acra", "acra-http").versionRef("acra")
            library("acra-sender-mail", "ch.acra", "acra-mail").versionRef("acra")
            library("acra-sender-custom", "ch.acra", "acra-core").versionRef("acra")
            library("acra-interaction-dialog", "ch.acra", "acra-dialog").versionRef("acra")
            library("acra-interaction-notification", "ch.acra", "acra-notification").versionRef("acra")
            library("acra-interaction-toast", "ch.acra", "acra-toast").versionRef("acra")
            library("acra-limiter", "ch.acra", "acra-limiter").versionRef("acra")
            library("acra-scheduler", "ch.acra", "acra-advanced-scheduler").versionRef("acra")
            bundle("acra", listOf(
                "acra-sender-http", "acra-sender-mail", "acra-sender-custom",
                "acra-interaction-dialog", "acra-interaction-notification", "acra-interaction-toast",
                "acra-limiter", "acra-scheduler"))

            library("slf4j-handroid", "com.gitlab.mvysny.slf4j:slf4j-handroid:2.0.4")
            bundle("slf4j", listOf("slf4j-handroid"))

            version("autoService", "1.1.1")
            library("autoService-ksp", "com.google.auto.service", "auto-service").versionRef("autoService")
            library("autoService-annotations", "com.google.auto.service", "auto-service-annotations").versionRef("autoService")

            version("autoDsl", "2.2.10")
            library("autoDsl-processor", "com.faendir.kotlin.autodsl", "processor").versionRef("autoDsl")
            library("autoDsl-annotations", "com.faendir.kotlin.autodsl", "annotations").versionRef("autoDsl")

            library("github", "org.kohsuke:github-api:1.315")

            library("jsoup", "org.jsoup:jsoup:1.16.1")

            library("newpipe-extractor", "com.github.TeamNewPipe:NewPipeExtractor:v0.22.1") // TODO: migrate :videoplugin

            version("commonmark", "0.21.0") // TODO: update
            library("commonmark-annotations", "org.commonmark", "commonmark").versionRef("commonmark")
            library("commonmark-extension-strikethrough", "org.commonmark", "commonmark-ext-gfm-strikethrough").versionRef("commonmark")
            library("commonmark-extension-tables", "org.commonmark", "commonmark-ext-gfm-tables").versionRef("commonmark")
            bundle("commonmark", listOf("commonmark-annotations", "commonmark-extension-strikethrough", "commonmark-extension-tables"))

            library("jraw", "com.github.Haptic-Apps:JRAW:9c8a410a06") // TODO: drop

            library("markdown", "org.jetbrains:markdown:0.2.2")

            plugin("androidgitversion", "com.gladed.androidgitversion").version("0.4.14")
        }
    }
}

rootProject.name = "slide"

include(":app")
include(":app:bottompicker")
include(":app:crash")
include(":app:roboto")
include(":app:util")
include(":app:videoplugin")
include(":data")
include(":data:lemmy")
include(":data:reddit")
include(":data:ycombinator")
include(":readability")
include(":stats")
include(":util")
