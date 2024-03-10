package io.romix.chat.model

data class User(
    val id: Long,
    val username: String,
    val avatarUrl: String?,
)