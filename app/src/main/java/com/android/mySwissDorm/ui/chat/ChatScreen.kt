// MyChatScreen.kt
package com.android.mySwissDorm.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.mySwissDorm.model.chat.StreamChatProvider
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import io.getstream.chat.android.compose.ui.messages.MessagesScreen
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.compose.viewmodel.messages.MessagesViewModelFactory
import kotlinx.coroutines.delay

@Composable
fun MyChatScreen(channelId: String, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
  val isPreview = LocalInspectionMode.current

  if (isPreview) {
    // Fake chat UI for preview only
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Chat with: $channelId (preview)")
          repeat(4) { Text("Message $it: lorem ipsum…") }
        }
  } else {
    val context = LocalContext.current
    val chatClient = StreamChatProvider.getClient()
    val currentUser = FirebaseAuth.getInstance().currentUser

    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }

    // Check connection and connect if needed
    LaunchedEffect(channelId) {
      val user = chatClient.clientState.user.value
      if (user != null) {
        isConnected = true
      } else {
        // User not connected, try to connect
        if (currentUser != null && !isConnecting) {
          isConnecting = true
          try {
            val profile = ProfileRepositoryProvider.repository.getProfile(currentUser.uid)
            StreamChatProvider.connectUser(
                firebaseUserId = currentUser.uid,
                displayName = "${profile.userInfo.name} ${profile.userInfo.lastName}".trim(),
                imageUrl = "")
            // Wait for connection to establish
            delay(1500)
            isConnected = true
          } catch (e: Exception) {
            android.util.Log.e("MyChatScreen", "Failed to connect to Stream Chat", e)
            // Still try to proceed, might work if connection was already in progress
            delay(1000)
            isConnected = chatClient.clientState.user.value != null
          } finally {
            isConnecting = false
          }
        }
      }
    }

    if (!isConnected || isConnecting) {
      // Show loading while connecting
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator()
          Spacer(modifier = Modifier.height(16.dp))
          Text("Connecting to chat...")
        }
      }
    } else {
      val viewModelFactory =
          remember(channelId) {
            MessagesViewModelFactory(
                context = context,
                chatClient = chatClient,
                channelId = channelId,
                messageLimit = 30)
          }

      MessagesScreen(
          viewModelFactory = viewModelFactory,
          onBackPressed = onBackClick,
      )
    }
  }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MyChatScreenPreview() {
  ChatTheme { MyChatScreen(channelId = "messaging:preview", onBackClick = {}) }
}
