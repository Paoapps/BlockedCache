pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "BlockedCache"

includeBuild("convention-plugins")

include(":blockedcache")
include(":sample:android")
include(":sample:shared")
