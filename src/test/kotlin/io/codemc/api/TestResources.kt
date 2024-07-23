package io.codemc.api

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis
import kotlin.test.assertTrue

object TestResources {

    @Test
    fun testLoadResources() {
        runBlocking {
            val time = measureTimeMillis {
                loadResources()
            }

            println("Loaded Resources in ${time}ms")
        }

        for ((_, value) in RESOURCE_CACHE)
            assertTrue(value.isNotBlank())
    }

}