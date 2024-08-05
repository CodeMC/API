package io.codemc.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestGenerator {

    @Test
    fun testCreatePassword() {
        val p1 = createPassword(16)
        assertEquals(p1.length, 16)
        assertTrue { !p1.contains("=") }

        val p2 = createPassword(32)
        assertEquals(p2.length, 32)
        assertTrue { !p2.contains("/") }
    }

}