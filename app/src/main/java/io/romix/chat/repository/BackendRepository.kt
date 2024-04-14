package io.romix.chat.repository

import com.haroldadmin.cnradapter.NetworkResponse
import dagger.Reusable
import io.romix.chat.model.Chat
import io.romix.chat.model.CurrentUser
import io.romix.chat.model.Message
import io.romix.chat.model.User
import io.romix.chat.network.BackendService
import javax.inject.Inject

@Reusable
class BackendRepository @Inject constructor(
    private val api: BackendService,
) {

    suspend fun getLastMessagesForCollocutor(
        currentUser: CurrentUser,
        collocutorId: Long,
    ): Result<List<Message>> {
        val response = api.getMessages(
            bearerToken = "Bearer ${currentUser.token}",
            collocutorId = collocutorId,
            pageSize = 50,
        )
        return when (response) {
            is NetworkResponse.Success -> {
                Result.success(response.body.messages.map { messageResponse ->
                    Message(
                        id = messageResponse.id,
                        authorId = messageResponse.authorId,
                        receiverId = messageResponse.receiverId,
                        message = messageResponse.message,
                        createdAt = messageResponse.createdAt,
                        isModified = messageResponse.isModified,
                        modifiedAt = messageResponse.modifiedAt,
                    )
                })
            }
            is NetworkResponse.Error -> {
                val throwable = response.error
                    ?: IllegalStateException("Unknown error, body: ${response.body}")
                Result.failure(throwable)
            }
        }
    }

    suspend fun getLastRoomMessages(
        currentUser: CurrentUser,
        roomId: Long,
    ): Result<List<Message>> {
        val response = api.getRoomMessages(
            bearerToken = "Bearer ${currentUser.token}",
            roomId = roomId,
            pageSize = 50,
        )
        return when (response) {
            is NetworkResponse.Success -> {
                Result.success(response.body.messages.map { messageResponse ->
                    Message(
                        id = messageResponse.id,
                        authorId = messageResponse.authorId,
                        receiverId = messageResponse.receiverId,
                        message = messageResponse.message,
                        createdAt = messageResponse.createdAt,
                        isModified = messageResponse.isModified,
                        modifiedAt = messageResponse.modifiedAt,
                    )
                })
            }
            is NetworkResponse.Error -> {
                val throwable = response.error
                    ?: IllegalStateException("Unknown error, body: ${response.body}")
                Result.failure(throwable)
            }
        }
    }

    suspend fun getUsers(currentUser: CurrentUser): Result<List<User>> {
        return when (val response = api.getUsers("Bearer ${currentUser.token}")) {
            is NetworkResponse.Success -> {
                Result.success(response.body.users.map {
                    User(
                        id = it.id,
                        username = it.username,
                        avatarUrl = it.photoUrl,
                    )
                })
            }
            is NetworkResponse.Error -> {
                val throwable = response.error
                    ?: IllegalStateException("Unknown error, body: ${response.body}")
                Result.failure(throwable)
            }
        }
    }

    suspend fun getUserById(currentUser: CurrentUser, collocutorId: Long): Result<User> {
        return when (val response = api.getUserById("Bearer ${currentUser.token}", collocutorId)) {
            is NetworkResponse.Success -> {
                Result.success(User(
                    id = response.body.id,
                    username = response.body.username,
                    avatarUrl = response.body.photoUrl,
                ))
            }
            is NetworkResponse.Error -> {
                val throwable = response.error
                    ?: IllegalStateException("Unknown error, body: ${response.body}")
                Result.failure(throwable)
            }
        }
    }

    suspend fun getChats(currentUser: CurrentUser): Result<List<Chat>> {
        return when (val response = api.getChats("Bearer ${currentUser.token}")) {
            is NetworkResponse.Success -> {
                Result.success(response.body.chats.map {
                    Chat(
                        id = it.id,
                        title = it.title,
                    )
                })
            }
            is NetworkResponse.Error -> {
                val throwable = response.error
                    ?: IllegalStateException("Unknown error, body: ${response.body}")
                Result.failure(throwable)
            }
        }
    }
}