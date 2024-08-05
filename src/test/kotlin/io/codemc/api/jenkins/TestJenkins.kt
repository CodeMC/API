package io.codemc.api.jenkins

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class TestJenkins {

    companion object {

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
            assertTrue(ping())
        }

    }

    @Test
    fun testCreateJenkinsUser() {
        assertTrue(createJenkinsUser("test", "test_password"))
        assertTrue(getJenkinsUser("test").isNotEmpty())
        assertTrue(deleteUser("test"))
        assertTrue(getJenkinsUser("test").isEmpty())

        assertTrue(createJenkinsUser("MyPlayer123", "MyPassword456"))
        assertTrue(getJenkinsUser("MyPlayer123").isNotEmpty())
        assertTrue(deleteUser("MyPlayer123"))
        assertTrue(getJenkinsUser("MyPlayer123").isEmpty())
    }

    @Test
    fun testCreateJenkinsJob() {
        val name = "TestUsername"
        val url = "https://github.com/TestUsername/TestRepo"

        assertTrue(createJenkinsUser(name, "TestPassword"))
        assertTrue(getJenkinsUser(name).isNotEmpty())

        assertTrue(createJenkinsJob(name, "TestJob_Freestyle", url, true))
        assertTrue(getJenkinsJob(name, "TestJob_Freestyle").isNotEmpty())

        assertTrue(createJenkinsJob(name, "TestJob2_Freestyle", url, true))
        assertTrue(getJenkinsJob(name, "TestJob2_Freestyle").isNotEmpty())
        assertTrue(deleteJob(name, "TestJob2_Freestyle"))
        assertTrue(getJenkinsJob(name, "TestJob2_Freestyle").isEmpty())

        assertTrue(createJenkinsJob(name, "TestJob_Maven", url, false))
        assertTrue(getJenkinsJob(name, "TestJob_Maven").isNotEmpty())

        assertTrue(createJenkinsJob(name, "TestJob2_Maven", url, false))
        assertTrue(getJenkinsJob(name, "TestJob2_Maven").isNotEmpty())
        assertTrue(deleteJob(name, "TestJob2_Maven"))
        assertTrue(getJenkinsJob(name, "TestJob2_Maven").isEmpty())

        assertTrue(deleteUser(name))
        assertTrue(getJenkinsUser(name).isEmpty())
    }

    @Test
    fun testIsFreestyle() = runBlocking(Dispatchers.IO) {
        val u1 = "https://github.com/CodeMC/API.git"
        assertTrue(isFreestyle(u1))

        val u2 = "https://github.com/jenkins-docs/simple-java-maven-app.git"
        assertFalse(isFreestyle(u2))

        val u3 = "https://bitbucket.org/jeyvison_andrade/gradle_tutorial.git"
        assertTrue(isFreestyle(u3))

        val u4 = "https://gitlab.com/gitlab-examples/maven/simple-maven-app.git/"
        assertFalse(isFreestyle(u4))
    }

}