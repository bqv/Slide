include(":app")
include(":stats")
include(":data")
include(":data:lemmy")
include(":data:reddit")
include(":util")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("acra", "5.10.1")

            library("okhttp3", "com.squareup.okhttp3:okhttp:4.10.0")
            library("ktor", "ktor", "ktor").version {
                strictly("[3.8, 4.0[")
                prefer("3.9")
            }

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

            plugin("androidgitversion", "com.gladed.androidgitversion").version("0.4.14")
        }
    }
}

rootProject.name = "slide"
