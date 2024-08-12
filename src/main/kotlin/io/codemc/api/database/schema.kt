@file:JvmName("DatabaseSchema")

package io.codemc.api.database

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val username: Column<String> = varchar("username", 39).uniqueIndex()
    val discord: Column<Long> = long("discord").uniqueIndex()

    override val primaryKey = PrimaryKey(username)
}

data class User(
    val username: String,
    val discord: Long
)