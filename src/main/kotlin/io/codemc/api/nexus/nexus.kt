package io.codemc.api.nexus

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

private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = true
}

// Implementation

suspend fun ping(): Boolean {
    val text = req("$API_URL/status")
    return text.statusCode() == 200
}

@OptIn(ExperimentalSerializationApi::class)
suspend fun createNexus(name: String, password: String) = withContext(Dispatchers.IO) {
    // Create User Repository
    val repo = createMavenRepository(name)
    val repoResponse = req("$API_URL/repositories/maven/hosted") {
        POST(HttpRequest.BodyPublishers.ofString(repo))
        header("Content-Type", "application/json")
    }

    println(repoResponse.body())
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

    val roleReq = req("$API_URL/security/roles") {
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

    val userRes = req("$API_URL/security/users") {
        POST(HttpRequest.BodyPublishers.ofString(user))
        header("Content-Type", "application/json")
    }
    return@withContext userRes.statusCode() == 200
}

@VisibleForTesting
internal suspend fun getRepositories(): List<JsonObject> {
    val text = req("$API_URL/repositories").body()
    return json.decodeFromString(text)
}

@VisibleForTesting
internal suspend fun getNexusRole(name: String): JsonObject {
    val text = req("$API_URL/security/roles/$name").body()
    return json.decodeFromString(text)
}