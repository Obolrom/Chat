package io.romix.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import io.romix.chat.model.CurrentUser
import io.romix.chat.state.ChatListState
import io.romix.chat.ui.theme.ChatTheme
import io.romix.chat.viewmodel.ChatListViewModel
import io.romix.chat.viewmodel.ChatState
import io.romix.chat.viewmodel.ChatViewModel
import io.romix.chat.viewmodel.LoginSideEffect
import io.romix.chat.viewmodel.LoginViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatAppNavHost()
        }
    }
}

@Composable
fun ChatAppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = "login",
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(route = "login") {
            val loginViewModel = hiltViewModel<LoginViewModel>()
            loginViewModel.collectSideEffect {
                when (it) {
                    LoginSideEffect.Success -> {
                        navController.navigate(route = "chatList")
                    }
                }
            }

            Login(onLoginClick = loginViewModel::login)
        }

        composable(route = "chatList") {
            val chatListViewModel = hiltViewModel<ChatListViewModel>()
            val chatListState: ChatListState = chatListViewModel.collectAsState().value

            ChatList(
                chatListState = chatListState,
                onNavigateToChat = { userId -> navController.navigate(route = "chat/$userId") }
            )
        }

        composable(
            route = "chat/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.LongType })
        ) {
            val chatViewModel = hiltViewModel<ChatViewModel>()
            val chatState: ChatState = chatViewModel.collectAsState().value

            DirectChat(
                chatState = chatState,
                onSendMessage = chatViewModel::sendMessage,
            )
        }
    }
}

@Composable
fun Login(
    onLoginClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChatTheme {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val loginInput = remember { mutableStateOf("romix") }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        value = loginInput.value,
                        onValueChange = { input -> loginInput.value = input },
                        label = { Text("Login") },
                    )

                    Button(onClick = { onLoginClick(loginInput.value) }) {
                        Text(text = "Login")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatList(
    chatListState: ChatListState,
    onNavigateToChat: (Long) -> Unit,
) {
    ChatTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column {
                CurrentAuthenticatedUser(currentUser = chatListState.currentUser)

                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = "Available chats",
                    )

                    LazyColumn {
                        items(
                            items = chatListState.chats,
                            key = { it.id }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillParentMaxWidth()
                                    .height(40.dp)
                                    .background(Color.LightGray, RoundedCornerShape(8.dp))
                                    .clickable { onNavigateToChat(it.id) }
                                    .padding(8.dp)
                            ) {
                                Text(text = it.username)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DirectChat(
    chatState: ChatState,
    onSendMessage: (String) -> Unit
) {
    ChatTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val chatInput = remember { mutableStateOf("") }

            Column {
                CurrentAuthenticatedUser(currentUser = chatState.currentUser)

                Column {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        reverseLayout = true,
                        state = rememberLazyListState(),
                    ) {
                        items(
                            items = chatState.messages,
                            key = { it.id },
                        ) { message ->
                            Text(text = "[author: ${message.authorId}] message: ${message.message}")
                        }
                    }

                    val keyboardController = LocalSoftwareKeyboardController.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = chatInput.value,
                            onValueChange = { input -> chatInput.value = input },
                            label = { Text("Enter a message") },
                            keyboardActions = KeyboardActions(onDone = {
                                keyboardController?.hide()
                            })
                        )
                        Button(onClick = {
                            onSendMessage(chatInput.value)
                            chatInput.value = ""
                        }) {
                            Text(text = "Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentAuthenticatedUser(
    currentUser: CurrentUser,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.LightGray)
            .padding(8.dp),
        text = "current user: ${currentUser.username}",
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ChatTheme {
        Greeting("Android")
    }
}