@file:JvmName("JenkinsAPI")

package io.codemc.api.jenkins

import com.cdancy.jenkins.rest.JenkinsClient
import io.codemc.api.JOB_FREESTYLE
import io.codemc.api.JOB_MAVEN
import io.codemc.api.RESOURCE_CACHE
import io.codemc.api.USER_CONFIG
import org.jetbrains.annotations.VisibleForTesting

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

fun createJenkinsUser(username: String, password: String): Boolean {
    val config0 = RESOURCE_CACHE[USER_CONFIG] ?: return false
    val config = config0
        .replace("{USERNAME}", username)
        .replace("{PASSWORD}", password)
    val status = client.api().jobsApi().create("/", username, config)

    return status.value()
}

@VisibleForTesting
internal fun getJenkinsUser(username: String): String {
    val user = client.api().jobsApi().config("/", username)
    return user ?: ""
}

fun createJenkinsJob(username: String, jobName: String, repoLink: String, isFreestyle: Boolean): Boolean {
    val template = (if (isFreestyle) RESOURCE_CACHE[JOB_FREESTYLE] else RESOURCE_CACHE[JOB_MAVEN])
        ?.replace("{PROJECT_URL}", repoLink) ?: return false

    // Jenkins will automatically add job to the URL
    val status = client.api().jobsApi().create(username, jobName, template)
    return status.value()
}

@VisibleForTesting
internal fun getJenkinsJob(username: String, jobName: String): String {
    val job = client.api().jobsApi().config("/", "$username/job/$jobName")
    return job ?: ""
}