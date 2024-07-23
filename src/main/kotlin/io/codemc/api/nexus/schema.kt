package io.codemc.api.nexus

data class NexusRepository(
    val name: String,
    val format: String,
    val type: String,
    val url: String
)