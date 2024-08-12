@file:JvmName("DatabaseAPI")

package io.codemc.api.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

private val config: DatabaseConfig = DatabaseConfig {
    defaultMaxAttempts = 5
}

var dbConfig: DBConfig = DBConfig("", "", "")

data class DBConfig(
    val url: String,
    val username: String,
    val password: String
)

lateinit var database: Database
    private set

fun connect(): Database {
    database = Database.connect(
        url = dbConfig.url,
        user = dbConfig.username,
        password = dbConfig.password,
        databaseConfig = config
    )

    return database
}

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

fun getUser(
    username: String
): User? = transaction(database) {
    Users.selectAll().where { Users.username eq username }
        .map { row -> User(row[Users.username], row[Users.discord]) }
        .firstOrNull()
}

fun getAllUsers(): List<User> = transaction(database) {
    Users.selectAll()
        .map { row -> User(row[Users.username], row[Users.discord]) }
}

fun updateUser(
    username: String,
    discord: Long
) = transaction(database) {
    Users.update({ Users.username eq username }) { row ->
        row[Users.discord] = discord
    }
}

fun removeUser(
    username: String
) = transaction(database) {
    Users.deleteWhere { Users.username eq username }
}

fun removeAllUsers() = transaction(database) {
    Users.deleteAll()
}