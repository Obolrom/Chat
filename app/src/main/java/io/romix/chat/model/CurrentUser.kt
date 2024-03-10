package io.romix.chat.model

data class CurrentUser(
    val userId: Long,
    val token: String,
    val username: String,
)