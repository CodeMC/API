package io.codemc.api.nexus

lateinit var nexusConfig: NexusConfig

data class NexusConfig(
    val url: String,
    val username: String,
    val password: String
)