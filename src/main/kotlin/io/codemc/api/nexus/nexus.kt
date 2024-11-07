@file:JvmName("NexusAPI")

package io.codemc.api.nexus

import io.codemc.api.isSuccess
import io.codemc.api.json
import io.codemc.api.req
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import org.jetbrains.annotations.VisibleForTesting
import java.net.http.HttpRequest
import java.util.*

// Schema

/**
 * The Nexus configuration.
 * @property url The URL to the Nexus Instance.
 * @property username The username of the admin user.
 * @property password The password of the admin user.
 */
data class NexusConfig(
    val url: String,
    val username: String,
    val password: String
) {
    /**
     * The Base64-Encoded value of `$username:$password`
     */
    private val token: String
        get() = Base64.getEncoder().encodeToString("$username:$password".toByteArray())

    /**
     * The authorization header based on the [username] and [password].
     */
    val authorization
        get() = "Basic $token"
}

// Fields

/**
 * The [NexusConfig] instance.
 */
lateinit var nexusConfig: NexusConfig

// Implementation

/**
 * Sends an HTTP request using the [NexusConfig.authorization] header.
 * @param url The URL to send the request to.
 * @param request The builder modifier on the HTTP Request.
 * @return An HTTP Response.
 * @see [req]
 */
suspend fun nexus(url: String, request: HttpRequest.Builder.() -> Unit = { GET() }) = req(url) {
    header("Authorization", nexusConfig.authorization)
    request(this)
}

/**
 * Pings the Nexus Instance.
 * @return `true` if currently available, `false` otherwise
 */
fun ping(): Boolean = runBlocking {
    val text = nexus("$API_URL/status")
    return@runBlocking text.statusCode().isSuccess
}

/**
 * Creates a Nexus User with the necessary data.
 *
 * This creates the user account, role, and repository with necessary credentials.
 * @param name The name of the user
 * @param password The password for the user
 * @return `true` if successfully created, `false` otherwise.
 */
@OptIn(ExperimentalSerializationApi::class)
fun createNexus(name: String, password: String) = runBlocking(Dispatchers.IO) {
    // Create User Repository
    val id = name.lowercase()
    if (getNexusRepository(id) == null) {
        val repo = createMavenRepository(name)
        val repoResponse = nexus("$API_URL/repositories/maven/hosted") {
            POST(HttpRequest.BodyPublishers.ofString(repo))
            header("Content-Type", "application/json")
        }

        if (!repoResponse.statusCode().isSuccess) return@runBlocking false
    }

    // Add Role
    if (getNexusRole(id) == null) {
        val role = buildJsonObject {
            put("id", id)
            put("name", name)
            put("description", "Role for $name")
            putJsonArray("privileges") {
                addAll(getNexusRoles(id))
            }
        }.toString()

        val roleReq = nexus("$API_URL/security/roles") {
            POST(HttpRequest.BodyPublishers.ofString(role))
            header("Content-Type", "application/json")
        }

        if (!roleReq.statusCode().isSuccess) return@runBlocking false
    }

    // Add User with Role
    if (getNexusUser(id) == null) {
        val user = buildJsonObject {
            put("userId", id)
            put("firstName", name)
            put("lastName", "User")
            put("emailAddress", "$name@users.noreply.github.com") // Can't actually receive mail
            put("status", "active")
            put("password", password)
            putJsonArray("roles") {
                add(id)
            }
        }.toString()

        val userRes = nexus("$API_URL/security/users") {
            POST(HttpRequest.BodyPublishers.ofString(user))
            header("Content-Type", "application/json")
        }

        if (!userRes.statusCode().isSuccess) return@runBlocking false
    }

    return@runBlocking true
}

/**
 * Changes the password linked to the Nexus User.
 * @param name The name of the user to change
 * @param newPassword The new password for the user
 * @return `true` if the change was successful, `false` otherwise
 */
fun changeNexusPassword(name: String, newPassword: String) = runBlocking(Dispatchers.IO) {
    if (getNexusUser(name) == null) {
        createNexus(name, newPassword)
        return@runBlocking true
    }

    val id = name.lowercase()
    val res = nexus("$API_URL/security/users/$id/change-password") {
        PUT(HttpRequest.BodyPublishers.ofString(newPassword))

        header("Content-Type", "text/plain")
    }

    return@runBlocking res.statusCode().isSuccess
}

/**
 * Deletes a Nexus user and its data, removing all artifacts from its repository.
 * @param name The name of the user to delete.
 * @return `true` if the deletion was successful, `false` otherwise
 */
fun deleteNexus(name: String) = runBlocking(Dispatchers.IO) {
    val id = name.lowercase()

    val repoRes = nexus("$API_URL/repositories/$id") { DELETE() }
    if (repoRes.statusCode() != 204) return@runBlocking false

    val roleRes = nexus("$API_URL/security/roles/$id") { DELETE() }
    if (roleRes.statusCode() != 204) return@runBlocking false

    val userRes = nexus("$API_URL/security/users/$id") { DELETE() }
    return@runBlocking userRes.statusCode().isSuccess
}

@VisibleForTesting
internal suspend fun getRepositories(): List<JsonObject> {
    val text = nexus("$API_URL/repositories").body()
    return json.decodeFromString(text)
}

/**
 * Gets a Nexus Repository by its case-sensitive name.
 * @param name The name of the nexus repository
 * @return The repository data in JSON format, or `null` if not found
 */
fun getNexusRepository(name: String): JsonObject? = runBlocking {
    val res = nexus("$API_URL/repositories/$name")
    if (res.statusCode() == 404) return@runBlocking null

    return@runBlocking json.decodeFromString(res.body())
}

/**
 * Gets a Nexus User by its case-sensitive name.
 * @param name The name of the nexus user.
 * @return The User data in JSON format, or `null` if not found
 */
fun getNexusUser(name: String): JsonObject? = runBlocking {
    val res = nexus("$API_URL/security/users?userId=$name")
    if (res.statusCode() == 404) return@runBlocking null

    return@runBlocking (json.decodeFromString(res.body()) as? JsonArray)?.firstOrNull() as? JsonObject
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