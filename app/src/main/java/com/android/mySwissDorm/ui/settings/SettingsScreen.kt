package com.android.mySwissDorm.ui.settings

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.ui.navigation.BottomNavigationMenu
import com.android.mySwissDorm.ui.navigation.NavigationActions
import com.android.mySwissDorm.ui.navigation.Screen
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.ui.theme.PalePink
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.theme.White
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** Centralized test tags for the Settings screen. */
object SettingsTestTags {
  const val SettingsScroll = "SettingsScroll"
  const val ProfileButton = "ProfileButton"
  const val EmailField = "EmailField"
  const val DeleteAccountButton = "DeleteAccountButton"
  const val BlockedContactsToggle = "BlockedContactsToggle"
  const val BlockedContactsList = "BlockedContactsList"
  const val BottomBar = "bottom_nav"

  fun switch(label: String) = "SettingSwitch_$label"
}

/** Build a SettingsViewModel with real Firebase singletons (works in app & in e2e). */
@Composable
private fun rememberSettingsViewModel(): SettingsViewModel {
  val factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          val auth = FirebaseAuth.getInstance()
          val repo = ProfileRepositoryFirestore(FirebaseFirestore.getInstance())
          return SettingsViewModel(auth = auth, profiles = repo) as T
        }
      }
  return viewModel(factory = factory)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onItemClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    navigationActions: NavigationActions? = null,
    // IMPORTANT: supply our own factory to avoid “Cannot create an instance…” in app/E2E
    vm: SettingsViewModel = rememberSettingsViewModel(),
    isAdmin: Boolean = false,
    onAdminClick: () -> Unit = {}
) {
  val ui by vm.uiState.collectAsState()

  LaunchedEffect(Unit) { vm.refresh() }

  val context = LocalContext.current
  LaunchedEffect(ui.errorMsg) {
    ui.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      vm.clearError()
    }
  }

  SettingsScreenContent(
      ui = ui,
      onItemClick = {
        vm.onItemClick(it)
        onItemClick(it)
      },
      onProfileClick = onProfileClick,
      onDeleteAccount = { vm.deleteAccount { _, _ -> } },
      navigationActions = navigationActions,
      isAdmin = isAdmin,
      onAdminClick = onAdminClick)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreenContent(
    ui: SettingsUiState,
    onItemClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    navigationActions: NavigationActions? = null,
    isAdmin: Boolean = false,
    onAdminClick: () -> Unit = {}
) {
  var notificationsMessages by remember { mutableStateOf(true) }
  var notificationsListings by remember { mutableStateOf(false) }
  var readReceipts by remember { mutableStateOf(true) }
  var blockedExpanded by remember { mutableStateOf(false) }
  val blockedContacts = listOf("Clarisse K.", "Alice P.", "Benjamin M.")
  val focusManager = LocalFocusManager.current
  var showDeleteConfirm by remember { mutableStateOf(false) }

  Scaffold(
      containerColor = BackGroundColor,
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Settings") },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackGroundColor, titleContentColor = TextColor))
      },
      bottomBar = {
        if (navigationActions != null) {
          BottomNavigationMenu(
              selectedScreen = Screen.Settings,
              onTabSelected = { screen ->
                if (screen != Screen.Settings) navigationActions.navigateTo(screen)
              })
        }
      }) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(BackGroundColor)) {
              val maxW = this.maxWidth
              val isTablet = maxW >= 600.dp
              val isCompact = maxW < 360.dp
              val horizontalPad: Dp =
                  when {
                    isTablet -> 24.dp
                    isCompact -> 12.dp
                    else -> 16.dp
                  }
              val contentWidthCap = if (isTablet) 600.dp else maxW

              LazyColumn(
                  modifier = Modifier.fillMaxSize().testTag(SettingsTestTags.SettingsScroll),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  contentPadding =
                      PaddingValues(
                          start = horizontalPad,
                          end = horizontalPad,
                          top = 12.dp,
                          bottom = 24.dp)) {
                    item {
                      Column(modifier = Modifier.fillMaxWidth().widthIn(max = contentWidthCap)) {
                        // Profile card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = BackGroundColor),
                            shape = MaterialTheme.shapes.large,
                            border = androidx.compose.foundation.BorderStroke(1.dp, TextBoxColor)) {
                              Row(
                                  modifier = Modifier.fillMaxWidth().padding(16.dp),
                                  verticalAlignment = Alignment.CenterVertically,
                                  horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    val initial =
                                        (ui.userName.firstOrNull()?.uppercaseChar() ?: 'U')
                                    Surface(
                                        modifier = Modifier.size(if (isTablet) 64.dp else 56.dp),
                                        color = PalePink.copy(alpha = 0.16f),
                                        shape = CircleShape) {
                                          Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                initial.toString(),
                                                fontWeight = FontWeight.Bold,
                                                color = MainColor)
                                          }
                                        }
                                    Column(modifier = Modifier.weight(1f)) {
                                      Text(
                                          ui.userName.ifBlank { "User" },
                                          style = MaterialTheme.typography.titleMedium,
                                          maxLines = 1,
                                          overflow = TextOverflow.Ellipsis)
                                      Text(
                                          "View profile",
                                          style = MaterialTheme.typography.bodySmall,
                                          color = MainColor)
                                    }
                                    IconButton(
                                        onClick = onProfileClick,
                                        modifier =
                                            Modifier.testTag(SettingsTestTags.ProfileButton)) {
                                          Icon(
                                              imageVector = Icons.Filled.ChevronRight,
                                              contentDescription = "Open profile")
                                        }
                                  }
                            }

                        SectionLabel("Notifications")
                        CardBlock {
                          SettingSwitchRow(
                              label = "Show notifications for messages",
                              checked = notificationsMessages,
                              onCheckedChange = { notificationsMessages = it })
                          SoftDivider()
                          SettingSwitchRow(
                              label = "Show notifications for new listings",
                              checked = notificationsListings,
                              onCheckedChange = { notificationsListings = it })
                        }

                        SectionLabel("Account")
                        CardBlock {
                          OutlinedTextField(
                              value = ui.email,
                              onValueChange = {},
                              label = { Text("Email address") },
                              singleLine = true,
                              readOnly = true,
                              enabled = false,
                              keyboardOptions =
                                  androidx.compose.foundation.text.KeyboardOptions(
                                      imeAction = ImeAction.Done),
                              keyboardActions =
                                  androidx.compose.foundation.text.KeyboardActions(
                                      onDone = { focusManager.clearFocus() }),
                              modifier =
                                  Modifier.fillMaxWidth().testTag(SettingsTestTags.EmailField))
                        }

                        SectionLabel("Privacy")
                        CardBlock {
                          SettingSwitchRow(
                              label = "Read receipts",
                              checked = readReceipts,
                              onCheckedChange = { readReceipts = it })
                          SoftDivider()

                          Row(
                              modifier = Modifier.fillMaxWidth().padding(4.dp, 10.dp),
                              verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Blocked contacts (${blockedContacts.size})",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                val rotation by
                                    animateFloatAsState(
                                        targetValue = if (blockedExpanded) 90f else 0f,
                                        label = "blockedArrowRotation")
                                IconButton(
                                    onClick = { blockedExpanded = !blockedExpanded },
                                    modifier =
                                        Modifier.testTag(SettingsTestTags.BlockedContactsToggle)) {
                                      Icon(
                                          imageVector =
                                              Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                          contentDescription =
                                              if (blockedExpanded) "Hide blocked"
                                              else "Show blocked",
                                          modifier = Modifier.rotate(rotation))
                                    }
                              }

                          val bring = remember { BringIntoViewRequester() }
                          LaunchedEffect(blockedExpanded) {
                            if (blockedExpanded) bring.bringIntoView()
                          }
                          if (blockedExpanded) {
                            Surface(
                                color = BackGroundColor,
                                shape = MaterialTheme.shapes.medium,
                                border =
                                    androidx.compose.foundation.BorderStroke(1.dp, TextBoxColor),
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .bringIntoViewRequester(bring)
                                        .testTag(SettingsTestTags.BlockedContactsList)) {
                                  Column(Modifier.padding(12.dp)) {
                                    blockedContacts.forEach { name ->
                                      Text(
                                          name,
                                          style = MaterialTheme.typography.bodyMedium,
                                          color = TextColor,
                                          modifier = Modifier.padding(vertical = 4.dp),
                                          maxLines = 1,
                                          overflow = TextOverflow.Ellipsis)
                                    }
                                  }
                                }
                          }
                        }

                        SectionLabel("Accessibility")
                        CardBlock {
                          SettingSwitchRow(
                              label = "Night Shift",
                              checked = notificationsMessages,
                              onCheckedChange = { notificationsMessages = it })
                          SoftDivider()
                          SettingSwitchRow(
                              label = "Anonymous",
                              checked = notificationsListings,
                              onCheckedChange = { notificationsListings = it })
                        }
                      }
                    }
                    item {
                      if (isAdmin) {
                        SectionLabel("Admin")
                        CardBlock {
                          Row(
                              modifier =
                                  Modifier.fillMaxWidth()
                                      .padding(horizontal = 16.dp, vertical = 10.dp)
                                      .clickable(onClick = onAdminClick),
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "Admin page",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextColor,
                                )
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = "Open admin page")
                              }
                        }
                      }
                    }

                    item {
                      Button(
                          onClick = { showDeleteConfirm = true },
                          enabled = !ui.isDeleting,
                          colors =
                              ButtonDefaults.buttonColors(
                                  containerColor = BackGroundColor,
                                  contentColor = MainColor,
                                  disabledContainerColor = BackGroundColor,
                                  disabledContentColor = MainColor.copy(alpha = 0.5f)),
                          shape = RoundedCornerShape(28.dp),
                          elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                          border =
                              androidx.compose.foundation.BorderStroke(
                                  1.dp, MainColor.copy(alpha = 0.15f)),
                          modifier =
                              Modifier.fillMaxWidth()
                                  .padding(top = 8.dp)
                                  .testTag(SettingsTestTags.DeleteAccountButton)
                                  .navigationBarsPadding()) {
                            Text(if (ui.isDeleting) "DELETING…" else "DELETE MY ACCOUNT")
                          }
                    }
                  }
            }
      }

  if (showDeleteConfirm) {
    AlertDialog(
        onDismissRequest = { showDeleteConfirm = false },
        title = { Text("Delete account?") },
        text = {
          Text("This will permanently remove your account. You may need to re-authenticate.")
        },
        confirmButton = {
          TextButton(
              onClick = {
                showDeleteConfirm = false
                onDeleteAccount()
              }) {
                Text("Delete")
              }
        },
        dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } })
  }
}

/* ---------- small UI helpers ---------- */

@Composable
private fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
  Surface(
      color = BackGroundColor,
      shape = MaterialTheme.shapes.large,
      border = androidx.compose.foundation.BorderStroke(1.dp, TextBoxColor),
      shadowElevation = 0.dp,
      tonalElevation = 0.dp,
      modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), content = content)
      }
}

@Composable
private fun SectionLabel(text: String) {
  Text(
      text = text,
      style = MaterialTheme.typography.titleMedium,
      color = TextColor,
      modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
}

@Composable
private fun SoftDivider() {
  HorizontalDivider(thickness = 1.dp, color = TextBoxColor.copy(alpha = 0.25f))
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  val screenWidthDp = LocalConfiguration.current.screenWidthDp
  val isExtraNarrow = screenWidthDp < 340
  if (!isExtraNarrow) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
          Text(
              label,
              style = MaterialTheme.typography.bodyLarge,
              color = TextColor,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f).padding(end = 12.dp))
          Switch(
              modifier =
                  Modifier.testTag(SettingsTestTags.switch(label)).semantics(
                      mergeDescendants = true) {
                        role = Role.Switch
                        stateDescription = if (checked) "On" else "Off"
                      },
              checked = checked,
              onCheckedChange = onCheckedChange,
              colors =
                  SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = MainColor))
        }
  } else {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
      Text(
          label,
          style = MaterialTheme.typography.bodyLarge,
          color = TextColor,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis)
      Row(
          modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
          horizontalArrangement = Arrangement.End) {
            Switch(
                modifier =
                    Modifier.testTag(SettingsTestTags.switch(label)).semantics(
                        mergeDescendants = true) {
                          role = Role.Switch
                          stateDescription = if (checked) "On" else "Off"
                        },
                checked = checked,
                onCheckedChange = onCheckedChange)
          }
    }
  }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SettingsScreenPreviewPhone() {
  MySwissDormAppTheme { SettingsScreenContent(ui = SettingsUiState()) }
}

@Preview(showBackground = true, widthDp = 700)
@Composable
private fun SettingsScreenPreviewTablet() {
  MySwissDormAppTheme { SettingsScreenContent(ui = SettingsUiState()) }
}
