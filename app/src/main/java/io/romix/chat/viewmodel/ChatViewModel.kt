package io.romix.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.romix.chat.model.User
import io.romix.chat.network.ChatMessage
import io.romix.chat.network.NetworkModule.Companion.CLUSTER_IP_ADDRESS
import io.romix.chat.repository.BackendRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.dto.StompHeader
import ua.naiksoftware.stomp.dto.StompMessage
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val backendRepository: BackendRepository,
) : ViewModel() {

    private val disposeBag = CompositeDisposable()
    private val topicDisposables = CompositeDisposable()
    private val gson = Gson()

    private val chatClient by lazy {
        Stomp.over(Stomp.ConnectionProvider.OKHTTP, WEB_SOCKET_ENDPOINT)
    }

    private val userFlow: MutableStateFlow<User?> = MutableStateFlow(null)

    private val messagesInternal = MutableStateFlow<List<StompMessage>>(emptyList())
    val messages: StateFlow<List<StompMessage>> = messagesInternal.asStateFlow()

    fun connectToChat() {
        val userToConnect = userFlow.value ?: return

        chatClient.connect(listOf(StompHeader("Authorization", "Bearer ${userToConnect.token}")))

        topicDisposables.clear()

        chatClient.topic("/user/${userToConnect.userId}/queue/chat")
            .subscribeOn(Schedulers.io())
            .subscribe(
                {
                    Timber.tag(ChatViewModel::class.java.simpleName).d("direct ${it.payload}")
                    messagesInternal.tryEmit(messagesInternal.value + it)
                },
                { error -> Timber.tag(ChatViewModel::class.java.simpleName).e(error) },
                { Timber.tag(ChatViewModel::class.java.simpleName).d("topic complete") }
            )
            .also(topicDisposables::add)

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
        val chatMessage = ChatMessage(message, 1, userFlow.value?.userId ?: 0L)

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
        private const val WEB_SOCKET_ENDPOINT = "ws://$CLUSTER_IP_ADDRESS:8080/gs-guide-websocket"
    }
}