@file:JvmName("JenkinsAPI")

package io.codemc.api.jenkins

import com.cdancy.jenkins.rest.JenkinsClient
import io.codemc.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import java.net.http.HttpRequest

var jenkinsConfig: JenkinsConfig = JenkinsConfig("", "", "")
    set(value) {
        field = value

        val client0 = JenkinsClient.builder()
            .endPoint(value.url)

        if (value.username.isNotEmpty() && value.password.isNotEmpty())
            client0.credentials("${value.username}:${value.password}")

        client = client0.build()
    }

private lateinit var client: JenkinsClient

data class JenkinsConfig(
    val url: String,
    val username: String,
    val password: String
)

fun ping(): Boolean =
    client.api().systemApi().systemInfo().jenkinsVersion() != null

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
        .replace("{USERNAME}", username.lowercase())
        .replace("{PASSWORD}", password)

    val store = req("${jenkinsConfig.url}/job/$username/credentials/store/folder/domain/Services/createCredentials") {
        POST(HttpRequest.BodyPublishers.ofString(storeConfig))

        header("Authorization", "Basic ${client.authValue()}")
        header("Content-Type", "application/xml")
    }

    return store.statusCode() == 200
}

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

fun getJenkinsUser(username: String): String {
    val user = client.api().jobsApi().config("/", username)
    return user ?: ""
}

fun getAllJenkinsUsers(): List<String>
    = client.api().jobsApi().jobList("/").jobs().map { it.name() }

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

fun getJobInfo(username: String, jobName: String): JenkinsJob? {
    val job = client.api().jobsApi().jobInfo("/", "$username/job/$jobName")
    return if (job == null) null else JenkinsJob(job)
}

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

fun deleteUser(username: String): Boolean {
    val status = client.api().jobsApi().delete("/", username)

    if (status.errors().isNotEmpty())
        println(status.errors())

    return status.value()
}

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

suspend fun isFreestyle(url: String): Boolean = withContext(Dispatchers.IO) {
    !filesExists(url, nonFreestyles)
}