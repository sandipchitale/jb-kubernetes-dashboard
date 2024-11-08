import org.apache.commons.lang3.SystemUtils

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "dev.sandipchitale"
version = "1.0.10"

repositories {
    mavenCentral()
}


configurations.all {
    exclude("org.slf4j")
}
dependencies {
    implementation ("io.fabric8:kubernetes-client:6.13.4")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.2.6")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf("terminal", "com.intellij.kubernetes:232.10203.2"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    runIde {
        if (project.hasProperty("runIde_ideDir")) {
            ideDir = file("${project.extra["runIde_ideDir"]}")
        }
    }

    prepareSandbox {
        from("${projectDir}/kubernetes/") {
            into("${intellij.pluginName.get()}/kubernetes")
        }
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("242.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
