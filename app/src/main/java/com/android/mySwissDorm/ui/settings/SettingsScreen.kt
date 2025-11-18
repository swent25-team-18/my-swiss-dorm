package com.android.mySwissDorm.ui.settings

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.navigation.BottomBarFromNav
import com.android.mySwissDorm.ui.navigation.NavigationActions
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.ui.theme.PalePink
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.theme.White
import com.android.mySwissDorm.ui.theme.rememberDarkModePreference

// Documentation was made with the help of AI
/**
 * Settings screen for user preferences and account management.
 *
 * This screen provides a comprehensive interface for users to manage their app settings, account
 * information, and preferences. It includes:
 * - **Profile Section**: Displays user name and avatar with navigation to profile screen
 * - **Notifications**: Toggle switches for message and listing notifications
 * - **Account**: Email display and contributions button
 * - **Privacy**: Read receipts toggle and blocked contacts management
 * - **Accessibility**: Dark mode preference and anonymous mode toggle
 * - **Admin Section**: Admin page access (only visible to admins)
 * - **Delete Account**: Button to permanently delete the user account
 *
 * The screen features:
 * - Responsive layout that adapts to tablet and compact screen sizes
 * - Dark mode preference that persists across app restarts
 * - Blocked contacts expandable list with unblock functionality
 * - Input sanitization and validation
 * - Loading states and error handling
 *
 * @param onItemClick Callback invoked when a settings item is clicked (currently not implemented).
 * @param onProfileClick Callback invoked when the profile button is clicked to navigate to profile
 *   screen.
 * @param navigationActions Optional [NavigationActions] for bottom bar navigation. If null, bottom
 *   bar is hidden.
 * @param vm The [SettingsViewModel] that manages the settings state and user data. Defaults to a
 *   new instance created via [viewModel].
 * @param isAdmin Whether the current user has admin access. If true, displays Admin section.
 * @param onAdminClick Callback invoked when the admin page button is clicked.
 * @param onContributionClick Callback invoked when the contributions button is clicked.
 * @see SettingsViewModel for state management and user data handling
 * @see SettingsScreenContent for the actual UI implementation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onItemClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    navigationActions: NavigationActions? = null,
    vm: SettingsViewModel = viewModel(),
    isAdmin: Boolean = false,
    onAdminClick: () -> Unit = {},
    onContributionClick: () -> Unit = {}
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
      onContributionClick = onContributionClick,
      onUnblockUser = { uid -> vm.unblockUser(uid) },
      navigationActions = navigationActions,
      isAdmin = isAdmin,
      onAdminClick = onAdminClick)
}

private val previewUiState =
    SettingsUiState(
        userName = "John Doe",
        email = "john.doe@email.com",
        errorMsg = null,
        topItems = emptyList(),
        accountItems = emptyList())

/**
 * Content composable for the Settings screen UI.
 *
 * This is the actual implementation of the settings screen UI, separated from the state management
 * logic in [SettingsScreen]. It displays all settings sections, handles user interactions, and
 * manages local UI state (toggles, expanded states).
 *
 * @param ui The [SettingsUiState] containing user data and blocked contacts.
 * @param onItemClick Callback invoked when a settings item is clicked.
 * @param onProfileClick Callback invoked when the profile button is clicked.
 * @param onDeleteAccount Callback invoked when the delete account button is confirmed.
 * @param onContributionClick Callback invoked when the contributions button is clicked.
 * @param onUnblockUser Callback invoked when a blocked user is unblocked.
 * @param navigationActions Optional [NavigationActions] for bottom bar navigation.
 * @param isAdmin Whether to display the Admin section.
 * @param onAdminClick Callback invoked when the admin page button is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreenContent(
    ui: SettingsUiState,
    onItemClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onContributionClick: () -> Unit = {},
    onUnblockUser: (String) -> Unit = {},
    navigationActions: NavigationActions? = null,
    isAdmin: Boolean = false,
    onAdminClick: () -> Unit = {}
) {
  // Independent toggle states
  var notificationsMessages by remember { mutableStateOf(true) }
  var notificationsListings by remember { mutableStateOf(false) }
  var readReceipts by remember { mutableStateOf(true) }
  var anonymous by remember { mutableStateOf(false) }

  // Dark mode preference - connected to theme
  val (darkModePreference, setDarkModePreference) = rememberDarkModePreference()
  val nightShift = darkModePreference ?: isSystemInDarkTheme()

  var blockedExpanded by remember { mutableStateOf(false) }
  val blockedContacts = ui.blockedContacts
  val focusManager = LocalFocusManager.current
  var showDeleteConfirm by remember { mutableStateOf(false) }

  Scaffold(
      containerColor = BackGroundColor,
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.settings_title)) },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackGroundColor, titleContentColor = TextColor))
      },
      bottomBar = {
        if (navigationActions != null) {
          BottomBarFromNav(navigationActions)
        }
      }) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(BackGroundColor)) {
              val maxW = this.maxWidth
              val isCompact = maxW < 360.dp
              val isTablet = maxW >= 600.dp

              val horizontalPad: Dp =
                  when {
                    isTablet -> 24.dp
                    isCompact -> 12.dp
                    else -> 16.dp
                  }
              val contentWidthCap = if (isTablet) 600.dp else maxW

              LazyColumn(
                  modifier = Modifier.fillMaxSize().testTag(C.SettingsTags.SETTINGS_SCROLL),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  contentPadding =
                      PaddingValues(
                          start = horizontalPad,
                          end = horizontalPad,
                          top = 12.dp,
                          bottom = 24.dp)) {
                    item {
                      Column(modifier = Modifier.fillMaxWidth().widthIn(max = contentWidthCap)) {

                        // ---- Profile card ----------------------------------------------------
                        CardBlock {
                          Row(
                              modifier = Modifier.fillMaxWidth(),
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                val avatarSize = if (isTablet) 64.dp else 56.dp
                                val initial =
                                    (ui.userName.firstOrNull()?.uppercaseChar() ?: 'A').toString()

                                // Avatar background = PalePink, hardcoded as requested
                                Box(
                                    modifier =
                                        Modifier.size(avatarSize)
                                            .clip(CircleShape)
                                            .background(PalePink.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center) {
                                      Text(initial, fontWeight = FontWeight.Bold, color = MainColor)
                                    }

                                Column(modifier = Modifier.weight(1f)) {
                                  Text(
                                      ui.userName.ifBlank { stringResource(R.string.user) },
                                      style = MaterialTheme.typography.titleMedium,
                                      color = TextColor,
                                      maxLines = 1,
                                      overflow = TextOverflow.Ellipsis)
                                  Text(
                                      stringResource(R.string.settings_view_profile),
                                      style = MaterialTheme.typography.bodySmall,
                                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                                      maxLines = 1,
                                      overflow = TextOverflow.Ellipsis)
                                }

                                IconButton(
                                    onClick = onProfileClick,
                                    modifier = Modifier.testTag(C.SettingsTags.PROFILE_BUTTON)) {
                                      Icon(
                                          imageVector = Icons.Filled.ChevronRight,
                                          contentDescription = "Open profile",
                                          tint = TextColor)
                                    }
                              }
                        }

                        // ---- Notifications ---------------------------------------------------
                        SectionLabel(stringResource(R.string.notifications))
                        CardBlock {
                          SettingSwitchRow(
                              label = stringResource(R.string.settings_notifications_messages),
                              checked = notificationsMessages,
                              onCheckedChange = { notificationsMessages = it })
                          SoftDivider()
                          SettingSwitchRow(
                              label = stringResource(R.string.settings_notifications_listings),
                              checked = notificationsListings,
                              onCheckedChange = { notificationsListings = it })
                        }

                        // ---- Account ---------------------------------------------------------
                        SectionLabel(stringResource(R.string.account))
                        CardBlock {
                          OutlinedTextField(
                              value = ui.email,
                              onValueChange = {},
                              label = { Text(stringResource(R.string.email_address)) },
                              singleLine = true,
                              readOnly = true,
                              enabled = false,
                              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                              keyboardActions =
                                  KeyboardActions(onDone = { focusManager.clearFocus() }),
                              colors =
                                  OutlinedTextFieldDefaults.colors(
                                      disabledTextColor = TextColor.copy(alpha = 0.9f),
                                      disabledBorderColor = TextBoxColor,
                                      disabledLabelColor =
                                          MaterialTheme.colorScheme.onSurfaceVariant,
                                      disabledContainerColor = MaterialTheme.colorScheme.surface),
                              modifier =
                                  Modifier.fillMaxWidth().testTag(C.SettingsTags.EMAIL_FIELD))
                          Spacer(Modifier.height(12.dp))
                          Button(
                              onClick = onContributionClick,
                              modifier =
                                  Modifier.fillMaxWidth()
                                      .testTag(C.SettingsTags.CONTRIBUTIONS_BUTTON),
                              shape = RoundedCornerShape(16.dp),
                              colors =
                                  ButtonDefaults.buttonColors(
                                      containerColor = MainColor, contentColor = White)) {
                                Text(stringResource(R.string.settings_view_contributions))
                              }
                        }

                        // ---- Privacy ---------------------------------------------------------
                        SectionLabel(stringResource(R.string.privacy))
                        CardBlock {
                          SettingSwitchRow(
                              label = stringResource(R.string.settings_read_receipts),
                              checked = readReceipts,
                              onCheckedChange = { readReceipts = it })
                          SoftDivider()

                          Row(
                              modifier =
                                  Modifier.fillMaxWidth()
                                      .padding(horizontal = 4.dp, vertical = 10.dp),
                              verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${stringResource(R.string.settings_blocked_contacts)} (${blockedContacts.size})",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f))
                                val rotation by
                                    animateFloatAsState(
                                        targetValue = if (blockedExpanded) 90f else 0f,
                                        label = "blockedArrowRotation")
                                IconButton(
                                    onClick = { blockedExpanded = !blockedExpanded },
                                    modifier =
                                        Modifier.testTag(C.SettingsTags.BLOCKED_CONTACTS_TOGGLE)) {
                                      Icon(
                                          imageVector =
                                              Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                          contentDescription =
                                              if (blockedExpanded)
                                                  stringResource(R.string.settings_hide_blocked)
                                              else stringResource(R.string.settings_show_blocked),
                                          modifier = Modifier.rotate(rotation),
                                          tint = TextColor)
                                    }
                              }

                          val blockedBringIntoView = remember { BringIntoViewRequester() }
                          LaunchedEffect(blockedExpanded) {
                            if (blockedExpanded) blockedBringIntoView.bringIntoView()
                          }

                          if (blockedExpanded) {
                            Surface(
                                color = BackGroundColor,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, TextBoxColor),
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .bringIntoViewRequester(blockedBringIntoView)
                                        .testTag(C.SettingsTags.BLOCKED_CONTACTS_LIST)) {
                                  Column(Modifier.padding(12.dp)) {
                                    if (blockedContacts.isEmpty()) {
                                      Text(
                                          text =
                                              stringResource(R.string.settings_no_blocked_contacts),
                                          style = MaterialTheme.typography.bodyMedium,
                                          color = TextColor.copy(alpha = 0.6f),
                                          modifier = Modifier.padding(vertical = 4.dp))
                                    } else {
                                      blockedContacts.forEach { contact ->
                                        Row(
                                            modifier =
                                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically) {
                                              Text(
                                                  text = contact.displayName,
                                                  style = MaterialTheme.typography.bodyMedium,
                                                  color = TextColor,
                                                  maxLines = 1,
                                                  overflow = TextOverflow.Ellipsis,
                                                  modifier =
                                                      Modifier.weight(1f).padding(end = 12.dp))
                                              TextButton(
                                                  onClick = { onUnblockUser(contact.uid) },
                                                  colors =
                                                      ButtonDefaults.textButtonColors(
                                                          contentColor = MainColor)) {
                                                    Text(stringResource(R.string.unblock))
                                                  }
                                            }
                                      }
                                    }
                                  }
                                }
                          }
                        }

                        // ---- Accessibility ---------------------------------------------------
                        SectionLabel(stringResource(R.string.accessibility))
                        CardBlock {
                          SettingSwitchRow(
                              label = stringResource(R.string.settings_dark_mode),
                              checked = nightShift,
                              onCheckedChange = { enabled -> setDarkModePreference(enabled) })
                          SoftDivider()
                          SettingSwitchRow(
                              label = stringResource(R.string.anonymous),
                              checked = anonymous,
                              onCheckedChange = { anonymous = it })
                        }

                        // ---- Admin ------------------------------------------------------------
                        if (isAdmin) {
                          SectionLabel(stringResource(R.string.admin))
                          CardBlock {
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable(onClick = onAdminClick),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                  Text(
                                      stringResource(R.string.settings_admin_page),
                                      style = MaterialTheme.typography.bodyLarge,
                                      color = TextColor,
                                      maxLines = 2,
                                      overflow = TextOverflow.Ellipsis,
                                      modifier = Modifier.weight(1f).padding(end = 12.dp))
                                  Icon(
                                      imageVector = Icons.Filled.ChevronRight,
                                      contentDescription = "Open admin page",
                                      tint = TextColor)
                                }
                          }
                        }
                      }
                    }

                    item {
                      Box(
                          modifier =
                              Modifier.fillMaxWidth()
                                  .widthIn(max = if (maxW >= 600.dp) 600.dp else maxW)) {
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
                                border = BorderStroke(1.dp, MainColor.copy(alpha = 0.15f)),
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .testTag(C.SettingsTags.DELETE_ACCOUNT_BUTTON)
                                        .navigationBarsPadding()) {
                                  Text(
                                      if (ui.isDeleting) stringResource(R.string.settings_deleting)
                                      else stringResource(R.string.settings_delete))
                                }
                          }
                    }
                  }
            }
      }

  if (showDeleteConfirm) {
    AlertDialog(
        onDismissRequest = { showDeleteConfirm = false },
        title = { Text(stringResource(R.string.settings_delete_dialog_title)) },
        text = { Text(stringResource(R.string.settings_delete_dialog_text)) },
        confirmButton = {
          TextButton(
              onClick = {
                showDeleteConfirm = false
                onDeleteAccount()
              }) {
                Text(stringResource(R.string.delete))
              }
        },
        dismissButton = {
          TextButton(onClick = { showDeleteConfirm = false }) {
            Text(stringResource(R.string.cancel))
          }
        })
  }
}

// ---------- Helpers ----------

@Composable
private fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
  Surface(
      color = BackGroundColor,
      shape = MaterialTheme.shapes.large,
      border = BorderStroke(1.dp, TextBoxColor),
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

/** Switch row with white thumb and theme-aware colors */
@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  val screenWidthDp = LocalConfiguration.current.screenWidthDp
  val isExtraNarrow = screenWidthDp < 340
  val switchColors =
      SwitchDefaults.colors(
          checkedThumbColor = White,
          checkedTrackColor = MainColor,
          uncheckedThumbColor = White,
          uncheckedTrackColor = TextBoxColor.copy(alpha = 0.6f))

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
                  Modifier.testTag(C.SettingsTags.switch(label)).semantics(
                      mergeDescendants = true) {
                        role = Role.Switch
                        stateDescription = if (checked) "On" else "Off"
                      },
              checked = checked,
              onCheckedChange = onCheckedChange,
              colors = switchColors)
        }
  } else {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
      Text(
          label,
          style = MaterialTheme.typography.bodyLarge,
          color = TextColor,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.fillMaxWidth())
      Row(
          modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
          horizontalArrangement = Arrangement.End) {
            Switch(
                modifier =
                    Modifier.testTag(C.SettingsTags.switch(label)).semantics(
                        mergeDescendants = true) {
                          role = Role.Switch
                          stateDescription = if (checked) "On" else "Off"
                        },
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = switchColors)
          }
    }
  }
}

// ---------- Previews ----------

@Preview(
    name = "Settings – Light Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    widthDp = 360)
@Composable
private fun SettingsPreview_Light() {
  MySwissDormAppTheme { SettingsScreenContent(ui = previewUiState) }
}

@Preview(
    name = "Settings – Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 360)
@Composable
private fun SettingsPreview_Dark() {
  MySwissDormAppTheme { SettingsScreenContent(ui = previewUiState) }
}
