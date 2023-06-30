include(":app")
include(":app:crash")
include(":stats")
include(":data")
include(":data:lemmy")
include(":data:reddit")
include(":util")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("checkstyle", "8.37")
            library("okhttp3", "com.squareup.okhttp3:okhttp:4.10.0")
            library("ktor", "ktor", "ktor").version {
                strictly("[3.8, 4.0[")
                prefer("3.9")
            }
            library("slf4j-handroid", "com.gitlab.mvysny.slf4j:slf4j-handroid:2.0.4")
            bundle("slf4j", listOf("slf4j-handroid"))
            plugin("androidgitversion", "com.gladed.androidgitversion").version("0.4.14")
        }
    }
}

rootProject.name = "slide"
