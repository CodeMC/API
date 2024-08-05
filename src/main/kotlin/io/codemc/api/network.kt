@file:JvmName("Network")

package io.codemc.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.time.Duration

private const val USER_AGENT = "CodeMC Nexus API"

internal val http = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build()

internal val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = true
}

internal suspend fun req(url: String, request: HttpRequest.Builder.() -> Unit = { GET() }): HttpResponse<String>
    = withContext(Dispatchers.IO) {
    val req = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("User-Agent", USER_AGENT)

    request(req)

    http.send(req.build(), HttpResponse.BodyHandlers.ofString())
}

suspend fun filesExists(url: String, files: Iterable<String>): Boolean = withContext(Dispatchers.IO) {
    val dir = Files.createTempDirectory("codemc")

    Runtime.getRuntime().exec(arrayOf(
        "git",
        "clone",
        url,
        dir.toString()
    )).waitFor()

    for (file in files)
        if (dir.resolve(file).toFile().exists())
            return@withContext true

    return@withContext false
}