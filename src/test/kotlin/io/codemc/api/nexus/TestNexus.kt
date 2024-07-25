package io.codemc.api.nexus

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

object TestNexus {

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
    fun testPing() {
        runBlocking { assert(ping()) }
    }

    @Test
    fun testNexus() {
        val name = "TestUser"

        runBlocking {
            assert(createNexus(name, UUID.randomUUID().toString()))
            assert(getRepositories().any {
                it["name"]?.jsonPrimitive?.content == name.lowercase()
            })

            assert(getNexusRole(name.lowercase()).isNotEmpty())
        }
    }

}