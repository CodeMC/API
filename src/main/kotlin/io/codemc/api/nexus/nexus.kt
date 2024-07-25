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
            = Base64.getEncoder().encodeToString("$username:$password".toByteArray())

    val authorization = "Basic $token"
}

data class NexusRepository(
    val name: String,
    val format: String,
    val type: String,
    val url: String
)

data class NexusRole(
    val id: String,
    val source: String,
    val name: String,
    val description: String,
    val readOnly: Boolean,
    val privileges: List<String>
)

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

suspend fun createNexus(name: String) = withContext(Dispatchers.IO) {
    createNexusRepository(name)
    createNexusUser(name, UUID.randomUUID().toString())
}

private suspend fun createNexusRepository(name: String): Boolean {
    val repo = createMavenRepository(name)
    val response = req("$API_URL/repositories/maven/hosted") {
        POST(HttpRequest.BodyPublishers.ofString(repo))
        header("Content-Type", "application/json")
    }

    return response.statusCode() == 201
}

@VisibleForTesting
internal suspend fun getRepositories(): List<NexusRepository> {
    val text = req("$API_URL/repositories").body()
    return json.decodeFromString(text)
}

@OptIn(ExperimentalSerializationApi::class)
private suspend fun createNexusUser(name: String, password: String): Boolean {
    // Add Role
    val roleId = name.lowercase()
    val role = buildJsonObject {
        put("id", roleId)
        put("name", name)
        put("description", "Role for $name")
        putJsonArray("privileges") {
            addAll(getNexusRoles(name))
        }
    }.toString()

    val roleReq = req("$API_URL/security/roles") {
        POST(HttpRequest.BodyPublishers.ofString(role))
        header("Content-Type", "application/json")
    }
    if (roleReq.statusCode() != 200) return false

    // Add User with Role
    val user = buildJsonObject {
        put("userId", name)
        put("firstName", name)
        put("password", password)
        putJsonArray("roles") {
            add(roleId)
        }
    }.toString()

    val userRes = req("$API_URL/security/users") {
        POST(HttpRequest.BodyPublishers.ofString(user))
        header("Content-Type", "application/json")
    }

    return userRes.statusCode() == 200
}

@VisibleForTesting
internal suspend fun getNexusRole(name: String): NexusRole {
    val text = req("$API_URL/security/roles/$name").body()
    return json.decodeFromString(text)
}