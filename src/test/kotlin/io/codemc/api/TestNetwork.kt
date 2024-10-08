package io.codemc.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import java.net.http.HttpRequest
import kotlin.test.Test

class TestNetwork {

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

        val r3 = req("https://httpbin.org/delete") { DELETE() }
        assertEquals(r3.statusCode(), 200)

        val statuses = listOf(200, 201, 202, 204, 400, 401, 403, 404, 500, 503)
        for (status in statuses) {
            val r = req("https://httpbin.org/status/$status")
            assertEquals(r.statusCode(), status)
        }
    }

}