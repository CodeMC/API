package io.codemc.api.jenkins

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestJenkinsData {

    @Test
    fun testJenkinsJob() {
        val j1 = JenkinsJob(
            "MyJob",
            "http://localhost:8080/job/MyJob",
            "My Description",
            null,
            null,
            null,
            null
        )

        assertNotNull(j1.url)
        assertNotNull(j1.description)
        assertNull(j1.lastBuild)

        val j2 = JenkinsJob(
            "BuiltJob",
            "http://localhost:8080/job/BuiltJob",
            "Built Description",
            JenkinsBuild("SUCCESS", 2, "http://localhost:8080/job/BuiltJob/2", 0),
            JenkinsBuild("SUCCESS", 2, "http://localhost:8080/job/BuiltJob/2", 0),
            JenkinsBuild("FAILURE", 1, "http://localhost:8080/job/BuiltJob/1", 0),
            JenkinsBuild("SUCCESS", 2, "http://localhost:8080/job/BuiltJob/2", 0)
        )

        assertNotNull(j2.url)
        assertNotNull(j2.description)
        assertNotNull(j2.lastBuild)
        assertEquals(j2.lastBuild, j2.lastCompletedBuild)
        assertEquals(j2.lastBuild, j2.lastStableBuild)
        assertNotEquals(j2.lastBuild, j2.lastFailedBuild)
    }

    @Test
    fun testJenkinsBuild() {
        val b1 = JenkinsBuild("SUCCESS", 1, "http://localhost:8080/job/MyJob/1", 0)
        assertEquals("SUCCESS", b1.result)
        assertEquals(1, b1.number)
        assertNotNull(b1.url)
        assertEquals(0, b1.timestamp)
        assertEquals(b1.toString(), "SUCCESS\n[Build #1](http://localhost:8080/job/MyJob/1) - Sometime\n")

        val b2 = JenkinsBuild("FAILURE", 2, "http://localhost:8080/job/MyJob/2", 10)
        assertEquals("FAILURE", b2.result)
        assertEquals(2, b2.number)
        assertNotNull(b2.url)
        assertEquals(10, b2.timestamp)
        assertEquals(b2.toString(), "FAILURE\n[Build #2](http://localhost:8080/job/MyJob/2) - <t:10:f>\n")

        val b3 = JenkinsBuild("SUCCESS", 0, "http://localhost:8080/job/MyJob/3", 100)
        assertEquals("SUCCESS", b3.result)
        assertEquals(0, b3.number)
        assertNotNull(b3.url)
        assertEquals(100, b3.timestamp)
        assertEquals(b3.toString(), "SUCCESS\n[Unknown Build](http://localhost:8080/job/MyJob/3) - <t:100:f>\n")

        val b4 = JenkinsBuild("SUCCESS", 4, null, 1000)
        assertEquals("SUCCESS", b4.result)
        assertEquals(4, b4.number)
        assertNull(b4.url)
        assertEquals(1000, b4.timestamp)
        assertEquals(b4.toString(), "SUCCESS\nBuild #4 - <t:1000:f>\n")

        val b5 = JenkinsBuild("ABORTED", 0, null, 10000)
        assertEquals("ABORTED", b5.result)
        assertEquals(0, b5.number)
        assertNull(b5.url)
        assertEquals(10000, b5.timestamp)
        assertEquals(b5.toString(), "ABORTED\nUnknown Build - <t:10000:f>\n")

        val b6 = JenkinsBuild("UNKNOWN", 0, null, 0)
        assertEquals("UNKNOWN", b6.result)
        assertEquals(0, b6.number)
        assertNull(b6.url)
        assertEquals(0, b6.timestamp)
        assertEquals(b6.toString(), "UNKNOWN\nUnknown Build - Sometime\n")
    }

}