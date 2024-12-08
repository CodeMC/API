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
    fun testAll() {
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

}