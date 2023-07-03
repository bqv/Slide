dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("okhttp3", "com.squareup.okhttp3:okhttp:4.10.0")
            library("ktor", "ktor", "ktor").version {
                strictly("[3.8, 4.0[")
                prefer("3.9")
            }

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

            version("commonmark", "commonmark-parent-0.5.1") // TODO: update
            library("commonmark-annotations", "com.github.commonmark.commonmark-java", "commonmark").versionRef("commonmark")
            library("commonmark-extension-strikethrough", "com.github.commonmark.commonmark-java", "commonmark-ext-gfm-strikethrough").versionRef("commonmark")
            library("commonmark-extension-tables", "com.github.commonmark.commonmark-java", "commonmark-ext-gfm-tables").versionRef("commonmark")
            bundle("commonmark", listOf("commonmark-annotations", "commonmark-extension-strikethrough", "commonmark-extension-tables"))

            library("jraw", "com.github.Haptic-Apps:JRAW:9c8a410a06") // TODO: drop

            library("markdown", "org.jetbrains:markdown:0.2.2")

            plugin("androidgitversion", "com.gladed.androidgitversion").version("0.4.14")
        }
    }
}

rootProject.name = "slide"

include(":app")
//include(":app:bottompicker")
include(":app:bottompickerv2")
include(":app:crash")
include(":app:roboto")
include(":app:util")
include(":data")
include(":data:lemmy")
include(":data:reddit")
include(":readability")
include(":stats")
include(":util")
