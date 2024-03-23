package io.romix.chat.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.romix.chat.CurrentUserStorage
import io.romix.chat.model.CurrentUser
import io.romix.chat.model.Message
import io.romix.chat.model.User
import io.romix.chat.network.ChatMessage
import io.romix.chat.repository.BackendRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlow
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.syntax.simple.repeatOnSubscription
import org.orbitmvi.orbit.viewmodel.container
import timber.log.Timber
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.StompHeader
import javax.inject.Inject

data class ChatState(
    val currentUser: CurrentUser,
    val collocutorUser: User?,
    val messages: List<Message>,
)

sealed class ChatSideEffect {

    data object Logout : ChatSideEffect()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val currentUserStorage: CurrentUserStorage,
    private val backendRepository: BackendRepository,
    private val chatClient: StompClient,
) : ViewModel(), ContainerHost<ChatState, ChatSideEffect> {

    private val destinationUserId: Long = checkNotNull(savedStateHandle["userId"])
    private val currentUser: CurrentUser = requireNotNull(currentUserStorage.userFlow.value) {
        "Current user should be presented"
    }

    override val container: Container<ChatState, ChatSideEffect> = container(
        initialState = ChatState(
            currentUser = currentUser,
            collocutorUser = null,
            messages = emptyList(),
        ),
        onCreate = { connectToChat() }
    )

    private val gson = Gson()

    fun sendMessage(message: String) = intent {
        val chatMessage = ChatMessage(message, destinationUserId)

        chatClient.send("/chat/direct", gson.toJson(chatMessage))
            .toObservable<Unit>()
            .asFlow()
            .catch { error -> Timber.tag(ChatViewModel::class.java.simpleName).e(error) }
            .collectLatest {
                Timber.tag(ChatViewModel::class.java.simpleName).d("sent message")
            }
    }

    fun logout() = intent {
        currentUserStorage.logout()

        postSideEffect(ChatSideEffect.Logout)
    }

    private fun connectToChat() = intent {
        chatClient.connect(listOf(StompHeader("Authorization", "Bearer ${currentUser.token}")))

        coroutineScope {
            repeatOnSubscription {
                launch {
                    chatClient.topic("/user/queue/chat")
                        .map { gson.fromJson(it.payload, Message::class.java) }
                        .asFlow()
                        .catch { error ->
                            Timber.tag(ChatViewModel::class.java.simpleName).e(error)
                        }
                        .onStart {
                            Timber.tag(ChatViewModel::class.java.simpleName).d("topic onStart")
                        }
                        .onCompletion {
                            Timber.tag(ChatViewModel::class.java.simpleName)
                                .d("topic onComplete")
                        }
                        .collectLatest { newMessage ->
                            reduce { state.copy(messages = listOf(newMessage) + state.messages) }
                        }
                }
                launch {
                    backendRepository.getLastMessagesForCollocutor(
                        currentUser = currentUser,
                        collocutorId = destinationUserId
                    ).onSuccess { lastDirectMessages ->
                        reduce { state.copy(messages = lastDirectMessages) }
                    }

                    Timber.tag(ChatViewModel::class.java.simpleName).d("Connected")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatClient.disconnect()
    }
}