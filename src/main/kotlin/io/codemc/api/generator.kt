@file:JvmName("Generator")

package io.codemc.api

private val pwdChars = ('a'..'z') + ('A'..'Z') + ('0'..'9') + "!@#-_^*".toList()

/**
 * Creates a random password of the given size.
 * @param size The size of the password.
 * @return The generated password.
 */
fun createPassword(size: Int): String = (1..size)
    .map { pwdChars.random() }
    .joinToString("")