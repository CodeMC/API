@file:JvmName("JenkinsAPI")

package io.codemc.api.jenkins

import com.cdancy.jenkins.rest.JenkinsClient
import io.codemc.api.JOB_FREESTYLE
import io.codemc.api.JOB_MAVEN
import io.codemc.api.RESOURCE_CACHE
import io.codemc.api.USER_CONFIG

var jenkinsConfig: JenkinsConfig = JenkinsConfig("", "", "")
    set(value) {
        field = value

        client = JenkinsClient.builder()
            .endPoint(value.url)
            .credentials("${value.username}:${value.password}")
            .build()
    }

private lateinit var client: JenkinsClient

data class JenkinsConfig(
    val url: String,
    val username: String,
    val password: String
)

fun ping(): Boolean =
    client.api().systemApi().systemInfo() != null

fun createJenkinsUser(username: String): Boolean {
    val config = RESOURCE_CACHE[USER_CONFIG]?.replace("{USERNAME}", username) ?: return false
    val status = client.api().jobsApi().create("/", username, config)

    return status.value()
}

fun createJenkinsJob(username: String, jobName: String, repoLink: String, isFreestyle: Boolean): Boolean {
    val template = (if (isFreestyle) RESOURCE_CACHE[JOB_FREESTYLE] else RESOURCE_CACHE[JOB_MAVEN])
        ?.replace("{PROJECT_URL}", repoLink) ?: return false

    // Jenkins will automatically add job to the URL
    val status = client.api().jobsApi().create(username, jobName, template)
    return status.value()
}