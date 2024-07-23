import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.0"
    id("io.github.goooler.shadow") version "8.1.7"

    java
    jacoco
    `maven-publish`
}

group = "io.codemc.api"
version = "1.0.0"
description = "Official API for CodeMC Jenkins & Nexus Services"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("io.github.cdancy:jenkins-rest:1.0.2")

    testImplementation(kotlin("test"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    jar.configure {
        dependsOn("shadowJar")
        archiveClassifier.set("raw")
    }

    withType<ShadowJar> {
        archiveFileName.set("${project.name}-${project.version}.jar")
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        dependsOn(test)

        reports {
            csv.required.set(false)

            xml.required.set(true)
            xml.outputLocation.set(layout.buildDirectory.file("jacoco.xml"))

            html.required.set(true)
            html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
        }
    }
}

artifacts {
    add("default", tasks.getByName<ShadowJar>("shadowJar"))
}

publishing {
    repositories {
        maven {
            credentials {
                username = System.getenv("JENKINS_USERNAME")
                password = System.getenv("JENKINS_PASSWORD")
            }

            val releases = "https://repo.codemc.io/repository/maven-releases/"
            val snapshots = "https://repo.codemc.io/repository/maven-snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshots else releases)
        }
    }
}