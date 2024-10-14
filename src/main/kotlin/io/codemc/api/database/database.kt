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