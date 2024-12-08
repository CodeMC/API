@file:JvmName("DatabaseSchema")

package io.codemc.api.database

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

/**
 * Represents the `users` SQL table.
 */
object Users : Table() {
    /**
     * The `username` column, representing their Jenkins username.
     */
    val username: Column<String> = varchar("username", 39).uniqueIndex()

    /**
     * The `discord` column, representing their Discord ID.
     */
    val discord: Column<Long> = long("discord")

    override val primaryKey = PrimaryKey(username)
}

/**
 * Represents a user in the database.
 * @property username The username of the user.
 * @property discord The Discord ID of the user.
 */
data class User(
    val username: String,
    val discord: Long
)