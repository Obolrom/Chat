package io.romix.chat.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.romix.chat.CurrentUserStorage
import io.romix.chat.model.CurrentUser
import io.romix.chat.model.Message
import io.romix.chat.model.TypingResponse
import io.romix.chat.model.User
import io.romix.chat.network.ChatMessage
import io.romix.chat.network.RoomChatMessage
import io.romix.chat.network.TypingMessage
import io.romix.chat.repository.BackendRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
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
    val isTyping: Boolean = false,
)

sealed class ChatSideEffect {

    data object Logout : ChatSideEffect()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val currentUserStorage: CurrentUserStorage,
    private val backendRepository: BackendRepository,
    private val chatClient: StompClient,
) : ViewModel(), ContainerHost<ChatState, ChatSideEffect> {

    private val destinationId: Long = checkNotNull(savedStateHandle["userId"])
    private val destinationType: String = checkNotNull(savedStateHandle["type"])
    private val currentUser: CurrentUser = requireNotNull(currentUserStorage.userFlow.value) {
        "Current user should be presented"
    }

    override val container: Container<ChatState, ChatSideEffect> = container(
        initialState = ChatState(
            currentUser = currentUser,
            collocutorUser = null,
            messages = emptyList(),
        ),
        onCreate = {
            if (destinationType == "direct") {
                backendRepository.getUserById(currentUser, destinationId)
                    .onSuccess { collocutorUser ->
                        reduce {
                            state.copy(collocutorUser = collocutorUser)
                        }
                    }
            }
            connectToChat()
            typing()
        }
    )

    private val gson = Gson()

    private val typingFlow = MutableStateFlow("")

    fun sendMessage(message: String) = intent {
        if (destinationType == "group") {
            sendMessageToRoom(message)
            return@intent
        }

        val chatMessage = ChatMessage(message, destinationId)

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

    fun onTyping(text: String) = intent {
        typingFlow.emit(text)
    }

    private suspend fun sendMessageToRoom(message: String) {
        val chatMessage = RoomChatMessage(message)

        chatClient.send("/chat/room/$destinationId", gson.toJson(chatMessage))
            .toObservable<Unit>()
            .asFlow()
            .catch { error -> Timber.tag(ChatViewModel::class.java.simpleName).e(error) }
            .collectLatest {
                Timber.tag(ChatViewModel::class.java.simpleName).d("sent message")
            }
    }

    private fun connectToChat() = intent {
        chatClient.connect(listOf(StompHeader("Authorization", "Bearer ${currentUser.token}")))

        coroutineScope {
            repeatOnSubscription {
                launch {
                    val topic =
                        if (destinationType == "group") chatClient.topic("/topic/room.$destinationId")
                        else chatClient.topic("/user/queue/chat")

                    topic
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
                    chatClient.topic("/user/queue/typing")
                        .map { gson.fromJson(it.payload, TypingResponse::class.java) }
                        .asFlow()
                        .map { it.isTyping }
                        .catch { error ->
                            Timber.tag(ChatViewModel::class.java.simpleName).e(error)
                        }
                        .collectLatest { isTyping ->
                            reduce { state.copy(isTyping = isTyping) }
                        }
                }
                launch {
                    if (destinationType == "group") {
                        backendRepository.getLastRoomMessages(
                            currentUser = currentUser,
                            roomId = destinationId,
                        ).onSuccess { lastDirectMessages ->
                            reduce { state.copy(messages = lastDirectMessages) }
                        }
                    } else {
                        backendRepository.getLastMessagesForCollocutor(
                            currentUser = currentUser,
                            collocutorId = destinationId,
                        ).onSuccess { lastDirectMessages ->
                            reduce { state.copy(messages = lastDirectMessages) }
                        }
                    }

                    Timber.tag(ChatViewModel::class.java.simpleName).d("Connected")
                }
            }
        }
    }

    // distinctUntilChanged() commented because there is a case when user started typing a message and receiver enters a screen and no "is typing..." message
    private fun typing() = intent {
        repeatOnSubscription {
            typingFlow
                .map { it.trim() }
                .map { it.isNotEmpty() }
//                .distinctUntilChanged()
                .onEach { isTyping ->
                    Timber.tag("isTyping").d(if (isTyping) "Typing" else "stopped typing")
                }
                .map { isTyping -> TypingMessage(isTyping = isTyping, receiverId = destinationId) }
                .flatMapLatest { typingMessage ->
                    typingSend(typingMessage)
                }
                .collect()
        }
    }

    private fun typingSend(typingMessage: TypingMessage): Flow<Unit> {
        return chatClient.send("/chat/direct/typing", gson.toJson(typingMessage))
            .toObservable<Unit>()
            .asFlow()
            .catch { error ->
                Timber.tag(ChatViewModel::class.java.simpleName).e(error)
            }
    }

    override fun onCleared() {
        super.onCleared()
        chatClient.disconnect()
    }
}