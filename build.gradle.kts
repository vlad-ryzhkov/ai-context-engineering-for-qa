import java.nio.file.Files

plugins {
    kotlin("jvm") version "1.9.22"
}

group = "com.example.tests"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.7"
val kotestVersion = "5.8.0"
val jacksonVersion = "2.16.1"
val allureVersion = "2.25.0"

dependencies {
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")

    testImplementation("io.ktor:ktor-client-core:$ktorVersion")
    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    testImplementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")

    testImplementation("io.qameta.allure:allure-junit5:$allureVersion")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.ktor:ktor-client-logging:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")

    testImplementation("net.datafaker:datafaker:2.1.0")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
    testImplementation("org.wiremock:wiremock-standalone:3.3.1")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.register("installGitHooks") {
    group = "setup"
    description = "Installs pre-commit and pre-push hooks from scripts/"

    val hooksDir = rootDir.resolve(".git/hooks")
    val scriptsDir = rootDir.resolve("scripts")

    doLast {
        mapOf(
            "pre-commit" to "pre-commit.sh",
            "pre-push" to "pre-push.sh",
        ).forEach { (hookName, scriptName) ->
            val hookFile = hooksDir.resolve(hookName)
            val scriptFile = scriptsDir.resolve(scriptName)

            if (hookFile.exists() || Files.isSymbolicLink(hookFile.toPath())) hookFile.delete()

            Files.createSymbolicLink(
                hookFile.toPath(),
                hookFile.parentFile.toPath().relativize(scriptFile.toPath()),
            )

            scriptFile.setExecutable(true)
            println("Installed: .git/hooks/$hookName -> scripts/$scriptName")
        }
    }
}
