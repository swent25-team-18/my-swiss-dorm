package com.android.mySwissDorm.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C.ViewUserProfileTags as T
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.github.se.bootcamp.ui.profile.ViewProfileScreenViewModel
import com.github.se.bootcamp.ui.profile.ViewProfileUiState
import com.google.firebase.auth.FirebaseAuth

/**
 * Displays a read-only view of another user's profile.
 *
 * This composable supports two modes:
 * 1) **Runtime mode** (default): provide a [ViewProfileScreenViewModel] (or let it be created) and
 *    a non-null [ownerId]. The VM will be used and data loaded.
 * 2) **Preview / Static mode**: pass a non-null [previewUi]. When [previewUi] is provided, the VM
 *    is never touched and no data is loaded (useful for @Preview).
 *
 * Test tags are provided via [T] for stable UI tests.
 *
 * @param viewModel Optional VM. If null (and not in preview), one will be created via [viewModel].
 * @param ownerId The user id of the profile owner. Required in runtime mode. Null for preview.
 * @param onBack Callback invoked when the top bar back icon is tapped.
 * @param onSendMessage Callback invoked when "Send a message" row is tapped. It is gated on a
 *   non-null [ownerId].
 * @param previewUi If non-null, the screen renders from this state only (VM and loading are
 *   skipped).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewUserProfileScreen(
    viewModel: ViewProfileScreenViewModel? = null,
    ownerId: String?,
    onBack: () -> Unit,
    onSendMessage: () -> Unit,
    previewUi: ViewProfileUiState? = null
) {
  // Obtain a real VM only when NOT in preview
  val realVm: ViewProfileScreenViewModel? =
      if (previewUi == null) (viewModel ?: viewModel()) else null

  // Trigger data load only when we have a VM and a real ownerId
  if (realVm != null && ownerId != null) {
    LaunchedEffect(ownerId) {
      // Idempotent load tied to ownerId changes
      realVm.loadProfile(ownerId)
    }
  }

  // Decide the UI source (previewUi takes precedence when provided)
  val ui: ViewProfileUiState =
      previewUi
          ?: run {
            // In runtime mode, observe the VM's state
            val vmUi by realVm!!.uiState.collectAsState()
            vmUi
          }

  val context = LocalContext.current
  val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
  val isCurrentUser = ownerId == currentUserId

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Text(
                  text = ui.name, // no loading placeholder
                  modifier = Modifier.testTag(T.TITLE),
                  style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                  textAlign = TextAlign.Center)
            },
            navigationIcon = {
              IconButton(onClick = onBack, modifier = Modifier.testTag(T.BACK_BTN)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MainColor)
              }
            })
      }) { padding ->
        if (ui.error != null) {
          // Error state + Retry (no composable calls inside onClick)
          Box(
              modifier = Modifier.fillMaxSize().padding(padding),
              contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("Error: ${ui.error}", modifier = Modifier.testTag(T.ERROR_TEXT))
                  Spacer(Modifier.height(12.dp))
                  Button(
                      onClick = { if (ownerId != null) realVm?.loadProfile(ownerId) },
                      modifier = Modifier.testTag(T.RETRY_BTN)) {
                        Text(stringResource(R.string.retry))
                      }
                }
              }
        } else {
          // Normal content
          LazyColumn(
              modifier = Modifier.fillMaxSize().padding(padding).testTag(T.ROOT),
              contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp)) {
                item {
                  // Center avatar horizontally with a full-width wrapper
                  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    // Avatar with red ring + person icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier.size(180.dp)
                                .clip(CircleShape)
                                .border(3.dp, MainColor, CircleShape)
                                .testTag(T.AVATAR_BOX)) {
                          Box(
                              modifier =
                                  Modifier.size(150.dp)
                                      .clip(CircleShape)
                                      .background(BackGroundColor),
                              contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "Profile picture",
                                    tint = MainColor,
                                    modifier = Modifier.size(64.dp))
                              }
                        }
                  }
                  Spacer(Modifier.height(28.dp))
                }

                // Residence chip (only when non-blank)
                if (ui.residence.isNotBlank()) {
                  item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TextBoxColor,
                        modifier =
                            Modifier.fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag(T.RESIDENCE_CHIP)) {
                          Box(
                              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                              contentAlignment = Alignment.CenterStart) {
                                Text(
                                    text = ui.residence,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextColor)
                              }
                        }
                    Spacer(Modifier.height(16.dp))
                  }
                }

                // Send a message row (enabled only when ownerId is non-null and not current user)
                if (!isCurrentUser && ownerId != null) {
                  item {
                    Surface(
                        onClick = { onSendMessage() },
                        shape = RoundedCornerShape(12.dp),
                        color = TextBoxColor,
                        modifier =
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(T.SEND_MESSAGE)) {
                          Row(
                              verticalAlignment = Alignment.CenterVertically,
                              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.Forum,
                                    contentDescription = null,
                                    tint = TextColor)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.view_user_profile_send_message),
                                    style = MaterialTheme.typography.bodyLarge.copy(),
                                    color = TextColor)
                              }
                        }
                  }
                }

                // Block user row (enabled only when ownerId is non-null and not current user)
                if (!isCurrentUser && ownerId != null && realVm != null) {
                  item {
                    Spacer(Modifier.height(12.dp))
                    val isBlocked = ui.isBlocked
                    val buttonColor = if (isBlocked) MainColor else Color(0xFFFF4444)
                    val textColor = if (isBlocked) BackGroundColor else Color.White
                    val iconColor = textColor
                    val buttonText =
                        if (isBlocked) stringResource(R.string.view_user_profile_unblock_user)
                        else stringResource(R.string.view_user_profile_block_user)

                    Surface(
                        onClick = {
                          ownerId?.let { targetUid ->
                            if (isBlocked) {
                              realVm.unblockUser(
                                  targetUid,
                                  onError = { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                  },
                                  context = context)
                            } else {
                              realVm.blockUser(
                                  targetUid,
                                  onError = { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                  },
                                  context = context)
                            }
                          }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = buttonColor,
                        modifier =
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(T.BLOCK_BUTTON)) {
                          Row(
                              verticalAlignment = Alignment.CenterVertically,
                              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.ReportProblem,
                                    contentDescription = null,
                                    tint = iconColor)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = buttonText,
                                    style = MaterialTheme.typography.bodyLarge.copy(),
                                    color = textColor)
                              }
                        }
                  }
                }
              }
        }
      }
}

/**
 * Preview that never touches the ViewModel. We provide a static previewUI so the composable renders
 * without creating or using a VM.
 */
@Preview(showBackground = true, name = "ViewUserProfile – Preview")
@Composable
private fun Preview_ViewUserProfile() {
  MySwissDormAppTheme {
    ViewUserProfileScreen(
        viewModel = null, // do NOT create a VM in preview
        ownerId = null,
        onBack = {},
        onSendMessage = {},
        previewUi =
            ViewProfileUiState(
                name = "Mansour Kanaan", residence = "Vortex, Coloc", image = null, error = null))
  }
}
