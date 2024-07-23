package io.codemc.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val JOB_FREESTYLE = "job-freestyle"
const val JOB_MAVEN = "job-maven"
const val USER_CONFIG = "user-config"

val RESOURCE_CACHE = mutableMapOf<String, String>()
private val resources = mapOf(
    "/templates/jenkins/job-freestyle.xml" to "job-freestyle",
    "/templates/jenkins/job-maven.xml" to "job-maven",
    "/templates/jenkins/user-config.xml" to "user-config",
)

suspend fun loadResources() = withContext(Dispatchers.IO) {
    for ((path, id) in resources) {
        launch {
            val file = object {}.javaClass.getResourceAsStream(path) ?: error("Resource not found: $path")
            RESOURCE_CACHE[id] = file.bufferedReader().use { it.readText() }
        }
    }
}