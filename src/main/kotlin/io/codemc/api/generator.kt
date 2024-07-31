@file:JvmName("Generator")

package io.codemc.api

private val pwdChars = ('a'..'z') + ('A'..'Z') + ('0'..'9') + "!@#-_^*".toList()

fun createPassword(size: Int): String = (1..size)
    .map { pwdChars.random() }
    .joinToString("")