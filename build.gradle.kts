import java.util.Properties

plugins {
    kotlin("multiplatform") version libs.versions.kotlin
    id("com.android.library") version libs.versions.plugin.android
    kotlin("plugin.serialization") version libs.versions.kotlin
    id("maven-publish")
    signing
}

// Load local.properties for publishing credentials
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

group = "com.paoapps.blockedcache"
version = "0.0.7-SNAPSHOT"

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
                }
            }
        }
        publishLibraryVariants("release")
        publishLibraryVariantsGroupedByFlavor = true
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.framework {
            baseName = "blocked-cache"
            isStatic = true
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
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependencies {
                // Android specific dependencies if any
            }
        }

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
    compileSdk = 34
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    namespace = "com.paoapps.blockedcache"
}

// Create zip tasks for iOS frameworks
val iosArm64FrameworkZip by tasks.registering(Zip::class) {
    dependsOn("linkReleaseFrameworkIosArm64")
    from("build/bin/iosArm64/releaseFramework")
    include("**/*")
    archiveFileName.set("blocked-cache.framework.zip")
    destinationDirectory.set(layout.buildDirectory.dir("frameworks/iosArm64"))
}

val iosX64FrameworkZip by tasks.registering(Zip::class) {
    dependsOn("linkReleaseFrameworkIosX64")
    from("build/bin/iosX64/releaseFramework")
    include("**/*")
    archiveFileName.set("blocked-cache.framework.zip")
    destinationDirectory.set(layout.buildDirectory.dir("frameworks/iosX64"))
}

val iosSimulatorArm64FrameworkZip by tasks.registering(Zip::class) {
    dependsOn("linkReleaseFrameworkIosSimulatorArm64")
    from("build/bin/iosSimulatorArm64/releaseFramework")
    include("**/*")
    archiveFileName.set("blocked-cache.framework.zip")
    destinationDirectory.set(layout.buildDirectory.dir("frameworks/iosSimulatorArm64"))
}

publishing {
    repositories {
        maven {
            name = "centralPortal"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = localProperties.getProperty("ossrhUsername") ?: ""
                password = localProperties.getProperty("ossrhPassword") ?: ""
            }
        }
    }

    publications.withType<MavenPublication> {
        artifactId = "blocked-cache"
        pom {
            name.set("BlockedCache")
            description.set("Kotlin Multiplatform Mobile framework for optimal code sharing between iOS and Android.")
            url.set("https://github.com/Paoapps/BlockedCache")

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("lammertw")
                    name.set("Lammert Westerhoff")
                    email.set("lammert@paoapps.com")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/Paoapps/BlockedCache.git")
                developerConnection.set("scm:git:ssh://github.com/Paoapps/BlockedCache.git")
                url.set("https://github.com/Paoapps/BlockedCache")
            }
        }
    }
}

// Configure publications after Kotlin plugin configures them
afterEvaluate {
    // Configure Android publication with correct artifactId
    publishing.publications.named("android", MavenPublication::class) {
        artifactId = "blocked-cache"
    }

    // Add iOS framework artifacts to their respective publications
    publishing.publications.getByName("iosArm64") {
        this as MavenPublication
        artifact(iosArm64FrameworkZip) {
            classifier = "iosarm64"
            extension = "zip"
        }
    }

    publishing.publications.getByName("iosX64") {
        this as MavenPublication
        artifact(iosX64FrameworkZip) {
            classifier = "iosx64"
            extension = "zip"
        }
    }

    publishing.publications.getByName("iosSimulatorArm64") {
        this as MavenPublication
        artifact(iosSimulatorArm64FrameworkZip) {
            classifier = "iossimulatorarm64"
            extension = "zip"
        }
    }

    // Ensure zip tasks run before publishing
    tasks.named("publishIosArm64PublicationToMavenLocal") {
        dependsOn(iosArm64FrameworkZip)
    }
    tasks.named("publishIosX64PublicationToMavenLocal") {
        dependsOn(iosX64FrameworkZip)
    }
    tasks.named("publishIosSimulatorArm64PublicationToMavenLocal") {
        dependsOn(iosSimulatorArm64FrameworkZip)
    }
}

signing {
    val signingKeyId = localProperties.getProperty("signing.keyId")
    val signingPassword = localProperties.getProperty("signing.password")
    val signingSecretKeyRingFile = localProperties.getProperty("signing.secretKeyRingFile")

    if (signingKeyId != null && signingPassword != null) {
        if (signingSecretKeyRingFile != null) {
            useInMemoryPgpKeys(signingKeyId, signingSecretKeyRingFile, signingPassword)
        } else {
            useGpgCmd()
        }
        // Sign all publications except iOS framework artifacts
        publishing.publications.filterNot { it.name.contains("ios", ignoreCase = true) }.forEach { sign(it) }
    } else {
        println("GPG signing skipped - missing keyId or password")
    }
}