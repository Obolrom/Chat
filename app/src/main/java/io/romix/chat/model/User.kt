package io.romix.chat.model

data class User(
    val userId: Long,
    val token: String,
    val username: String,
)