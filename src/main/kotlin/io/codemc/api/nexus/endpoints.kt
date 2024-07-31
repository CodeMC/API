@file:JvmName("NexusEndpoints")

package io.codemc.api.nexus

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

// Fields

val API_URL
    get() = "${nexusConfig.url}/service/rest/v1"

// Implementation

internal fun createMavenRepository(name: String): String = buildJsonObject {
    put("name", name.lowercase())
    put("online", true)

    putJsonObject("storage") {
        put("blobStoreName", "default")
        put("strictContentTypeValidation", true)
        put("writePolicy", "allow")
    }

    putJsonObject("maven") {
        put("versionPolicy", "MIXED")
        put("layoutPolicy", "STRICT")
    }
}.toString()

internal fun getNexusRoles(name: String): List<String> = listOf(
    "nx-healthcheck-read",
    "nx-search-read",
    "nx-repository-view-*-*-read",
    "nx-repository-view-*-*-browse",
    "nx-repository-admin-maven2-$name-edit"
)