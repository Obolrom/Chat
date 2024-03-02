package io.romix.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.romix.chat.ui.theme.ChatTheme
import io.romix.chat.viewmodel.ChatViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val chatViewModel = hiltViewModel<ChatViewModel>()

            val collectAsStateWithLifecycle = chatViewModel.messages.collectAsStateWithLifecycle()

            ChatTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val loginInput = remember { mutableStateOf("romix") }
                    val chatInput = remember { mutableStateOf("") }

                    Column {
                        OutlinedTextField(
                            value = loginInput.value,
                            onValueChange = { input -> loginInput.value = input },
                            label = { Text("Login") },
                        )

                        Button(onClick = { chatViewModel.login(loginInput.value) }) {
                            Text(text = "Login")
                        }
                        Button(onClick = { chatViewModel.connectToChat() }) {
                            Text(text = "Connect")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = chatInput.value,
                                onValueChange = { input -> chatInput.value = input },
                                label = { Text("Enter a message") },
                            )
                            Button(onClick = {
                                chatViewModel.sendMessage(chatInput.value)
                                chatInput.value = ""
                            }) {
                                Text(text = "Send")
                            }
                        }
                        LazyColumn {
                            items(collectAsStateWithLifecycle.value) { message ->
                                Text(text = "message: ${message.payload}")
                            }
                        }
                    }
                }
            }
        }
    }
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