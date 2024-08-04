package io.codemc.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import java.net.http.HttpRequest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

object TestNetwork {

    @Test
    fun testRequest() = runBlocking(Dispatchers.IO) {
        val r1 = req("https://github.com/CodeMC/.github")
        assertEquals(r1.statusCode(), 200)

        val r2 = req("https://httpbin.org/post") {
            POST(HttpRequest.BodyPublishers.ofString("Hello World!"))
        }
        assertEquals(r2.statusCode(), 200)

        val data = r2.body()
        assertFalse(data.isNullOrEmpty())

        val json = json.parseToJsonElement(data).jsonObject
        assertEquals(json["data"]?.jsonPrimitive?.content, "Hello World!")
    }

}