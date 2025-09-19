plugins {
    kotlin("multiplatform") version libs.versions.kotlin apply false
    id("com.android.library") version libs.versions.plugin.android apply false
    kotlin("plugin.serialization") version libs.versions.kotlin apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
}



