package io.romix.chat.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.romix.chat.CurrentUserStorage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

sealed class LoginSideEffect {

    data object Success : LoginSideEffect()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val currentUserStorage: CurrentUserStorage,
) : ViewModel(), ContainerHost<Unit, LoginSideEffect> {

    override val container: Container<Unit, LoginSideEffect> = container(
        initialState = Unit,
        onCreate = {
            coroutineScope {
                currentUserStorage.tryAutoLogin()

                currentUserStorage.userFlow
                    .filterNotNull()
                    .collectLatest { postSideEffect(LoginSideEffect.Success) }
            }
        }
    )

    fun login(username: String) = intent {
        currentUserStorage.login(username)
    }
}