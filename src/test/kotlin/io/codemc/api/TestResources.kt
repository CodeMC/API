package io.codemc.api

import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

class TestResources {

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