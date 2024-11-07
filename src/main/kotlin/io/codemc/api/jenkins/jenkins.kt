@file:JvmName("JenkinsAPI")

package io.codemc.api.jenkins

import io.codemc.api.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.jetbrains.annotations.VisibleForTesting
import java.net.http.HttpRequest
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The Jenkins configuration.
 * @property url The URL to the Jenkins Instance.
 * @property username The Jenkins username.
 * @property token The Jenkins API token.
 */
data class JenkinsConfig(
    val url: String,
    val username: String,
    val token: String
) {
    /**
     * The Base64-Encoded value of `$username:$token`
     */
    private val auth: String
        get() = Base64.getEncoder().encodeToString("$username:$token".toByteArray())

    /**
     * The authorization header based on the [username] and [token].
     */
    val authorization
        get() = "Basic $auth"
}

/**
 * The [JenkinsConfig] instance.
 */
lateinit var jenkinsConfig: JenkinsConfig

/**
 * Sends an HTTP request using the [JenkinsConfig.authorization] header.
 * @param url The URL to send the request to.
 * @param request The builder modifier on the HTTP Request.
 * @return An HTTP Response.
 * @see [req]
 */
suspend fun jenkins(url: String, request: HttpRequest.Builder.() -> Unit = { GET() }) = req(url) {
    header("Authorization", jenkinsConfig.authorization)
    request(this)
}

/**
 * Pings the Jenkins server.
 * @return `true` if the server is reachable, `false` otherwise.
 */
fun ping(): Boolean = runBlocking(Dispatchers.IO) {
    val res = jenkins("${jenkinsConfig.url}/api/")
    return@runBlocking res.statusCode().isSuccess
}

// Credentials API Documentation:
// https://github.com/jenkinsci/credentials-plugin/blob/master/docs/user.adoc

/**
 * The Jenkins Credentials ID for the Nexus Credentials.
 */
const val NEXUS_CREDENTIALS_ID = "nexus-repository"

/**
 * The Jenkins Credentials Description for the Nexus Credentials.
 */
const val NEXUS_CREDENTIALS_DESCRIPTION = "Your Nexus Login Details"

internal suspend fun setCredentials(username: String, password: String): Boolean {
    // Create Credentials Domain
    val checkDomain = jenkins("${jenkinsConfig.url}/job/$username/credentials/store/folder/domain/Services/config.xml")

    if (checkDomain.statusCode() == 404) {
        val domainConfig = RESOURCE_CACHE[CREDENTIALS_DOMAIN] ?: return false
        val domain = jenkins("${jenkinsConfig.url}/job/$username/credentials/store/folder/createDomain") {
            POST(HttpRequest.BodyPublishers.ofString(domainConfig))

            header("Content-Type", "application/xml")
        }

        if (!domain.statusCode().isSuccess) return false
    }

    // Create Credentials Store
    val storeConfig = (RESOURCE_CACHE[CREDENTIALS] ?: return false)
        .replace("{ID}", NEXUS_CREDENTIALS_ID)
        .replace("{DESCRIPTION}", NEXUS_CREDENTIALS_DESCRIPTION)
        .replace("{USERNAME}", username.lowercase())
        .replace("{PASSWORD}", password)

    val store = jenkins("${jenkinsConfig.url}/job/$username/credentials/store/folder/domain/Services/createCredentials") {
        POST(HttpRequest.BodyPublishers.ofString(storeConfig))

        header("Content-Type", "application/xml")
    }

    // Update if Already Exists
    if (store.statusCode() == 409) {
        val update = jenkins("${jenkinsConfig.url}/job/$username/credentials/store/folder/domain/Services/credential/$NEXUS_CREDENTIALS_ID/config.xml") {
            POST(HttpRequest.BodyPublishers.ofString(storeConfig))

            header("Content-Type", "application/xml")
        }

        return update.statusCode().isSuccess
    } else
        return store.statusCode().isSuccess
}

/**
 * Checks if the Jenkins credentials exist, and creates them if they don't.
 * @param username The username of the user.
 * @param password The password of the user.
 */
fun checkCredentials(username: String, password: String) = runBlocking(Dispatchers.IO) {
    val checkDomain = jenkins("${jenkinsConfig.url}/job/$username/credentials/store/folder/domain/Services/config.xml")
    val checkStore = jenkins("${jenkinsConfig.url}/job/$username/credentials/store/folder/domain/Services/credential/$NEXUS_CREDENTIALS_ID/config.xml")

    if (checkDomain.statusCode() == 404 || checkStore.statusCode() == 404) {
        setCredentials(username, password)
    }
}

/**
 * Checks the user `config.xml` present on the Jenkins CI.
 * @param username The username of the user.
 * @return `true` if the user configuration was changed, `false` otherwise.
 */
fun checkUserConfig(username: String): Boolean = runBlocking(Dispatchers.IO) {
    val xml = jenkins("${jenkinsConfig.url}/job/$username/config.xml").body()
    var changed = false

    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val doc = builder.parse(xml.byteInputStream())

    // Check Maven Settings/
    if (!xml.contains("<id>nexus-login</id>")) {
        val settings = RESOURCE_CACHE[MAVEN_SETTINGS_XML] ?: return@runBlocking false
        val configs = doc.getElementsByTagName("configs").item(0)

        configs.appendChild(builder.parse(settings.byteInputStream()).documentElement)
        changed = true
    }

    if (changed) {
        val res = jenkins("${jenkinsConfig.url}/job/$username/config.xml") {
            POST(HttpRequest.BodyPublishers.ofString(doc.textContent))

            header("Content-Type", "text/xml")
        }

        return@runBlocking res.statusCode().isSuccess
    }

    return@runBlocking true
}

/**
 * Changes the Jenkins password for a user.
 * @param username The username of the user.
 * @param newPassword The new password.
 * @return `true` if the password was changed, `false` otherwise.
 */
fun changeJenkinsPassword(username: String, newPassword: String): Boolean = runBlocking(Dispatchers.IO) {
    return@runBlocking setCredentials(username, newPassword)
}

/**
 * Creates a Jenkins user.
 * @param username The username of the user.
 * @param password The password of the user.
 * @return `true` if the user was created, `false` otherwise.
 */
fun createJenkinsUser(username: String, password: String): Boolean = runBlocking(Dispatchers.IO) {
    if (getJenkinsUser(username).isNotEmpty()) return@runBlocking false

    val config0 = RESOURCE_CACHE[USER_CONFIG] ?: return@runBlocking false

    val config = config0
        .replace("{USERNAME}", username)

    val res = jenkins("${jenkinsConfig.url}/createItem?name=$username") {
        POST(HttpRequest.BodyPublishers.ofString(config))

        header("Content-Type", "application/xml")
    }

    if (!res.statusCode().isSuccess) return@runBlocking false
    return@runBlocking setCredentials(username, password)
}

/**
 * Gets a Jenkins user's configuration.
 * @param username The username of the user.
 * @return The user's configuration in XML format.
 */
fun getJenkinsUser(username: String): String = runBlocking(Dispatchers.IO) {
    val res = jenkins("${jenkinsConfig.url}/job/$username/config.xml")
    if (res.statusCode() == 404) return@runBlocking ""

    return@runBlocking res.body() ?: ""
}

/**
 * Gets all Jenkins users.
 * @return A list of all Jenkins users mapped by their username.
 */
fun getAllJenkinsUsers(): List<String> = runBlocking(Dispatchers.IO) {
    val users = jenkins("${jenkinsConfig.url}/api/json?tree=jobs[name]").body()
    val obj = json.decodeFromString<JsonObject>(users)

    return@runBlocking obj["jobs"]?.jsonArray?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content } ?: emptyList()
}

/**
 * Creates a Jenkins job.
 * @param username The username of the user to create the job at.
 * @param jobName The name of the job.
 * @param repoLink The Git URL to the repository.
 * @param isFreestyle `true` if the job is a freestyle job, `false` otherwise. A freestyle job is defined as a job that isn't built with Maven.
 * @param config A function to modify the XML configuration of the job.
 */
@JvmOverloads
fun createJenkinsJob(
    username: String,
    jobName: String,
    repoLink: String,
    isFreestyle: Boolean,
    config: (String) -> String = { it }
): Boolean = runBlocking(Dispatchers.IO) {
    if (getJenkinsJob(username, jobName).isNotEmpty()) return@runBlocking false

    val template = (if (isFreestyle) RESOURCE_CACHE[JOB_FREESTYLE] else RESOURCE_CACHE[JOB_MAVEN])
        ?.replace("{PROJECT_URL}", repoLink) ?: return@runBlocking false

    val job = config(template)
    val res = jenkins("${jenkinsConfig.url}/job/$username/createItem?name=$jobName") {
        POST(HttpRequest.BodyPublishers.ofString(job))

        header("Content-Type", "application/xml")
    }

    return@runBlocking res.statusCode().isSuccess
}

@VisibleForTesting
internal fun getJenkinsJob(username: String, jobName: String): String = runBlocking(Dispatchers.IO) {
    val res = jenkins("${jenkinsConfig.url}/job/$username/job/$jobName/config.xml")
    if (res.statusCode() == 404) return@runBlocking ""

    return@runBlocking res.body() ?: ""
}

/**
 * Gets the information of a Jenkins job.
 * @param username The username of the user.
 * @param jobName The name of the job.
 * @return The job information, or `null` if the job doesn't exist.
 */
fun getJobInfo(username: String, jobName: String): JenkinsJob? = runBlocking(Dispatchers.IO) {
    val res = jenkins("${jenkinsConfig.url}/job/$username/job/$jobName/api/json")
    if (res.statusCode() == 404) return@runBlocking null

    val job = res.body() ?: return@runBlocking null
    val obj = json.decodeFromString<JsonObject>(job)

    val name = obj["name"]?.jsonPrimitive?.content ?: return@runBlocking null
    val url = obj["url"]?.jsonPrimitive?.content
    val description = obj["description"]?.jsonPrimitive?.content

    val getBuild: suspend (String) -> Deferred<JenkinsBuild?> = {
        async {
            val build = (obj[it] as? JsonObject)?.get("url")?.jsonPrimitive?.content ?: return@async null
            val buildInfo = jenkins("$build/api/json").body() ?: return@async null
            if (buildInfo.isEmpty()) return@async null

            val buildObj = json.decodeFromString<JsonElement>(buildInfo)
            if (buildObj !is JsonObject) return@async null

            JenkinsBuild(buildObj)
        }
    }

    val lastBuild = getBuild("lastBuild")
    val lastCompletedBuild = getBuild("lastCompletedBuild")
    val lastFailedBuild = getBuild("lastFailedBuild")
    val lastStableBuild = getBuild("lastStableBuild")

    return@runBlocking JenkinsJob(
        name,
        url,
        description,
        lastBuild.await(),
        lastCompletedBuild.await(),
        lastFailedBuild.await(),
        lastStableBuild.await()
    )
}

/**
 * Triggers a build for a Jenkins job.
 * @param username The username of the user.
 * @param jobName The name of the job.
 * @return `true` if the build was triggered, `false` otherwise.
 */
fun triggerBuild(username: String, jobName: String): Boolean = runBlocking(Dispatchers.IO) {
    val res = jenkins("${jenkinsConfig.url}/job/$username/job/$jobName/build") {
        POST(HttpRequest.BodyPublishers.noBody())
    }

    return@runBlocking res.statusCode().isSuccess
}

@VisibleForTesting
internal fun isBuilding(username: String, jobName: String): Boolean = runBlocking(Dispatchers.IO) {
    val job = jenkins("${jenkinsConfig.url}/job/$username/job/$jobName/api/json").body()
    val obj = json.decodeFromString<JsonObject>(job)

    val isAnimated = obj["color"]?.jsonPrimitive?.content?.lowercase()?.contains("anime") ?: false
    val isInQueue = obj["inQueue"]?.jsonPrimitive?.boolean ?: false

    return@runBlocking isAnimated || isInQueue
}

/**
 * Deletes a Jenkins user.
 * @param username The username of the user.
 * @return `true` if the user was deleted, `false` otherwise.
 */
fun deleteUser(username: String): Boolean = runBlocking(Dispatchers.IO) {
    val res = jenkins("${jenkinsConfig.url}/job/$username/") { DELETE() }
    return@runBlocking res.statusCode().isSuccess
}

/**
 * Deletes a Jenkins job.
 * @param username The username of the user.
 * @param jobName The name of the job.
 */
fun deleteJob(username: String, jobName: String): Boolean = runBlocking(Dispatchers.IO) {
    val res = jenkins("${jenkinsConfig.url}/job/$username/job/$jobName/") { DELETE() }
    return@runBlocking res.statusCode().isSuccess
}

private val nonFreestyles = listOf(
    "pom.xml",
    "dependency-reduced-pom.xml"
)

/**
 * Checks if a Git repository is a freestyle project.
 * @param url The URL to the Git repository.
 * @return `true` if the project is a freestyle project, `false` otherwise.
 */
fun isFreestyle(url: String): Boolean = runBlocking { !filesExists(url, nonFreestyles) }