package io.romix.chat.network

data class ChatMessage(
    val message: String,
    val receiverId: Long,
)