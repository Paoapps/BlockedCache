plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
    id("convention.publication")
}

group = "com.paoapps.blockedcache"
version = "0.0.5"

kotlin {
    android {
        publishLibraryVariants("debug", "release")
    }

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.touchlab.kermit)
            }

            kotlin.srcDirs(project.projectDir.resolve("build/src/commonMain/kotlin"))
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
        }

//        val androidTest by getting {
//            dependencies {
//                implementation(kotlin("test-junit"))
//                implementation("junit:junit:4.13.2")
//            }
//        }

//        val jvmMain by getting {
//            kotlin.srcDirs(project.projectDir.resolve("build/src/jvmMain/kotlin"))
//        }

//        val jsMain by getting {
//            kotlin.srcDirs(project.projectDir.resolve("build/src/jsMain/kotlin"))
//        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

android {
    compileSdk = 33
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
