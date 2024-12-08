package io.codemc.api.nexus

import io.codemc.api.loadResources
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

            runBlocking(Dispatchers.IO) {
                loadResources()
            }
        }

        @JvmStatic
        @BeforeAll
        fun testPing() {
            assertTrue(ping())
        }

    }

    @Test
    fun testNexus() = runBlocking(Dispatchers.IO) {
        val name = "TestUser"
        val repoName = name.lowercase()

        assertTrue(createNexus(name, UUID.randomUUID().toString()))
        assertFalse(getNexusUser(name).isNullOrEmpty())
        assertTrue(exists(name))
        assertTrue(getNexusUser("OtherName").isNullOrEmpty())
        assertTrue(getRepositories().isNotEmpty())
        assertFalse(getNexusRepository(repoName).isNullOrEmpty())
        assertFalse(getNexusRole(repoName).isNullOrEmpty())

        assertTrue(deleteNexus(name))
        assertTrue(getNexusUser(name).isNullOrEmpty())
        assertTrue(getNexusRepository(repoName).isNullOrEmpty())
        assertTrue(getNexusRole(repoName).isNullOrEmpty())
    }

    @Test
    fun testChangePassword() = runBlocking(Dispatchers.IO) {
        val name = "OldUser123"

        val p1 = "OldPass987"
        assertTrue(createNexus(name, p1))
        assertFalse(getNexusUser(name).isNullOrEmpty())

        val p2 = "NewPass654"
        assertTrue(changeNexusPassword(name, p2))
        assertFalse(getNexusUser(name).isNullOrEmpty())

        assertTrue(deleteNexus(name))
        assertTrue(getNexusUser(name).isNullOrEmpty())
    }

}