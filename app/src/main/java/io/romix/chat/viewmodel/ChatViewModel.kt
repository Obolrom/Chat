package io.romix.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.romix.chat.model.Message
import io.romix.chat.model.User
import io.romix.chat.network.ChatMessage
import io.romix.chat.repository.BackendRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.StompHeader
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val backendRepository: BackendRepository,
    private val chatClient: StompClient,
) : ViewModel() {

    private val disposeBag = CompositeDisposable()
    private val topicDisposables = CompositeDisposable()
    private val gson = Gson()

    private val userFlow: MutableStateFlow<User?> = MutableStateFlow(null)

    private val messagesInternal = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = messagesInternal.asStateFlow()

    fun connectToChat() {
        val userToConnect = userFlow.value ?: return

        chatClient.connect(listOf(StompHeader("Authorization", "Bearer ${userToConnect.token}")))

        topicDisposables.clear()

        chatClient.topic("/user/${userToConnect.userId}/queue/chat")
            .subscribeOn(Schedulers.io())
            .map { gson.fromJson(it.payload, Message::class.java) }
            .subscribe(
                {
                    Timber.tag(ChatViewModel::class.java.simpleName).d("direct $it")
                    messagesInternal.tryEmit(listOf(it) + messagesInternal.value)
                },
                { error -> Timber.tag(ChatViewModel::class.java.simpleName).e(error) },
                { Timber.tag(ChatViewModel::class.java.simpleName).d("topic complete") }
            )
            .also(topicDisposables::add)

        viewModelScope.launch(Dispatchers.IO) {
            backendRepository.getLastMessagesForCollocutor(
                currentUser = userToConnect,
                collocutorId = RECEIVER_USER_ID
            ).onSuccess { lastDirectMessages ->
                messagesInternal.emit(lastDirectMessages)
            }
        }

        Timber.tag(ChatViewModel::class.java.simpleName).d("Connected")
    }

    fun login(login: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val userResult = backendRepository.authorize(login)

            userResult.onSuccess { user ->
                userFlow.emit(user)
            }
            Timber.d("Auth response: $userResult")
        }
    }

    fun sendMessage(message: String) {
        val chatMessage = ChatMessage(message, RECEIVER_USER_ID)

        chatClient.send("/chat/direct", gson.toJson(chatMessage))
            .subscribe(
                {
                    Timber.tag(ChatViewModel::class.java.simpleName).d("sent message")
                },
                { error -> Timber.tag(ChatViewModel::class.java.simpleName).e(error) }
            )
            .also(disposeBag::add)
    }

    override fun onCleared() {
        super.onCleared()
        chatClient.disconnect()
        disposeBag.dispose()
        topicDisposables.dispose()
    }

    companion object {
        private const val RECEIVER_USER_ID = 4L
    }
}