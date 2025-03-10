package io.codemc.api.jenkins

import io.codemc.api.RESOURCE_CACHE
import io.codemc.api.USER_CONFIG
import io.codemc.api.loadResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class TestJenkins {

    companion object {

        init {
            jenkinsConfig = JenkinsConfig(
                url = "http://localhost:8080",
                username = "admin",
                token = "00000000000000000000000000000000"
            )

            runBlocking(Dispatchers.IO) {
                loadResources()
            }
        }

        @JvmStatic
        @BeforeAll
        fun testPing() {
            assertTrue(ping())
        }

    }

    @Test
    fun testCreateJenkinsUser() {
        val u1 = "test"
        assertTrue(createJenkinsUser(u1, "test_password"))
        checkUserConfig(u1)

        assertTrue(getJenkinsUser(u1).isNotEmpty())
        assertTrue(existsUser(u1))
        assertTrue(deleteUser(u1))
        assertTrue(getJenkinsUser(u1).isEmpty())
        assertFalse(existsUser(u1))

        val u2 = "MyPlayer123"
        assertTrue(createJenkinsUser(u2, "MyPassword456"))
        checkUserConfig(u2)

        assertTrue(getJenkinsUser(u2).isNotEmpty())
        assertTrue(deleteUser(u2))
        assertTrue(getJenkinsUser(u2).isEmpty())
    }

    @Test
    fun testCreateMultipleJenkinsUsers() {
        val users = listOf(
            "User1",
            "user2",
            "Player2",
            "MyAuthor66_",
            "XxCrazyDudeXX"
        )

        users.forEach { user ->
            assertTrue(createJenkinsUser(user, "${user}_password"))
            assertTrue(getJenkinsUser(user).isNotEmpty())
        }

        assertTrue(getAllJenkinsUsers().size >= users.size) // For Concurrent Tests

        users.forEach { user ->
            assertTrue(deleteUser(user))
            assertTrue(getJenkinsUser(user).isEmpty())
        }
    }

    @Test
    fun testCreateJenkinsJob() {
        val name = "TestUsername"
        val url = "https://github.com/TestUsername/TestRepo"

        assertTrue(createJenkinsUser(name, "TestPassword"))
        assertTrue(getJenkinsUser(name).isNotEmpty())

        val j1 = "TestJob_Freestyle"
        assertTrue(createJenkinsJob(name, j1, url, true))
        assertTrue(getJenkinsJob(name, j1).isNotEmpty())
        assertTrue(existsJob(name, j1))

        val i1 = getJobInfo(name, j1)
        assertNotNull(i1)
        assertNotNull(i1?.url)

        val j2 = "TestJob2_Freestyle"
        assertTrue(createJenkinsJob(name, j2, url, true))
        assertTrue(getJenkinsJob(name, j2).isNotEmpty())
        assertTrue(existsJob(name, j2))
        assertTrue(deleteJob(name, j2))
        assertTrue(getJenkinsJob(name, j2).isEmpty())
        assertFalse(existsJob(name, j2))

        val j3 = "TestJob_Maven"
        assertTrue(createJenkinsJob(name, j3, url, false))
        assertTrue(getJenkinsJob(name, j3).isNotEmpty())

        val i3 = getJobInfo(name, j3)
        assertNotNull(i3)
        assertNotNull(i3?.url)

        val j4 = "TestJob2_Maven"
        assertTrue(createJenkinsJob(name, j4, url, false))
        assertTrue(getJenkinsJob(name, j4).isNotEmpty())
        assertTrue(deleteJob(name, j4))
        assertTrue(getJenkinsJob(name, j4).isEmpty())

        assertTrue(deleteUser(name))
        assertTrue(getJenkinsUser(name).isEmpty())
    }

    @Test
    fun testIsFreestyle() {
        val u1 = "https://github.com/CodeMC/API.git"
        assertTrue(isFreestyle(u1))

        val u2 = "https://github.com/jenkins-docs/simple-java-maven-app.git"
        assertFalse(isFreestyle(u2))

        val u3 = "https://bitbucket.org/jeyvison_andrade/gradle_tutorial.git"
        assertTrue(isFreestyle(u3))

        val u4 = "https://gitlab.com/gitlab-examples/maven/simple-maven-app.git/"
        assertFalse(isFreestyle(u4))
    }

    @Test
    fun testChangePassword() {
        val name = "OldUser788"

        val p1 = "OldPassword123"
        assertTrue(createJenkinsUser(name, p1))
        assertTrue(getJenkinsUser(name).isNotEmpty())

        val p2 = "NewPassword456"
        assertTrue(changeJenkinsPassword(name, p2))
        assertTrue(getJenkinsUser(name).isNotEmpty())

        assertTrue(deleteUser(name))
        assertTrue(getJenkinsUser(name).isEmpty())
    }

    @Test
    fun testCheckUserConfig() = runTest {
        val username = "OldUser567"
        val type = "USER"

        val noMavenConfig = (javaClass.getResourceAsStream("/user-config-no-maven.xml")?.bufferedReader()?.readText() ?: error("Resource not found: /user-config-no-maven.xml"))
            .replace("{USERNAME}", username)
            .replace("{TYPE}", type)

        assertFalse(noMavenConfig.contains("<id>nexus-login</id>"))

        val mavenConfig = (javaClass.getResourceAsStream("/templates/jenkins/user-config.xml")?.bufferedReader()?.readText() ?: error("Resource not found: /templates/jenkins/user-config.xml"))
            .replace("{USERNAME}", username)
            .replace("{TYPE}", type)

        assertTrue(mavenConfig.contains("<id>nexus-login</id>"))

        assertNotEquals(noMavenConfig, mavenConfig)

        val newMavenConfig = checkMavenSettings(noMavenConfig)
        assertTrue(newMavenConfig.contains("<id>nexus-login</id>"))
    }

}