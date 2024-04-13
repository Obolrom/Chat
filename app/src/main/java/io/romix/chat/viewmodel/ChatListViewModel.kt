package io.romix.chat.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.romix.chat.CurrentUserStorage
import io.romix.chat.model.CurrentUser
import io.romix.chat.repository.BackendRepository
import io.romix.chat.state.ChatItem
import io.romix.chat.state.ChatListState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

sealed class ChatListSideEffect {

    data class LoadChatsError(val error: Throwable) : ChatListSideEffect()

    data object Logout : ChatListSideEffect()
}

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val currentUserStorage: CurrentUserStorage,
    private val backendRepository: BackendRepository,
) : ViewModel(), ContainerHost<ChatListState, ChatListSideEffect> {

    private val currentUser: CurrentUser = requireNotNull(currentUserStorage.userFlow.value) {
        "Current user should be presented"
    }

    override val container: Container<ChatListState, ChatListSideEffect> = container(
        initialState = ChatListState(
            currentUser = currentUser,
            chats = emptyList(),
            chatItems = emptyList(),
        ),
        onCreate = {
            coroutineScope {
                val directChats = async { backendRepository.getUsers(currentUser) }
                val groupChats = async { backendRepository.getChats(currentUser) }

                val directChatsResult = directChats.await()
                    .map { users -> users.map { ChatItem.DirectItem(it) } }
                val groupChatsResult = groupChats.await()
                    .map { chats -> chats.map { ChatItem.GroupItem(it) } }

                if (directChatsResult.isSuccess && groupChatsResult.isSuccess) {
                    reduce {
                        state.copy(
                            chatItems = directChatsResult.getOrThrow() + groupChatsResult.getOrThrow(),
                        )
                    }
                } else {
                    when {
                        directChatsResult.isFailure && groupChatsResult.isFailure -> {
                            postSideEffect(ChatListSideEffect.LoadChatsError(Exception("Direct chats and group chats failed to load")))
                        }
                        directChatsResult.isFailure -> {
                            postSideEffect(ChatListSideEffect.LoadChatsError(directChatsResult.exceptionOrNull() ?: Exception()))
                        }
                        groupChatsResult.isFailure -> {
                            postSideEffect(ChatListSideEffect.LoadChatsError(groupChatsResult.exceptionOrNull() ?: Exception()))
                        }
                    }
                }
            }
        }
    )

    fun logout() = intent {
        currentUserStorage.logout()

        postSideEffect(ChatListSideEffect.Logout)
    }
}