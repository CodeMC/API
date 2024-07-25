package io.codemc.api.jenkins

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

@EnabledIfEnvironmentVariable(named = "JENKINS_USERNAME", matches = "(?!^\$)([^\\s])")
@EnabledIfEnvironmentVariable(named = "JENKINS_PASSWORD", matches = "(?!^\$)([^\\s])")
object TestJenkins {

    @JvmStatic
    @BeforeAll
    fun setup() {
        jenkinsConfig = JenkinsConfig(
            "http://localhost:8080",
            System.getenv("JENKINS_USERNAME"),
            System.getenv("JENKINS_PASSWORD")
        )

        assert(ping())
    }

    @Test
    fun testCreateJenkinsUser() {
        assert(createJenkinsUser("test"))
        assert(getJenkinsUser("test").isNotEmpty())

        assert(createJenkinsUser("MyPlayer123"))
        assert(getJenkinsUser("MyPlayer123").isNotEmpty())
    }

    @Test
    fun testCreateJenkinsJob() {
        val name = "TestUsername"
        val url = "https://github.com/TestUsername/TestRepo"

        assert(createJenkinsUser(name))
        assert(getJenkinsUser(name).isNotEmpty())

        assert(createJenkinsJob(name, "TestJob_Freestyle", url, true))
        assert(getJenkinsJob(name, "TestJob_Freestyle").isNotEmpty())
        assert(createJenkinsJob(name, "TestJob2_Freestyle", url, true))
        assert(getJenkinsJob(name, "TestJob2_Freestyle").isNotEmpty())

        assert(createJenkinsJob(name, "TestJob_Maven", url, false))
        assert(getJenkinsJob(name, "TestJob_Maven").isNotEmpty())
        assert(createJenkinsJob(name, "TestJob2_Maven", url, false))
        assert(getJenkinsJob(name, "TestJob2_Maven").isNotEmpty())
    }

}