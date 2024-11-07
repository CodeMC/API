package io.codemc.api.jenkins

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

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
)

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

    constructor(json: JsonObject) : this(
        json["result"]!!.jsonPrimitive.content,
        json["number"]!!.jsonPrimitive.int,
        json["url"]?.jsonPrimitive?.content,
        json["timestamp"]!!.jsonPrimitive.long
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