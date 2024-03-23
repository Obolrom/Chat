package io.romix.chat.repository

import android.content.Context
import androidx.core.content.edit
import com.haroldadmin.cnradapter.NetworkResponse
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import io.romix.chat.model.CurrentUser
import io.romix.chat.network.AuthRequest
import io.romix.chat.network.AuthResponse
import io.romix.chat.network.BackendService
import javax.inject.Inject

@Reusable
class AuthRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val api: BackendService,
) {

    suspend fun authorize(login: String): Result<CurrentUser> {
        saveLogin(login)

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

    suspend fun tryAutoLogin() = findLogin()?.let { login -> authorize(login) }

    fun logout() {
        appContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit(commit = true) { remove(LOGIN_PREF_KEY) }
    }

    private fun saveLogin(login: String) {
        appContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit(commit = true) { putString(LOGIN_PREF_KEY, login) }
    }

    private fun authRequestToAuth(authResponse: AuthResponse, login: String): CurrentUser {
        return CurrentUser(
            userId = requireNotNull(authResponse.userId),
            token = requireNotNull(authResponse.token),
            username = login,
        )
    }

    private fun findLogin(): String? {
        return appContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(LOGIN_PREF_KEY, null)
    }

    companion object {
        private const val PREFERENCES_NAME = "chat_preferences"
        private const val LOGIN_PREF_KEY = "login_pref_key"
    }
}