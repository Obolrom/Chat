package io.romix.chat.model

data class Message(
    val id: Long,
    val authorId: Long,
    val receiverId: Long?,
    val message: String?,
    val createdAt: Long,
    val isModified: Boolean,
    val modifiedAt: Long?,
)