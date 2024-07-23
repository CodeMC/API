@file:JvmName("CodeMCAPI")

package io.codemc.api

import io.codemc.api.jenkins.JenkinsConfig
import io.codemc.api.jenkins.jenkinsConfig

suspend fun initialize(
    jenkins: JenkinsConfig
) {
    loadResources()

    jenkinsConfig = jenkins
}