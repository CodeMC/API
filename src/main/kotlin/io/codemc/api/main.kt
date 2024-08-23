@file:JvmName("CodeMCAPI")

package io.codemc.api

import io.codemc.api.database.DBConfig
import io.codemc.api.database.dbConfig
import io.codemc.api.jenkins.JenkinsConfig
import io.codemc.api.jenkins.jenkinsConfig
import io.codemc.api.nexus.NexusConfig
import io.codemc.api.nexus.nexusConfig

/**
 * Initializes the CodeMC API.
 * @param jenkins The Jenkins configuration.
 * @param nexus The Nexus configuration.
 * @param db The database configuration.
 */
suspend fun initialize(
    jenkins: JenkinsConfig,
    nexus: NexusConfig,
    db: DBConfig
) {
    loadResources()

    jenkinsConfig = jenkins
    nexusConfig = nexus
    dbConfig = db
}