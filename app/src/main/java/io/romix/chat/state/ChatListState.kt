package io.romix.chat.state

import io.romix.chat.model.CurrentUser
import io.romix.chat.model.User

data class ChatListState(
    val currentUser: CurrentUser,
    val chats: List<User>,
)
