package io.codemc.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// IDs

/**
 * The ID of the jenkins freestyle job template.
 */
const val JOB_FREESTYLE = "job-freestyle"

/**
 * The ID of the jenkins maven job template.
 */
const val JOB_MAVEN = "job-maven"

/**
 * The ID of the jenkins user config template.
 */
const val USER_CONFIG = "user-config"

/**
 * The ID of the jenkins credentials template.
 */
const val CREDENTIALS = "credentials"

/**
 * The ID of the jenkins credentials domain template.
 */
const val CREDENTIALS_DOMAIN = "credentials-domain"

/**
 * The ID of the maven settings template for jenkins credentials.
 */
const val MAVEN_SETTINGS_XML = "maven-settings"

// Fields

/**
 * The resource cache of loaded resources, mapped by their ID.
 */
val RESOURCE_CACHE = mutableMapOf<String, String>()
private val resources = mapOf(
    "/templates/jenkins/job-freestyle.xml" to JOB_FREESTYLE,
    "/templates/jenkins/job-maven.xml" to JOB_MAVEN,
    "/templates/jenkins/user-config.xml" to USER_CONFIG,
    "/templates/jenkins/credentials.xml" to CREDENTIALS,
    "/templates/jenkins/credentials-domain.xml" to CREDENTIALS_DOMAIN,
    "/templates/jenkins/maven/settings.xml" to MAVEN_SETTINGS_XML
)

/**
 * Loads the resources into the [RESOURCE_CACHE].
 * @throws [IllegalStateException] if a resource is not found.
 */
suspend fun loadResources() = withContext(Dispatchers.IO) {
    if (RESOURCE_CACHE.isNotEmpty()) return@withContext

    for ((path, id) in resources) {
        launch {
            val file = object {}.javaClass.getResourceAsStream(path) ?: error("Resource not found: $path")
            RESOURCE_CACHE[id] = file.bufferedReader().use { it.readText() }
        }
    }
}