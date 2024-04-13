package io.romix.chat.state

import io.romix.chat.model.Chat
import io.romix.chat.model.CurrentUser
import io.romix.chat.model.User

data class ChatListState(
    val currentUser: CurrentUser,

    @Deprecated(message = "Will not be used anymore")
    val chats: List<User>,

    val chatItems: List<ChatItem>,
)

sealed class ChatItem {

    data class DirectItem(val user: User) : ChatItem()

    data class GroupItem(val chat: Chat) : ChatItem()
}
