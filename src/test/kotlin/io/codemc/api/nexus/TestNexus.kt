package io.codemc.api.nexus

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File

@EnabledIfEnvironmentVariable(named = "NEXUS_INSTANCE", matches = "true")
object TestNexus {

    @BeforeAll
    @JvmStatic
    fun setup() {
        val password = File("/nexus-data/admin.password").readText().trim()

        nexusConfig = NexusConfig(
            url = "http://localhost:8081",
            username = "admin",
            password = password
        )

        runBlocking {
            assert(ping())
        }
    }

    @Test
    fun testNexus() {
        val name = "TestUser"

        runBlocking {
            assert(createNexus(name))
            assert(getRepositories().any { it.name == name.lowercase() })
        }
    }

}