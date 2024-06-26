import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT

// Library version is defined in gradle.properties
val libraryVersion: String by project

plugins {
    kotlin("multiplatform") version "1.9.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
    id("org.gradle.java-library")
    id("org.gradle.maven-publish")
    id("org.jetbrains.dokka") version "1.9.10"
    id("app.cash.sqldelight") version "2.0.2"
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("me.tb.cashulib")
        }
    }
}

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                // Kotlin
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

                // Bitcoin
                implementation("fr.acinq.bitcoin:bitcoin-kmp:0.14.0")
                implementation("fr.acinq.secp256k1:secp256k1-kmp:0.11.0")
                implementation("fr.acinq.lightning:lightning-kmp:1.5.15")

                // SqlDelight
                implementation("app.cash.sqldelight:sqlite-driver:2.0.2")

                // Ktor
                implementation("io.ktor:ktor-client-core:2.3.1")
                implementation("io.ktor:ktor-client-logging:2.3.1")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.1")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.1")

                // Logging
                implementation("co.touchlab:kermit:2.0.4")
            }
        }

        jvmMain {
            dependencies {
                // Bitcoin
                implementation("fr.acinq.secp256k1:secp256k1-kmp-jni-jvm:0.11.0")

                // Ktor OkHttp engine
                implementation("io.ktor:ktor-client-okhttp:2.3.1")
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
            }
        }
    }
}

tasks.withType<Test> {
    testLogging {
        events(PASSED, SKIPPED, FAILED, STANDARD_OUT, STANDARD_ERROR)
        exceptionFormat = FULL
        showExceptions = true
        showStackTraces = true
        showCauses = true
    }
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    explicitApi()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "me.tb"
            artifactId = "cashu-client"
            version = libraryVersion

            from(components["java"])
        }
    }
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    dokkaSourceSets {
        named("commonMain") {
            moduleName.set("cashu-client")
            moduleVersion.set(libraryVersion)
            // includes.from("Module.md")
            // samples.from("src/test/kotlin/me/tb/Samples.kt")
        }
    }
}
