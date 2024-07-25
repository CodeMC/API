package io.codemc.api.nexus

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

// Fields

private const val USER_AGENT = "CodeMC Nexus API"

private val client = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build()

val API_URL
    get() = "${nexusConfig.url}/service/rest/v1"

// Implementation

internal suspend fun req(url: String, request: HttpRequest.Builder.() -> Unit = { GET() }): HttpResponse<String>
    = withContext(Dispatchers.IO) {
        val req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", USER_AGENT)
            .header("Authorization", nexusConfig.authorization)

        request(req)

        client.send(req.build(), HttpResponse.BodyHandlers.ofString())
    }

internal fun createMavenRepository(name: String): String = buildJsonObject {
    put("name", name.lowercase())
    put("online", true)

    putJsonObject("storage") {
        put("blobStoreName", "default")
        put("strictContentTypeValidation", true)
        put("writePolicy", "allow")
    }

    putJsonObject("maven") {
        put("versionPolicy", "MIXED")
        put("layoutPolicy", "STRICT")
    }
}.toString()

internal fun getNexusRoles(name: String): List<String> = listOf(
    "nx-healthcheck-read",
    "nx-search-read",
    "nx-repository-view-*-*-read",
    "nx-repository-view-*-*-browse",
    "nx-repository-admin-maven2-$name-edit"
)