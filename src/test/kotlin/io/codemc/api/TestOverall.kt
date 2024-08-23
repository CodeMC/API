package io.codemc.api

import io.codemc.api.database.DBConfig
import io.codemc.api.jenkins.*
import io.codemc.api.nexus.*
import io.codemc.api.nexus.getNexusAssets
import io.codemc.api.nexus.getNexusRepository
import io.codemc.api.nexus.getNexusRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.io.File
import kotlin.test.Test

@Execution(ExecutionMode.CONCURRENT)
class TestOverall {

    companion object {
        init {
            val jenkins = JenkinsConfig(
                url = "http://localhost:8080",
                username = "admin",
                token = "00000000000000000000000000000000"
            )

            val nexus = NexusConfig(
                url = "http://localhost:8081",
                username = "admin",
                password = File("/tmp/admin.password").readText().trim()
            )

            val db = DBConfig(
                url = "jdbc:mariadb://localhost:3306/test",
                username = "admin",
                password = "password"
            )

            runBlocking { initialize(jenkins, nexus, db) }
        }
    }

    @Test
    fun testBuildGradleJob() = runBlocking(Dispatchers.IO) {
        val name = "CodeMC-Gradle"
        val jobName = "API"
        val password = createPassword(24)
        val url = "https://github.com/CodeMC/API"

        println("Adding Nexus/Jenkins Details...")

        // Jenkins
        val repoName = name.lowercase()

        assertTrue(createJenkinsUser(name, password))
        assertTrue(getJenkinsUser(name).isNotEmpty())
        assertTrue(createJenkinsJob(name, jobName, url, true) { config ->
            config.replace(
                "<tasks>publish</tasks>",
                "<tasks>publish -PrepositoryURL=http://nexus-rest:8081/repository/$repoName/</tasks>"
            )
        })
        assertTrue(getJenkinsJob(name, jobName).isNotEmpty())

        // Nexus
        assertTrue(createNexus(name, password))
        assertFalse(getNexusRepository(repoName).isNullOrEmpty())
        assertFalse(getNexusRole(repoName).isNullOrEmpty())

        // Await Job
        println("Waiting for Jenkins to finish building...")
        triggerBuild(name, jobName)
        while (isBuilding(name, jobName)) {
            delay(1000)
        }

        // Jenkins 2
        val info = getJobInfo(name, jobName)
        assertNotNull(info)
        assertNotNull(info?.url)
        assertNotNull(info?.lastBuild)
        assertTrue((info?.lastBuild?.number ?: 0) > 0)
        assertTrue(info?.lastBuild.toString() != "null")
        assertNotNull(info?.lastBuild?.url)
        assertNull(info?.lastFailedBuild)

        // Cleanup
        println("Cleaning up... ")
        assertFalse(getNexusAssets(repoName).isNullOrEmpty())

        assertTrue(deleteJob(name, jobName))
        assertTrue(getJenkinsJob(name, jobName).isEmpty())
        assertTrue(deleteUser(name))
        assertTrue(getJenkinsUser(name).isEmpty())
        assertTrue(deleteNexus(name))
        assertTrue(getNexusRepository(repoName).isNullOrEmpty())
    }

    @Test
    fun testBuildMavenJob() = runBlocking(Dispatchers.IO) {
        val name = "CodeMC-Maven"
        val jobName = "WorldGuardWrapper"
        val password = createPassword(24)
        val url = "https://github.com/CodeMC/WorldGuardWrapper"

        // Jenkins
        val repoName = name.lowercase()

        assertTrue(createJenkinsUser(name, password))
        assertTrue(getJenkinsUser(name).isNotEmpty())
        assertTrue(createJenkinsJob(name, jobName, url, false) { config ->
            config.replace(
                "<goals>clean deploy</goals>",
                "<goals>clean deploy -DaltDeploymentRepository=$repoName::http://nexus-rest:8081/repository/$repoName/ -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true</goals>"
            ).replace(
                "<settingsConfigId>e5b005b5-be4d-4709-8657-1981662bcbe3</settingsConfigId>",
                ""
            )
        })
        assertTrue(getJenkinsJob(name, jobName).isNotEmpty())

        // Nexus
        assertTrue(createNexus(name, password))
        assertFalse(getNexusRepository(repoName).isNullOrEmpty())
        assertFalse(getNexusRole(repoName).isNullOrEmpty())

        // Await Job
        println("Waiting for Jenkins to finish building...")
        triggerBuild(name, jobName)
        while (isBuilding(name, jobName)) {
            delay(1000)
        }

        // Jenkins 2
        val info = getJobInfo(name, jobName)
        assertNotNull(info)
        assertNotNull(info?.url)
        assertNotNull(info?.lastBuild)
        assertTrue((info?.lastBuild?.number ?: 0) > 0)
        assertTrue(info?.lastBuild.toString() != "null")
        assertNotNull(info?.lastBuild?.url)
        assertNull(info?.lastFailedBuild)

        // Cleanup
        println("Cleaning up... ")
        assertFalse(getNexusAssets(repoName).isNullOrEmpty())

        assertTrue(deleteJob(name, jobName))
        assertTrue(getJenkinsJob(name, jobName).isEmpty())
        assertTrue(deleteUser(name))
        assertTrue(getJenkinsUser(name).isEmpty())
        assertTrue(deleteNexus(name))
        assertTrue(getNexusRepository(repoName).isNullOrEmpty())
    }

}