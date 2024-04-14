package io.romix.chat.network

data class TypingMessage(
    val isTyping: Boolean,
    val receiverId: Long,
)
