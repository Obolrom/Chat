package io.romix.chat.network.response

data class PageMessageResponse(
    val messages: List<MessageResponse>,
    val total: Long,
    val totalPages: Int,
) {

    data class MessageResponse(
        val id: Long,
        val authorId: Long,
        val receiverId: Long,
        val message: String?,
        val createdAt: Long,
        val isModified: Boolean,
        val modifiedAt: Long?,
    )
}
