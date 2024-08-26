@file:JvmName("JenkinsAPI")

package io.codemc.api.jenkins

import com.cdancy.jenkins.rest.JenkinsClient
import io.codemc.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.VisibleForTesting
import java.net.http.HttpRequest

/**
 * The [JenkinsConfig] instance.
 */
var jenkinsConfig: JenkinsConfig = JenkinsConfig("", "", "")
    set(value) {
        field = value

        val client0 = JenkinsClient.builder()
            .endPoint(value.url)

        if (value.username.isNotEmpty() && value.token.isNotEmpty())
            client0.credentials("${value.username}:${value.token}")

        client = client0.build()
    }

private lateinit var client: JenkinsClient

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
)

/**
 * Pings the Jenkins server.
 * @return `true` if the server is reachable, `false` otherwise.
 */
fun ping(): Boolean =
    client.api().systemApi().systemInfo().jenkinsVersion() != null

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

internal suspend fun createCredentials(username: String, password: String): Boolean {
    // Create Credentials Domain
    val domainConfig = RESOURCE_CACHE[CREDENTIALS_DOMAIN] ?: return false
    val domain = req("${jenkinsConfig.url}/job/$username/credentials/store/folder/createDomain") {
        POST(HttpRequest.BodyPublishers.ofString(domainConfig))

        header("Authorization", "Basic ${client.authValue()}")
        header("Content-Type", "application/xml")
    }

    if (domain.statusCode() != 200) return false

    // Create Credentials Store
    val storeConfig = (RESOURCE_CACHE[CREDENTIALS] ?: return false)
        .replace("{ID}", NEXUS_CREDENTIALS_ID)
        .replace("{DESCRIPTION}", NEXUS_CREDENTIALS_DESCRIPTION)
        .replace("{USERNAME}", username.lowercase())
        .replace("{PASSWORD}", password)

    val store = req("${jenkinsConfig.url}/job/$username/credentials/store/folder/domain/Services/createCredentials") {
        POST(HttpRequest.BodyPublishers.ofString(storeConfig))

        header("Authorization", "Basic ${client.authValue()}")
        header("Content-Type", "application/xml")
    }

    return store.statusCode() == 200
}

/**
 * Changes the Jenkins password for a user.
 * @param username The username of the user.
 * @param newPassword The new password.
 * @return `true` if the password was changed, `false` otherwise.
 */
suspend fun changeJenkinsPassword(username: String, newPassword: String): Boolean {
    val config = (RESOURCE_CACHE[CREDENTIALS] ?: return false)
        .replace("{ID}", NEXUS_CREDENTIALS_ID)
        .replace("{DESCRIPTION}", NEXUS_CREDENTIALS_DESCRIPTION)
        .replace("{USERNAME}", username.lowercase())
        .replace("{PASSWORD}", newPassword)

    val res = req("${jenkinsConfig.url}/job/$username/credentials/store/folder/domain/Services/credential/$NEXUS_CREDENTIALS_ID/config.xml") {
        POST(HttpRequest.BodyPublishers.ofString(config))

        header("Authorization", "Basic ${client.authValue()}")
        header("Content-Type", "application/xml")
    }

    return res.statusCode() == 200
}

/**
 * Creates a Jenkins user.
 * @param username The username of the user.
 * @param password The password of the user.
 * @return `true` if the user was created, `false` otherwise.
 */
fun createJenkinsUser(username: String, password: String): Boolean = runBlocking(Dispatchers.IO) {
    val config0 = RESOURCE_CACHE[USER_CONFIG] ?: return@runBlocking false

    val config = config0
        .replace("{USERNAME}", username)
    val status = client.api().jobsApi().create("/", username, config)

    if (status.errors().isNotEmpty())
        println(status.errors())

    if (!status.value()) return@runBlocking false

    return@runBlocking createCredentials(username, password)
}

/**
 * Gets a Jenkins user's configuration.
 * @param username The username of the user.
 * @return The user's configuration in XML format.
 */
fun getJenkinsUser(username: String): String {
    val user = client.api().jobsApi().config("/", username)
    return user ?: ""
}

/**
 * Gets all Jenkins users.
 * @return A list of all Jenkins users mapped by their username.
 */
fun getAllJenkinsUsers(): List<String>
    = client.api().jobsApi().jobList("/").jobs().map { it.name() }

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
): Boolean {
    val template = (if (isFreestyle) RESOURCE_CACHE[JOB_FREESTYLE] else RESOURCE_CACHE[JOB_MAVEN])
        ?.replace("{PROJECT_URL}", repoLink) ?: return false

    // Jenkins will automatically add job to the URL
    val status = client.api().jobsApi().create(username, jobName, config(template))

    if (status.errors().isNotEmpty())
        println(status.errors())

    return status.value()
}

@VisibleForTesting
internal fun getJenkinsJob(username: String, jobName: String): String {
    val job = client.api().jobsApi().config("/", "$username/job/$jobName")
    return job ?: ""
}

/**
 * Gets the information of a Jenkins job.
 * @param username The username of the user.
 * @param jobName The name of the job.
 * @return The job information, or `null` if the job doesn't exist.
 */
fun getJobInfo(username: String, jobName: String): JenkinsJob? {
    val job = client.api().jobsApi().jobInfo("/", "$username/job/$jobName")
    return if (job == null) null else JenkinsJob(job)
}

/**
 * Triggers a build for a Jenkins job.
 * @param username The username of the user.
 * @param jobName The name of the job.
 * @return `true` if the build was triggered, `false` otherwise.
 */
fun triggerBuild(username: String, jobName: String): Boolean {
    val status = client.api().jobsApi().build("/", "$username/job/$jobName")
    if (status.errors().isNotEmpty()) {
        println(status.errors())
        return false
    }

    return true
}

@VisibleForTesting
internal fun isBuilding(username: String, jobName: String): Boolean {
    val job = client.api().jobsApi().jobInfo("/", "$username/job/$jobName")
    return (job.color() ?: "").contains("anime") || job.inQueue() || (job.lastBuild()?.building() ?: false)
}

/**
 * Deletes a Jenkins user.
 * @param username The username of the user.
 * @return `true` if the user was deleted, `false` otherwise.
 */
fun deleteUser(username: String): Boolean {
    val status = client.api().jobsApi().delete("/", username)

    if (status.errors().isNotEmpty())
        println(status.errors())

    return status.value()
}

/**
 * Deletes a Jenkins job.
 * @param username The username of the user.
 * @param jobName The name of the job.
 */
fun deleteJob(username: String, jobName: String): Boolean {
    val status = client.api().jobsApi().delete("/", "$username/job/$jobName")

    if (status.errors().isNotEmpty())
        println(status.errors())

    return status.value()
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
suspend fun isFreestyle(url: String): Boolean = !filesExists(url, nonFreestyles)