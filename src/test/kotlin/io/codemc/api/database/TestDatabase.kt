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
        assertEquals(u1?.discord, 123456789L)

        removeUser(n1)

        val n2 = "OtherAuthor456"
        addUser(n2, 987654321L)

        val u2 = getUser(n2)
        assertNotNull(u2)
        assertEquals(u2?.discord, 987654321L)

        removeUser(n2)
    }

    @Test
    fun testUpdateUser() {
        val name = "MyAuthor"
        addUser(name, 0L)

        val u1 = getUser(name)
        assertNotNull(u1)
        assertEquals(u1?.discord, 0L)

        updateUser(name, 123456789L)

        val u2 = getUser(name)
        assertNotNull(u2)
        assertEquals(u2?.discord, 123456789L)

        removeUser(name)
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

        val retrieved = getAllUsers()
        assertEquals(users.size, retrieved.size)
        for (i in retrieved.indices) {
            assertEquals(retrieved[i].username, users[i])
            assertEquals(retrieved[i].discord, i.toLong())
        }

        removeAllUsers()
        assertTrue(getAllUsers().isEmpty())
    }

}