import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.dokka") version "1.9.20"
    id("io.github.goooler.shadow") version "8.1.8"

    java
    jacoco
    `maven-publish`
}

group = "io.codemc.api"
version = "1.1.1"
description = "Official API for CodeMC Jenkins & Nexus Services"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.0")
    implementation("org.jetbrains.exposed:exposed-core:0.56.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.56.0")

    testImplementation(kotlin("test"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withSourcesJar()
}

tasks {
    clean {
        delete("bin")
    }

    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jar.configure {
        dependsOn("shadowJar")
        archiveClassifier.set("raw")
    }

    withType<ShadowJar> {
        archiveFileName.set("${project.name}-${project.version}.jar")
        archiveClassifier.set("")
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
    publications {
        create<MavenPublication>("maven") {
            artifactId = project.name

            pom {
                description.set(project.description)
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/CodeMC/API/blob/master/LICENSE")
                    }
                }
            }

            from(components["java"])
        }
    }

    repositories {
        maven {
            credentials {
                username = System.getenv("JENKINS_USERNAME")
                password = System.getenv("JENKINS_PASSWORD")
            }

            isAllowInsecureProtocol = true

            url = uri(project.findProperty("repositoryURL") ?: "https://repo.codemc.io/repository/codemc/")
        }
    }
}
