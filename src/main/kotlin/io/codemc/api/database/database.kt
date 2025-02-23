@file:JvmName("DatabaseAPI")

package io.codemc.api.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

private val config: DatabaseConfig = DatabaseConfig {
    defaultMaxAttempts = 5
}

/**
 * The [DBConfig] instance.
 */
var dbConfig: DBConfig = DBConfig("", "", "")

/**
 * The MariaDB configuration.
 * @property url The URl to the database.
 * @property username The username into the database.
 * @property password The password into the database.
 */
data class DBConfig(
    val url: String,
    val username: String,
    val password: String
)

/**
 * Represents the database connection.
 */
lateinit var database: Database
    private set

/**
 * Attempts to connect to the database.
 * @return The new [database] value
 */
fun connect(): Database {
    database = Database.connect(
        url = dbConfig.url,
        user = dbConfig.username,
        password = dbConfig.password,
        databaseConfig = config
    )

    transaction(database) {
        val name = database.name
        if (!SchemaUtils.listDatabases().contains(name))
            SchemaUtils.createDatabase(name)
    }

    return database
}

// Users

/**
 * Adds a user to the database.
 * @param username The username of the jenkins user
 * @param discord The Discord ID of the jenkins user
 * @return The result of the `INSERT` statement.
 */
fun addUser(
    username: String,
    discord: Long
) = transaction(database) {
    SchemaUtils.create(Users)

    Users.insert { row ->
        row[Users.username] = username
        row[Users.discord] = discord
    }
}

/**
 * Gets a [User] by its jenkins username.
 * @param username The username to lookup
 * @return The user mapped to the jenkins username, or `null` if not found
 */
fun getUser(
    username: String
): User? = transaction(database) {
    if (!Users.exists()) return@transaction null

    Users.selectAll().where { Users.username eq username }
        .map { row -> User(row[Users.username], row[Users.discord]) }
        .firstOrNull()
}

/**
 * Gets all users currently linked in the database.
 * @return All users in the database
 */
fun getAllUsers(): List<User> = transaction(database) {
    if (!Users.exists()) return@transaction emptyList()

    Users.selectAll()
        .map { row -> User(row[Users.username], row[Users.discord]) }
}

/**
 * Updates the user linked in the database.
 * @param username The jenkins user to update
 * @param discord The new Discord ID mapped to the user
 * @return `1` if updated, else `0`
 */
fun updateUser(
    username: String,
    discord: Long
) = transaction(database) {
    if (!Users.exists()) return@transaction 0

    Users.update({ Users.username eq username }) { row ->
        row[Users.discord] = discord
    }
}

/**
 * Removes a user from the database.
 * @param username The jenkins user to remove
 * @return `1` if removed, else `0`
 */
fun removeUser(
    username: String
) = transaction(database) {
    if (!Users.exists()) return@transaction 1
    Users.deleteWhere { Users.username eq username }
}

/**
 * Removes all users from the database.
 * @return The count of deleted members
 */
fun removeAllUsers() = transaction(database) {
    if (!Users.exists()) return@transaction 0
    Users.deleteAll()
}

// Requests

/**
 * Adds a request to the database.
 * @param messageId The unique ID of the request according to Discord
 * @param userId The unique ID of the Discord user
 * @param githubName The GitHub username of the user
 * @param repoName The name of the repository
 * @return The result of the `INSERT` statement
 */
fun createRequest(
    messageId: Long,
    userId: Long,
    githubName: String,
    repoName: String
) = transaction(database) {
    SchemaUtils.create(Requests)

    return@transaction Requests.insert { row ->
        row[Requests.messageId] = messageId
        row[Requests.userId] = userId
        row[Requests.githubName] = githubName
        row[Requests.repoName] = repoName
    }
}

/**
 * Gets a [Request] by its unique ID.
 * @param messageId The unique ID to lookup
 * @return The request mapped to the unique ID, or `null` if not found
 */
fun getRequest(
    messageId: Long
): Request? = transaction(database) {
    if (!Requests.exists()) return@transaction null

    return@transaction Requests.selectAll().where { Requests.messageId eq messageId }
        .map { row -> Request(row[Requests.messageId], row[Requests.userId], row[Requests.githubName], row[Requests.repoName]) }
        .firstOrNull()
}

/**
 * Checks if a request exists by its unique ID.
 * @param messageId The unique ID to lookup
 * @return `true` if the request exists, `false` otherwise
 */
fun requestExists(
    messageId: Long
): Boolean = transaction(database) {
    if (!Requests.exists()) return@transaction false

    return@transaction Requests.selectAll().where { Requests.messageId eq messageId }.count() > 0
}

/**
 * Gets all requests currently linked in the database.
 * @return All requests in the database
 */
fun getAllRequests(): List<Request> = transaction(database) {
    if (!Requests.exists()) return@transaction emptyList()

    return@transaction Requests.selectAll()
        .map { row -> Request(row[Requests.messageId], row[Requests.userId], row[Requests.githubName], row[Requests.repoName]) }
}

/**
 * Removes a request from the database.
 * @param messageId The unique ID of the request to remove
 * @return `1` if removed, else `0`
 */
fun removeRequest(
    messageId: Long
) = transaction(database) {
    if (!Requests.exists()) return@transaction 0
    return@transaction Requests.deleteWhere { Requests.messageId eq messageId }
}

/**
 * Removes all requests from the database.
 * @return The count of deleted requests
 */
fun removeAllRequests() = transaction(database) {
    if (!Requests.exists()) return@transaction 0
    return@transaction Requests.deleteAll()
}