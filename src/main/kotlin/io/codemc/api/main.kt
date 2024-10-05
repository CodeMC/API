@file:JvmName("CodeMCAPI")

package io.codemc.api

import io.codemc.api.database.DBConfig
import io.codemc.api.database.connect
import io.codemc.api.database.dbConfig
import io.codemc.api.jenkins.JenkinsConfig
import io.codemc.api.jenkins.jenkinsConfig
import io.codemc.api.nexus.NexusConfig
import io.codemc.api.nexus.nexusConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Initializes the CodeMC API.
 * @param jenkins The Jenkins configuration.
 * @param nexus The Nexus configuration.
 * @param db The database configuration.
 */
fun initialize(
    jenkins: JenkinsConfig,
    nexus: NexusConfig,
    db: DBConfig
) = runBlocking {
    launch {
        loadResources()
    }

    jenkinsConfig = jenkins
    nexusConfig = nexus

    launch {
        dbConfig = db
        connect()
    }
}