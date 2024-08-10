package io.codemc.api.jenkins

import com.cdancy.jenkins.rest.domain.job.BuildInfo
import com.cdancy.jenkins.rest.domain.job.JobInfo

data class JenkinsJob(
    val name: String,
    val url: String?,
    val description: String?,
    val lastBuild: JenkinsBuild?,
    val lastCompleteBuild: JenkinsBuild?,
    val lastFailedBuild: JenkinsBuild?,
    val lastStableBuild: JenkinsBuild?
) {

    constructor(info: JobInfo) : this(
        info.displayNameOrNull() ?: info.name(),
        info.url(),
        info.description(),
        tryBuild(info.lastBuild()),
        tryBuild(info.lastCompleteBuild()),
        tryBuild(info.lastFailedBuild()),
        tryBuild(info.lastStableBuild())
    )

}

private fun tryBuild(info: BuildInfo?) = info?.let { JenkinsBuild(it) }
data class JenkinsBuild(
    val result: String,
    val number: Int,
    val url: String?,
    val timestamp: Long
) {

    constructor(info: BuildInfo) : this(
        info.result() ?: "Unknown",
        info.number(),
        info.url(),
        info.timestamp()
    )

    override fun toString(): String {
        val title = when {
            url != null && number != 0 -> "[Build #$number]($url)"
            url != null -> "[Unknown Build]($url)"
            number != 0 -> "Build #$number"
            else -> "Unknown Build"
        }
        val timestamp = if (timestamp == 0L) "Sometime" else "<t:$timestamp:f>"

        return "$result\n$title - $timestamp\n"
    }

}