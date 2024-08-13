package io.codemc.api.nexus

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import java.io.File
import java.util.*
import kotlin.test.Test

class TestNexus {

    companion object {

        init {
            val password = File("/tmp/admin.password").readText().trim()

            nexusConfig = NexusConfig(
                url = "http://localhost:8081",
                username = "admin",
                password = password
            )
        }

        @JvmStatic
        @BeforeAll
        fun testPing() = runBlocking(Dispatchers.IO) {
            assertTrue(ping())
        }

    }

    @Test
    fun testNexus() = runBlocking(Dispatchers.IO) {
        val name = "TestUser"
        val repoName = name.lowercase()

        assertTrue(createNexus(name, UUID.randomUUID().toString()))
        assertFalse(getNexusUser(name).isNullOrEmpty())
        assertTrue(getNexusUser("OtherName").isNullOrEmpty())
        assertTrue(getRepositories().isNotEmpty())
        assertFalse(getNexusRepository(repoName).isNullOrEmpty())
        assertFalse(getNexusRole(repoName).isNullOrEmpty())

        assertTrue(deleteNexus(name))
        assertTrue(getNexusUser(name).isNullOrEmpty())
        assertTrue(getNexusRepository(repoName).isNullOrEmpty())
        assertTrue(getNexusRole(repoName).isNullOrEmpty())
    }

}