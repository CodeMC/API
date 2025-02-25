package io.codemc.api.database

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class TestDatabase {

    companion object {

        @BeforeAll
        @JvmStatic
        fun initialize() {
            dbConfig = DBConfig(
                "jdbc:mariadb://localhost:3306/test",
                "admin",
                "password"
            )

            connect()
        }

    }

    @Test
    fun testAddUser() {
        val n1 = "MyAuthor123"
        addUser(n1, 123456789L)

        val u1 = getUser(n1)
        assertNotNull(u1)
        assertEquals(123456789L, u1?.discord)

        removeUser(n1)

        val n2 = "OtherAuthor456"
        addUser(n2, 987654321L)

        val u2 = getUser(n2)
        assertNotNull(u2)
        assertEquals(987654321L, u2?.discord)

        removeUser(n2)
    }

    @Test
    fun testUpdateUser() {
        val name = "MyAuthor"
        addUser(name, 0L)

        val u1 = getUser(name)
        assertNotNull(u1)
        assertEquals(0L, u1?.discord)

        updateUser(name, 123456789L)

        val u2 = getUser(name)
        assertNotNull(u2)
        assertEquals(123456789L, u2?.discord)

        removeUser(name)
    }

    @Test
    fun testMultipleUsers() {
        val users = listOf(
            "TestMultiUser",
            "TestMultiUser2",
        )

        users.forEach { s -> addUser(s, 4567L) }

        assertEquals(4567L, getUser(users[0])?.discord)
        assertEquals(4567L, getUser(users[1])?.discord)

        removeUser(users[0])
        removeUser(users[1])
    }

    @Test
    fun testRemoveUser() {
        val name = "MyAuthor"
        addUser(name, 0L)
        assertNotNull(getUser(name))

        removeUser(name)
        assertNull(getUser(name))
    }

    @Test
    fun testAllUsers() {
        val users = listOf(
            "TestUser",
            "TestUser2",
            "MyAuthor3",
            "CoolDude75_"
        )

        users.forEachIndexed { index, s -> addUser(s, index.toLong()) }

        for ((i, user) in users.withIndex()) {
            assertNotNull(getUser(user))
            assertEquals(i.toLong(), getUser(user)?.discord)
        }

        users.forEach { removeUser(it) }
    }

    @Test
    fun testCreateRequest() {
        val messageId = 123456789L
        val userId = 987654321L
        val githubName = "MyAuthor"
        val repoName = "MyRepo"

        createRequest(messageId, 0L, userId, githubName, repoName)

        val r = getRequest(messageId)
        assertNotNull(r)
        assertEquals(userId, r?.userId)
        assertEquals(githubName, r?.githubName)
        assertEquals(repoName, r?.repoName)

        removeRequest(messageId)
    }

    @Test
    fun testRemoveRequest() {
        val messageId = 123456789L
        val userId = 987654321L
        val githubName = "MyAuthor"
        val repoName = "MyRepo"

        createRequest(messageId, 0L, userId, githubName, repoName)
        assertNotNull(getRequest(messageId))

        removeRequest(messageId)
        assertNull(getRequest(messageId))
    }

    @Test
    fun testRemoveAllRequests() {
        val requests = listOf(
            123456789L,
            987654321L,
            123L,
            456L
        )

        requests.forEach { createRequest(it, 0L,0L, "Test", "Test") }
        requests.forEach { assertNotNull(getRequest(it)) }

        removeAllRequests()

        requests.forEach { assertNull(getRequest(it)) }
    }

    @Test
    fun testMultipleRequests() {
        val requests = listOf(
            123456789L,
            987654321L,
            123L,
            456L
        )

        requests.forEachIndexed { index, l -> createRequest(l, 0L, index.toLong(), "Test", "Test") }
        requests.forEachIndexed { index, l ->
            val r = getRequest(l)
            assertNotNull(r)
            assertEquals(index.toLong(), r?.userId)
            assertEquals(0L, r?.threadId)
            assertEquals("Test", r?.githubName)
            assertEquals("Test", r?.repoName)
        }

        requests.forEach { removeRequest(it) }
    }

}