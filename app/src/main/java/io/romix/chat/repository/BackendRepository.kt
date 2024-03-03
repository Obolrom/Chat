package io.romix.chat.repository

import com.haroldadmin.cnradapter.NetworkResponse
import dagger.Reusable
import io.romix.chat.model.Message
import io.romix.chat.model.User
import io.romix.chat.network.AuthRequest
import io.romix.chat.network.AuthResponse
import io.romix.chat.network.BackendService
import javax.inject.Inject

@Reusable
class BackendRepository @Inject constructor(
    private val api: BackendService,
) {

    suspend fun authorize(login: String): Result<User> {
        return when (val response = api.authorize(AuthRequest(login, login))) {
            is NetworkResponse.Success -> {
                Result.success(authRequestToAuth(response.body, login))
            }
            is NetworkResponse.Error -> {
                val throwable = response.error
                    ?: IllegalStateException("Unknown error, body: ${response.body}")
                Result.failure(throwable)
            }
        }
    }

    suspend fun getLastMessagesForCollocutor(
        currentUser: User,
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

    private fun authRequestToAuth(authResponse: AuthResponse, login: String): User {
        return User(
            userId = requireNotNull(authResponse.userId),
            token = requireNotNull(authResponse.token),
            username = login,
        )
    }
}