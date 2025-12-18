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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.resources.C.ViewUserProfileTags as T
import com.android.mySwissDorm.ui.theme.AlmostWhite
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.Black
import com.android.mySwissDorm.ui.theme.Dimens
import com.android.mySwissDorm.ui.theme.Gray
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.ui.theme.OutlineColor
import com.android.mySwissDorm.ui.theme.Red
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.theme.White
import com.google.firebase.auth.FirebaseAuth

/**
 * Displays a read-only view of another user's profile.
 *
 * This composable supports two modes:
 * 1) **Runtime mode** (default): provide a [ViewProfileScreenViewModel] (or let it be created via
 *    [viewModel]) and a non-null [ownerId]. The VM will be used and data loaded.
 * 2) **Preview / Static mode**: pass a non-null [previewUi]. When [previewUi] is provided, the VM
 *    is never touched and no data is loaded (useful for @Preview).
 *
 * Test tags are provided via [T] for stable UI tests.
 *
 * @param viewModel Optional ViewModel instance. If null, a new instance will be created via
 *   [viewModel].
 * @param ownerId The UID of the user whose profile to display. Required in runtime mode, ignored in
 *   preview mode.
 * @param onBack Callback invoked when the back button is clicked.
 * @param onSendMessage Callback invoked when the send message button is clicked.
 * @param previewUi Optional preview UI state. When provided, the composable operates in preview
 *   mode and displays this static data without loading from the repository.
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
  val context = LocalContext.current

  // Obtain a real VM only when NOT in preview
  val realVm: ViewProfileScreenViewModel? =
      if (previewUi == null) (viewModel ?: viewModel()) else null

  // Trigger data load only when we have a VM and a real ownerId
  if (realVm != null && ownerId != null) {
    LaunchedEffect(ownerId) { realVm.loadProfile(ownerId, context) }
  }

  // Observe UI state
  val ui: ViewProfileUiState =
      previewUi
          ?: run {
            val vmUi by realVm!!.uiState.collectAsState()
            vmUi
          }

  val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
  val isCurrentUser = ownerId == currentUserId

  val latestIsBlocked by rememberUpdatedState(ui.isBlocked)
  val latestOwnerId by rememberUpdatedState(ownerId)

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Text(
                  text = ui.name,
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
          Box(
              modifier = Modifier.fillMaxSize().padding(padding),
              contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                      "${stringResource(R.string.error)}: ${ui.error}",
                      modifier = Modifier.testTag(T.ERROR_TEXT))
                  Spacer(Modifier.height(Dimens.SpacingLarge))
                  Button(
                      onClick = { if (ownerId != null) realVm?.loadProfile(ownerId, context) },
                      modifier = Modifier.testTag(T.RETRY_BTN)) {
                        Text(stringResource(R.string.retry))
                      }
                }
              }
        } else {
          LazyColumn(
              modifier = Modifier.fillMaxSize().padding(padding).testTag(T.ROOT),
              contentPadding =
                  PaddingValues(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingLarge)) {
                item {
                  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier.size(Dimens.ImageSizeAvatarLarge)
                                .clip(CircleShape)
                                .border(Dimens.BorderWidthSmall, MainColor, CircleShape)
                                .testTag(T.AVATAR_BOX)) {
                          Box(
                              modifier =
                                  Modifier.fillMaxSize()
                                      .clip(CircleShape)
                                      .background(BackGroundColor),
                              contentAlignment = Alignment.Center) {
                                if (!ui.isBlocked && ui.profilePicture != null) {
                                  AsyncImage(
                                      model = ui.profilePicture.image,
                                      contentDescription = "Profile picture",
                                      contentScale = ContentScale.Crop,
                                      modifier =
                                          Modifier.fillMaxSize()
                                              .testTag(C.ViewUserProfileTags.PROFILE_PICTURE),
                                  )
                                } else {
                                  Icon(
                                      imageVector = Icons.Outlined.Person,
                                      contentDescription = "Profile picture",
                                      tint = MainColor,
                                      modifier =
                                          Modifier.size(Dimens.ImageSizeSmall)
                                              .testTag(C.ViewUserProfileTags.PROFILE_PICTURE))
                                }
                              }
                        }
                  }
                  Spacer(Modifier.height(Dimens.SpacingXXLarge))
                }

                if (ui.residence.isNotBlank()) {
                  item {
                    Surface(
                        shape = RoundedCornerShape(Dimens.CornerRadiusDefault),
                        color = TextBoxColor,
                        modifier =
                            Modifier.fillMaxWidth()
                                .heightIn(min = Dimens.IconSizeXXLarge)
                                .testTag(T.RESIDENCE_CHIP)) {
                          Box(
                              modifier =
                                  Modifier.fillMaxWidth()
                                      .padding(horizontal = Dimens.PaddingDefault),
                              contentAlignment = Alignment.CenterStart) {
                                Text(
                                    text = ui.residence,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextColor)
                              }
                        }
                    Spacer(Modifier.height(Dimens.SpacingXLarge))
                  }
                }

                if (!isCurrentUser && ownerId != null) {
                  item {
                    if (ui.hasExistingMessage) {
                      Surface(
                          shape = RoundedCornerShape(Dimens.CornerRadiusDefault),
                          color = TextBoxColor,
                          modifier = Modifier.fillMaxWidth().testTag(T.SEND_MESSAGE)) {
                            Column(
                                modifier = Modifier.padding(Dimens.PaddingDefault),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                  Text(
                                      text = stringResource(R.string.view_profile_has_msg),
                                      style = MaterialTheme.typography.bodyLarge,
                                      color = MainColor,
                                      textAlign = TextAlign.Center)
                                  Text(
                                      text =
                                          stringResource(R.string.view_profile_has_msg_secondary),
                                      style = MaterialTheme.typography.bodyMedium,
                                      color = Gray,
                                      textAlign = TextAlign.Center)
                                }
                          }
                    } else if (!ui.isBlocked) {
                      Column(
                          verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall),
                          modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = ui.messageText,
                                onValueChange = { realVm?.updateMessageText(it) },
                                placeholder = {
                                  Text(stringResource(R.string.write_msg), color = Gray)
                                },
                                modifier = Modifier.fillMaxWidth().testTag(T.SEND_MESSAGE),
                                shape = RoundedCornerShape(Dimens.CardCornerRadius),
                                minLines = 2,
                                maxLines = 4,
                                colors =
                                    OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = AlmostWhite,
                                        unfocusedContainerColor = AlmostWhite,
                                        disabledContainerColor = AlmostWhite,
                                        focusedBorderColor = OutlineColor,
                                        unfocusedBorderColor = OutlineColor,
                                        cursorColor = Black,
                                        focusedTextColor = Black))

                            Button(
                                onClick = { realVm?.sendDirectMessage(context, ownerId) },
                                enabled = ui.messageText.isNotBlank(),
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                shape = RoundedCornerShape(Dimens.CardCornerRadius),
                                colors = ButtonDefaults.buttonColors(containerColor = MainColor)) {
                                  Icon(
                                      imageVector = Icons.AutoMirrored.Outlined.Send,
                                      contentDescription = null,
                                      modifier = Modifier.size(Dimens.PaddingDefault))
                                  Spacer(Modifier.width(Dimens.SpacingSmall))
                                  Text(stringResource(R.string.view_user_profile_send_message))
                                }
                          }
                    }
                  }
                }

                if (!isCurrentUser && ownerId != null && realVm != null) {
                  item {
                    Spacer(Modifier.height(Dimens.SpacingLarge))

                    // UI appearance can use ui.isBlocked directly (recomposes fine)
                    val buttonColor = if (ui.isBlocked) MainColor else Red
                    val textColor = if (ui.isBlocked) BackGroundColor else White
                    val iconColor = textColor
                    val buttonText =
                        if (ui.isBlocked) stringResource(R.string.view_user_profile_unblock_user)
                        else stringResource(R.string.view_user_profile_block_user)

                    Surface(
                        onClick = {
                          val targetUid = latestOwnerId ?: return@Surface
                          if (latestIsBlocked) {
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
                        },
                        shape = RoundedCornerShape(Dimens.CornerRadiusDefault),
                        color = buttonColor,
                        modifier =
                            Modifier.fillMaxWidth()
                                .heightIn(min = Dimens.IconSizeXXLarge)
                                .testTag(T.BLOCK_BUTTON)) {
                          Row(
                              verticalAlignment = Alignment.CenterVertically,
                              modifier =
                                  Modifier.fillMaxWidth()
                                      .padding(horizontal = Dimens.PaddingDefault)) {
                                Icon(
                                    imageVector = Icons.Outlined.ReportProblem,
                                    contentDescription = null,
                                    tint = iconColor)
                                Spacer(Modifier.width(Dimens.SpacingLarge))
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

@Preview(showBackground = true, name = "ViewUserProfile – Preview")
@Composable
private fun Preview_ViewUserProfile() {
  MySwissDormAppTheme {
    ViewUserProfileScreen(
        viewModel = null,
        ownerId = null,
        onBack = {},
        onSendMessage = {},
        previewUi =
            ViewProfileUiState(
                name = "Mansour Kanaan", residence = "Vortex, Coloc", image = null, error = null))
  }
}
