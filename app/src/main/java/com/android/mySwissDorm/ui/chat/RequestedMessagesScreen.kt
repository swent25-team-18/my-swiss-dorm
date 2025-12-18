package com.android.mySwissDorm.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessage
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.Dimens
import com.android.mySwissDorm.ui.theme.Green
import com.android.mySwissDorm.ui.theme.LightGray0
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.google.firebase.auth.FirebaseAuth

/**
 * Screen displaying all pending message requests that require approval or rejection.
 *
 * This screen is used by listing owners to manage incoming contact message requests from potential
 * renters. When a user sends a contact message through a listing, it appears here as a pending
 * request that the listing owner can approve or reject.
 *
 * **Features:**
 * - Displays all pending message requests for the current user (as the receiver)
 * - Shows sender's name (clickable to view their profile), listing title, and message content
 * - Approve action (✓): Creates a Stream Chat channel between the users and removes the request
 * - Reject action (✗): Removes the request without creating a chat channel
 * - Automatically refreshes the list after approve/reject actions
 * - Shows loading state while fetching messages
 * - Shows empty state when no pending messages exist
 *
 * @param modifier Modifier to be applied to the screen
 * @param onBackClick Callback invoked when the back button is clicked
 * @param onApprove Callback invoked when a message is approved. Receives the message ID. The
 *   ViewModel handles all approval logic including Stream Chat channel creation.
 * @param onReject Callback invoked when a message is rejected. Receives the message ID. The
 *   ViewModel handles all rejection logic.
 * @param onViewProfile Callback invoked when the sender's name is clicked. Receives the sender's
 *   user ID and should navigate to their profile screen.
 * @param viewModel The ViewModel instance (defaults to viewModel() if not provided)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestedMessagesScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onApprove: (String) -> Unit = { _ ->
      // Default implementation uses ViewModel - can be overridden for testing
    },
    onReject: (String) -> Unit = { _ ->
      // Default implementation uses ViewModel - can be overridden for testing
    },
    onViewProfile: (String) -> Unit = {},
    viewModel: RequestedMessagesViewModel = viewModel()
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()

  // Load messages when screen is first displayed
  LaunchedEffect(Unit) { viewModel.loadMessages(context) }

  // Handle errors and success messages
  LaunchedEffect(uiState.error) {
    uiState.error?.let { error ->
      android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
      viewModel.clearError()
    }
  }

  LaunchedEffect(uiState.successMessage) {
    uiState.successMessage?.let { message ->
      android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
      viewModel.clearSuccessMessage()
    }
  }

  val currentUser = FirebaseAuth.getInstance().currentUser
  if (currentUser == null) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(stringResource(R.string.view_user_profile_not_signed_in))
    }
    return
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.requested_messages), color = TextColor) },
            navigationIcon = {
              IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = BackGroundColor,
                    titleContentColor = TextColor,
                    navigationIconContentColor = TextColor))
      }) { paddingValues ->
        when {
          uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(color = MainColor)
                }
          }
          uiState.messages.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center) {
                  Text(
                      text = stringResource(R.string.no_requested_messages),
                      style = MaterialTheme.typography.bodyLarge,
                      color = LightGray0)
                }
          }
          else -> {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
              items(uiState.messages) { enrichedMessage ->
                RequestedMessageItem(
                    message = enrichedMessage.message,
                    fromUserName = enrichedMessage.senderName,
                    fromUserImageUrl = enrichedMessage.senderImageUrl,
                    fromUserId = enrichedMessage.message.fromUserId,
                    onApprove = {
                      onApprove(enrichedMessage.message.id)
                      viewModel.approveMessage(enrichedMessage.message.id, context)
                    },
                    onReject = {
                      onReject(enrichedMessage.message.id)
                      viewModel.rejectMessage(enrichedMessage.message.id, context)
                    },
                    onViewProfile = { onViewProfile(enrichedMessage.message.fromUserId) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.PaddingDefault))
              }
            }
          }
        }
      }
}

/**
 * Composable item representing a single requested message in the list.
 *
 * Displays the sender's name (clickable to view profile), the listing title the message is about,
 * the message content, and action buttons for approve/reject.
 *
 * **Layout:**
 * - Left side: Sender name (bold, clickable, in main color), listing title, and message content
 * - Right side: Approve (green check) and Reject (red X) icon buttons aligned with the sender's
 *   name
 *
 * @param message The [RequestedMessage] to display
 * @param fromUserName The display name of the message sender
 * @param fromUserImageUrl Optional profile image URL of the sender (currently not displayed)
 * @param fromUserId The user ID of the sender (used for navigation)
 * @param onApprove Callback invoked when the approve button is clicked
 * @param onReject Callback invoked when the reject button is clicked
 * @param onViewProfile Callback invoked when the sender's name is clicked
 * @param modifier Modifier to be applied to the item
 */
@Composable
fun RequestedMessageItem(
    message: RequestedMessage,
    fromUserName: String,
    fromUserImageUrl: String?,
    fromUserId: String,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onViewProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(horizontal = Dimens.PaddingDefault, vertical = Dimens.PaddingXSmall),
      colors = CardDefaults.cardColors(containerColor = BackGroundColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.PaddingDefault),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              // Left side: Name (clickable) and listing title
              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fromUserName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onViewProfile),
                    color = MainColor)
                Text(
                    text = "About: ${message.listingTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LightGray0)
                Spacer(modifier = Modifier.height(Dimens.SpacingXSmall))
                Text(
                    text = message.message.ifBlank { stringResource(R.string.no_message_provided) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextColor)
              }

              // Right side: Action buttons aligned with name
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onApprove, modifier = Modifier.size(Dimens.IconSizeButton)) {
                  Icon(Icons.Default.Check, contentDescription = "Approve", tint = Green)
                }
                IconButton(onClick = onReject, modifier = Modifier.size(Dimens.IconSizeButton)) {
                  Icon(Icons.Default.Close, contentDescription = "Reject", tint = MainColor)
                }
              }
            }
      }
}
