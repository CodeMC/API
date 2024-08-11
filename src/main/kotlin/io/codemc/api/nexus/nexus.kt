@file:JvmName("NexusAPI")

package io.codemc.api.nexus

import io.codemc.api.json
import io.codemc.api.req
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import org.jetbrains.annotations.VisibleForTesting
import java.net.http.HttpRequest
import java.util.*

// Schema

data class NexusConfig(
    val url: String,
    val username: String,
    val password: String
) {
    private val token: String
        get() = Base64.getEncoder().encodeToString("$username:$password".toByteArray())

    val authorization
        get() = "Basic $token"
}

// Fields

lateinit var nexusConfig: NexusConfig

// Implementation

private suspend fun nexus(url: String, request: HttpRequest.Builder.() -> Unit = { GET() }) = req(url) {
    header("Authorization", nexusConfig.authorization)
    request(this)
}

suspend fun ping(): Boolean {
    val text = nexus("$API_URL/status")
    return text.statusCode() == 200
}

@OptIn(ExperimentalSerializationApi::class)
suspend fun createNexus(name: String, password: String) = withContext(Dispatchers.IO) {
    // Create User Repository
    val repo = createMavenRepository(name)
    val repoResponse = nexus("$API_URL/repositories/maven/hosted") {
        POST(HttpRequest.BodyPublishers.ofString(repo))
        header("Content-Type", "application/json")
    }

    if (repoResponse.statusCode() != 201) return@withContext false

    // Add Role
    val roleId = name.lowercase()
    val role = buildJsonObject {
        put("id", roleId)
        put("name", name)
        put("description", "Role for $name")
        putJsonArray("privileges") {
            addAll(getNexusRoles(roleId))
        }
    }.toString()

    val roleReq = nexus("$API_URL/security/roles") {
        POST(HttpRequest.BodyPublishers.ofString(role))
        header("Content-Type", "application/json")
    }

    if (roleReq.statusCode() != 200) return@withContext false

    // Add User with Role
    val user = buildJsonObject {
        put("userId", name)
        put("firstName", name)
        put("lastName", "User")
        put("emailAddress", "$name@users.noreply.github.com") // Can't actually receive mail
        put("status", "active")
        put("password", password)
        putJsonArray("roles") {
            add(roleId)
        }
    }.toString()

    val userRes = nexus("$API_URL/security/users") {
        POST(HttpRequest.BodyPublishers.ofString(user))
        header("Content-Type", "application/json")
    }
    return@withContext userRes.statusCode() == 200
}

suspend fun deleteNexus(name: String) = withContext(Dispatchers.IO) {
    val repoName = name.lowercase()

    val repoRes = nexus("$API_URL/repositories/$repoName") { DELETE() }
    if (repoRes.statusCode() != 204) return@withContext false

    val roleRes = nexus("$API_URL/security/roles/$repoName") { DELETE() }
    if (roleRes.statusCode() != 204) return@withContext false

    val userRes = nexus("$API_URL/security/users/$name") { DELETE() }
    return@withContext userRes.statusCode() == 204
}

@VisibleForTesting
internal suspend fun getRepositories(): List<JsonObject> {
    val text = nexus("$API_URL/repositories").body()
    return json.decodeFromString(text)
}

suspend fun getNexusRepository(name: String): JsonObject? {
    val res = nexus("$API_URL/repositories/$name")
    if (res.statusCode() == 404) return null

    return json.decodeFromString(res.body())
}

@VisibleForTesting
internal suspend fun getNexusRole(name: String): JsonObject? {
    val res = nexus("$API_URL/security/roles/$name")
    if (res.statusCode() == 404) return null

    return json.decodeFromString(res.body())
}

@VisibleForTesting
internal suspend fun getNexusAssets(repository: String): JsonObject? {
    val res = nexus("$API_URL/assets?repository=$repository")
    if (res.statusCode() == 404) return null

    return json.decodeFromString(res.body())
}