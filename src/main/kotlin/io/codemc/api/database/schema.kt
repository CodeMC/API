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

/**
 * Represents the `requests` SQL table.
 */
object Requests : Table() {
    /**
     * The `message_id` column, representing the unique ID of the request according to Discord.
     */
    val messageId: Column<Long> = long("message_id").uniqueIndex()
    /**
     * The `thread_id` column, representing the unique ID of the thread according to Discord.
     */
    val threadId: Column<Long> = long("thread_id")
    /**
     * The `user_id` column, representing the unique ID of the Discord user.
     */
    val userId: Column<Long> = long("user_id")
    /**
     * The `github_name` column, representing the GitHub username of the user.
     */
    val githubName: Column<String> = varchar("github_name", 39)
    /**
     * The `repo_name` column, representing the name of the repository.
     */
    val repoName: Column<String> = varchar("repo_name", 39)

    override val primaryKey = PrimaryKey(messageId)
}

/**
 * Represents a request in the database.
 * @property messageId The unique ID of the request according to Discord.
 * @property threadId The unique ID of the thread according to Discord.
 * @property userId The unique ID of the Discord user.
 * @property githubName The GitHub username of the user.
 * @property repoName The name of the repository.
 */
data class Request(
    val messageId: Long,
    val threadId: Long,
    val userId: Long,
    val githubName: String,
    val repoName: String
)