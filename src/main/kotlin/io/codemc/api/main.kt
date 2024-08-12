@file:JvmName("CodeMCAPI")

package io.codemc.api

import io.codemc.api.database.DBConfig
import io.codemc.api.database.dbConfig
import io.codemc.api.jenkins.JenkinsConfig
import io.codemc.api.jenkins.jenkinsConfig
import io.codemc.api.nexus.NexusConfig
import io.codemc.api.nexus.nexusConfig

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