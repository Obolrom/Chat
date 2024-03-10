package io.romix.chat

import io.romix.chat.model.CurrentUser
import io.romix.chat.repository.BackendRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentUserStorage @Inject constructor(
    private val backendRepository: BackendRepository,
) {
    val userFlow: MutableStateFlow<CurrentUser?> = MutableStateFlow(null)

    suspend fun login(login: String) {
        withContext(Dispatchers.IO) {
            val userResult = backendRepository.authorize(login)

            userResult.onSuccess { user ->
                userFlow.emit(user)
            }
            Timber.d("Auth response: $userResult")
        }
    }
}