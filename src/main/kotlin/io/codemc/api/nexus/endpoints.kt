package io.codemc.api.nexus

import kotlinx.serialization.json.Json
import java.net.URI

private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = true
}
val API_URL
    get() = "${nexusConfig.url}/service/rest/v1"

fun getRepositories(): List<NexusRepository> {
    val text = URI("$API_URL/repositories").toURL().readText()
    return json.decodeFromString(text)
}