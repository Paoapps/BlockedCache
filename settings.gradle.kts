pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "BlockedCache"

includeBuild("convention-plugins")

include(":blocked-cache")
include(":sample:android")
include(":sample:shared")
