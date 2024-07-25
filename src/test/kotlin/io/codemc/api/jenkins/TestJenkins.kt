package io.codemc.api.jenkins

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

object TestJenkins {

    init {
        jenkinsConfig = JenkinsConfig(
            url = "http://localhost:8080",
            username = "",
            password = ""
        )
    }

    @JvmStatic
    @BeforeAll
    fun testPing() {
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