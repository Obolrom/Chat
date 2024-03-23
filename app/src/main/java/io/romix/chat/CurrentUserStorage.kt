package io.romix.chat

import io.romix.chat.model.CurrentUser
import io.romix.chat.repository.AuthRepository
import io.romix.chat.repository.BackendRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentUserStorage @Inject constructor(
    private val authRepository: AuthRepository,
) {
    val userFlow: MutableStateFlow<CurrentUser?> = MutableStateFlow(null)

    suspend fun login(login: String) {
        withContext(Dispatchers.IO) {
            val userResult = authRepository.authorize(login)

            userResult.onSuccess { user ->
                userFlow.emit(user)
            }
            Timber.d("Auth response: $userResult")
        }
    }

    suspend fun logout() {
        authRepository.logout()
        userFlow.emit(null)
    }

    suspend fun tryAutoLogin() {
        withContext(Dispatchers.IO) {
            val userResult = authRepository.tryAutoLogin()

            userResult?.onSuccess { user ->
                userFlow.emit(user)
            }
            Timber.d("Auth response: $userResult")
        }
    }
}