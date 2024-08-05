package io.codemc.api

import io.codemc.api.jenkins.*
import io.codemc.api.nexus.*
import io.codemc.api.nexus.getNexusAssets
import io.codemc.api.nexus.getNexusRepository
import io.codemc.api.nexus.getNexusRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import kotlin.test.Test

class TestOverall {

    companion object {
        init {
            val jenkins = JenkinsConfig(
                url = "http://localhost:8080",
                username = "",
                password = ""
            )

            val nexus = NexusConfig(
                url = "http://localhost:8081",
                username = "admin",
                password = File("/tmp/admin.password").readText().trim()
            )

            runBlocking { initialize(jenkins, nexus) }
        }
    }

    @Test
    fun testBuildJob() = runBlocking(Dispatchers.IO) {
        val name = "CodeMC"
        val jobName = "API"
        val password = createPassword(24)
        val url = "https://github.com/CodeMC/API"

        println("Adding Nexus/Jenkins Details...")

        // Jenkins
        assertTrue(createJenkinsUser(name, password))
        assertTrue(getJenkinsUser(name).isNotEmpty())
        assertTrue(createJenkinsJob(name, jobName, url, true))
        assertTrue(getJenkinsJob(name, jobName).isNotEmpty())

        // Nexus
        assertTrue(createNexus(name, password))

        val repoName = name.lowercase()
        assertFalse(getNexusRepository(repoName).isNullOrEmpty())
        assertFalse(getNexusRole(repoName).isNullOrEmpty())

        // Await Job
        println("Waiting for Jenkins to finish building...")
        triggerBuild(name, jobName)
        while (isBuilding(name, jobName)) {
            delay(1000)
        }

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