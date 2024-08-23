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

private const val USER_AGENT = "CodeMC API"

internal val http = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build()

internal val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = true
}

/**
 * Sends an HTTP request using the HTTP Client.
 * @param url The URL to send the request to.
 * @param request The builder modifier on the HTTP Request.
 * @return An HTTP Response.
 */
suspend fun req(url: String, request: HttpRequest.Builder.() -> Unit = { GET() }): HttpResponse<String>
    = withContext(Dispatchers.IO) {
    val req = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("User-Agent", USER_AGENT)

    request(req)

    http.send(req.build(), HttpResponse.BodyHandlers.ofString())
}

/**
 * Checks if any file exist in a Git Repository.
 * @param git The URL to the git repository.
 * @param files The files to check if they exist, relative to the repository.
 * @return `true` if **any** file exists in the git repository, `false` if none exist.
 */
suspend fun filesExists(git: String, files: Iterable<String>): Boolean = withContext(Dispatchers.IO) {
    val dir = Files.createTempDirectory("codemc")

    Runtime.getRuntime().exec(arrayOf(
        "git",
        "clone",
        git,
        dir.toString()
    )).waitFor()

    for (file in files)
        if (dir.resolve(file).toFile().exists())
            return@withContext true

    return@withContext false
}