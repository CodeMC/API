# CodeMC API

This is the official API for CodeMC Jenkins and Nexus services. It is used by the
Discord Bot to do various tasks as a part of our service to our users.

## Installation

[![GitHub branch checks state](https://github.com/CodeMC/API/actions/workflows/build.yml/badge.svg)](https://github.com/gmitch215/SocketMC/actions/workflows/build.yml)
![GitHub](https://img.shields.io/github/license/CodeMC/API)
![GitHub issues](https://img.shields.io/github/issues/CodeMC/API)

<details>
    <summary>Maven</summary>

```xml
<project>
    
    <!-- Import CodeMC Repo -->
    
    <repositories>
        <repository>
            <id>codemc-public</id>
            <url>https://repo.codemc.io/repository/maven-public/</url>
        </repository>
    </repositories>
    
    <dependencies>
        <dependency>
          <groupId>io.codemc.api</groupId>
          <artifactId>codemc-api</artifactId>
          <version>[VERSION]</version>
        </dependency>
    </dependencies>
    
</project>
```
</details>

<details>
    <summary>Gradle (Groovy)</summary>

```gradle
repositories {
    maven { url 'https://repo.codemc.io/repository/maven-public/' }
}

dependencies {
    implementation 'io.codemc.api:codemc-api:[VERSION]'
}
```
</details>

<details>
    <summary>Gradle (Kotlin DSL)</summary>

```kotlin
repositories {
    maven(url = "https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    implementation("io.codemc.api:codemc-api:[VERSION]")
}
```
</details>

## Local Testing

Test suites rely on both an active services, such as Nexus and Jenkins instances.
Luckily, the project comes with Docker files to set these up yourself.

Without the services, some test suites will fail.

### Test Script (preferred)

A bash script is provided to build the containers and run the tests. This is the easiest way to test the project.

Requires [Docker](https://www.docker.com) to be installed.

```bash
bash .github/test.sh
```

### [Act](https://nektosact.com)

Test the project by using Act, a GitHub Action emulator. This will set up and test everything for you.

```bash
act push -j 'test'
```

### Manual

First, build and run both containers:

```bash
# MariaDB
docker build -t codemc/mariadb src/test/docker/mariadb
docker run -d -p 3306:3306 --name mariadb codemc/mariadb

# Jenkins
docker build -t codemc/jenkins src/test/docker/jenkins
docker run -d -p 8080:8080 --name jenkins-rest codemc/jenkins

# Nexus
docker build -t codemc/nexus src/test/docker/nexus
docker run -d -p 8081:8081 --name nexus-rest codemc/nexus
```

Before running, copy over the `admin.password` file into the `/tmp/` folder so that the API can log in to the newly created Nexus instance.

```bash
docker cp nexus-rest:/nexus-data/admin.password /tmp/admin.password
```

Then, run the tests:

```bash
./gradlew test
```