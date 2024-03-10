package io.romix.chat.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.romix.chat.CurrentUserStorage
import io.romix.chat.model.CurrentUser
import io.romix.chat.repository.BackendRepository
import io.romix.chat.state.ChatListState
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

sealed class ChatListSideEffect {

    data class LoadChatsError(val error: Throwable) : ChatListSideEffect()
}

@HiltViewModel
class ChatListViewModel @Inject constructor(
    currentUserStorage: CurrentUserStorage,
    private val backendRepository: BackendRepository,
) : ViewModel(), ContainerHost<ChatListState, ChatListSideEffect> {

    private val currentUser: CurrentUser = requireNotNull(currentUserStorage.userFlow.value) {
        "Current user should be presented"
    }

    override val container: Container<ChatListState, ChatListSideEffect> = container(
        initialState = ChatListState(
            currentUser = currentUser,
            chats = emptyList(),
        ),
        onCreate = {
            backendRepository.getUsers(currentUser)
                .onSuccess { chats -> reduce { state.copy(chats = chats) } }
                .onFailure { postSideEffect(ChatListSideEffect.LoadChatsError(it)) }
        }
    )
}