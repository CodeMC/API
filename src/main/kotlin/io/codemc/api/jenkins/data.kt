package io.codemc.api.jenkins

import com.cdancy.jenkins.rest.domain.job.BuildInfo
import com.cdancy.jenkins.rest.domain.job.JobInfo

/**
 * Represents a Jenkins job.
 * @property name The name of the job.
 * @property url The URL of the job.
 * @property description The description of the job.
 * @property lastBuild The last build of the job.
 * @property lastCompletedBuild The last completed build of the job.
 * @property lastFailedBuild The last failed build of the job.
 * @property lastStableBuild The last stable build of the job.
 */
data class JenkinsJob(
    val name: String,
    val url: String?,
    val description: String?,
    val lastBuild: JenkinsBuild?,
    val lastCompletedBuild: JenkinsBuild?,
    val lastFailedBuild: JenkinsBuild?,
    val lastStableBuild: JenkinsBuild?
) {

    /**
     * Creates a Jenkins job from a [JobInfo] object.
     * @param info The job info.
     */
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

/**
 * Represents a Jenkins build.
 * @property result The result of the build.
 * @property number The number of the build.
 * @property url The URL of the build.
 * @property timestamp The timestamp of the build.
 */
data class JenkinsBuild(
    val result: String,
    val number: Int,
    val url: String?,
    val timestamp: Long
) {

    /**
     * Creates a Jenkins build from a [BuildInfo] object.
     * @param info The build info.
     * @see BuildInfo
     */
    constructor(info: BuildInfo) : this(
        info.result() ?: "Unknown",
        info.number() + 1,
        info.url(),
        info.timestamp()
    )

    /**
     * Returns a string representation of the build in Discord Message format.
     * @return The string representation.
     */
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