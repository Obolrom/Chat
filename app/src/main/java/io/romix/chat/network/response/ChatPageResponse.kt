package io.romix.chat.network.response

data class ChatPageResponse(
    val chats: List<ChatResponse>,
    val total: Long,
    val totalPages: Int,
) {

    data class ChatResponse(
        val id: Long,
        val title: String,
    )
}
