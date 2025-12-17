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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.navigation.NavigationActions
import com.android.mySwissDorm.ui.navigation.Screen
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.Dimens
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
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
 *   screen.
 *
 * @param navigationActions Optional [NavigationActions] for bottom bar navigation. If null, bottom
 *   bar is hidden.
 * @param vm The [SettingsViewModel] that manages the settings state and user data. Defaults to a
 *   new instance created via [viewModel].
 * @param isAdmin Whether the current user has admin access. If true, displays Admin section.
 * @param onAdminClick Callback invoked when the admin page button is clicked.
 * @see SettingsViewModel for state management and user data handling
 * @see SettingsScreenContent for the actual UI implementation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    navigationActions: NavigationActions? = null,
    vm: SettingsViewModel = viewModel(),
    isAdmin: Boolean = false,
    onAdminClick: () -> Unit = {},
) {
  val ui by vm.uiState.collectAsState()
  vm.setIsGuest()
  LaunchedEffect(Unit) { vm.refresh() }

  val context = LocalContext.current
  LaunchedEffect(ui.errorMsg) { //
    ui.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      vm.clearError()
    }
  }

  SettingsScreenContent(
      ui = ui,
      onBack = onBack,
      onDeleteAccount = {
        vm.deleteAccount(
            { success, _ ->
              if (success) {
                navigationActions?.navigateTo(Screen.SignIn)
              }
            },
            context)
      },
      onUnblockUser = { uid -> vm.unblockUser(uid, context) },
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
 * @param onDeleteAccount Callback invoked when the delete account button is confirmed.
 * @param onUnblockUser Callback invoked when a blocked user is unblocked.
 * @param navigationActions Optional [NavigationActions] for bottom bar navigation.
 * @param isAdmin Whether to display the Admin section.
 * @param onAdminClick Callback invoked when the admin page button is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreenContent(
    ui: SettingsUiState,
    onBack: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onUnblockUser: (String) -> Unit = {},
    navigationActions: NavigationActions? = null,
    isAdmin: Boolean = false,
    onAdminClick: () -> Unit = {},
) {
  // Independent toggle states
  var notificationsMessages by remember { mutableStateOf(true) }
  var notificationsListings by remember { mutableStateOf(false) }
  var readReceipts by remember { mutableStateOf(true) }

  // Dark mode preference - connected to theme
  val (darkModePreference, setDarkModePreference) = rememberDarkModePreference()
  val nightShift = darkModePreference ?: isSystemInDarkTheme()

  var blockedExpanded by remember { mutableStateOf(false) }
  val blockedContacts = ui.blockedContacts
  var showDeleteConfirm by remember { mutableStateOf(false) }

  Scaffold(
      containerColor = BackGroundColor,
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.settings_title)) },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackGroundColor, titleContentColor = TextColor),
            navigationIcon = {
              IconButton(
                  onClick = onBack, modifier = Modifier.testTag(C.SettingsTags.BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                        tint = MainColor)
                  }
            })
      }) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(BackGroundColor)) {
              val maxW = this.maxWidth
              val isCompact = maxW < 360.dp
              val isTablet = maxW >= 600.dp

              val horizontalPad: Dp =
                  when {
                    isTablet -> Dimens.PaddingLarge
                    isCompact -> Dimens.PaddingMedium
                    else -> Dimens.PaddingDefault
                  }
              val contentWidthCap = if (isTablet) 600.dp else maxW

              LazyColumn(
                  modifier = Modifier.fillMaxSize().testTag(C.SettingsTags.SETTINGS_SCROLL),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  contentPadding =
                      PaddingValues(
                          start = horizontalPad,
                          end = horizontalPad,
                          top = Dimens.PaddingMedium,
                          bottom = Dimens.PaddingLarge)) {
                    item {
                      Column(modifier = Modifier.fillMaxWidth().widthIn(max = contentWidthCap)) {
                        // ---- Notifications ---------------------------------------------------
                        SectionLabel(stringResource(R.string.notifications))
                        CardBlock {
                          SettingSwitchRow(
                              label = stringResource(R.string.settings_notifications_messages),
                              checked = if (ui.isGuest) false else notificationsMessages,
                              onCheckedChange = { notificationsMessages = it })
                          SoftDivider()
                          SettingSwitchRow(
                              label = stringResource(R.string.settings_notifications_listings),
                              checked = if (ui.isGuest) false else notificationsListings,
                              onCheckedChange = { notificationsListings = it })
                        }

                        // ---- Privacy ---------------------------------------------------------
                        SectionLabel(stringResource(R.string.privacy))
                        CardBlock {
                          SettingSwitchRow(
                              label = stringResource(R.string.settings_read_receipts),
                              checked = if (ui.isGuest) true else readReceipts,
                              onCheckedChange = { readReceipts = it })
                          SoftDivider()

                          Row(
                              modifier =
                                  Modifier.fillMaxWidth()
                                      .padding(
                                          horizontal = Dimens.PaddingXSmall,
                                          vertical = Dimens.PaddingTopSmall),
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
                                  Column(Modifier.padding(Dimens.PaddingMedium)) {
                                    if (blockedContacts.isEmpty()) {
                                      Text(
                                          text =
                                              stringResource(R.string.settings_no_blocked_contacts),
                                          style = MaterialTheme.typography.bodyMedium,
                                          color = TextColor.copy(alpha = Dimens.AlphaSecondary),
                                          modifier =
                                              Modifier.padding(vertical = Dimens.PaddingXSmall))
                                    } else {
                                      blockedContacts.forEach { contact ->
                                        Row(
                                            modifier =
                                                Modifier.fillMaxWidth()
                                                    .padding(vertical = Dimens.PaddingXSmall),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically) {
                                              Text(
                                                  text = contact.displayName,
                                                  style = MaterialTheme.typography.bodyMedium,
                                                  color = TextColor,
                                                  maxLines = 1,
                                                  overflow = TextOverflow.Ellipsis,
                                                  modifier =
                                                      Modifier.weight(1f)
                                                          .padding(end = Dimens.PaddingMedium))
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
                        }

                        // ---- Admin ------------------------------------------------------------
                        if (isAdmin) {
                          SectionLabel(stringResource(R.string.admin))
                          CardBlock {
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(vertical = Dimens.PaddingXSmall)
                                        .clickable(onClick = onAdminClick),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                  Text(
                                      stringResource(R.string.settings_admin_page),
                                      style = MaterialTheme.typography.bodyLarge,
                                      color = TextColor,
                                      maxLines = 2,
                                      overflow = TextOverflow.Ellipsis,
                                      modifier =
                                          Modifier.weight(1f).padding(end = Dimens.PaddingMedium))
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
                            if (ui.isGuest) {
                              Button(
                                  onClick = { navigationActions?.navigateTo(Screen.SignIn) },
                                  enabled = true,
                                  colors =
                                      ButtonDefaults.buttonColors(
                                          containerColor = MainColor,
                                          contentColor = White,
                                          disabledContainerColor =
                                              MainColor.copy(alpha = Dimens.AlphaMedium),
                                          disabledContentColor = White),
                                  shape = RoundedCornerShape(Dimens.IconSizeXLarge),
                                  elevation =
                                      ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                  modifier =
                                      Modifier.fillMaxWidth()
                                          .padding(top = Dimens.PaddingSmall)
                                          .testTag(C.SettingsTags.DELETE_ACCOUNT_BUTTON)
                                          .navigationBarsPadding()) {
                                    Text(
                                        stringResource(R.string.settings_sign_up_to_create_account))
                                  }
                            } else {
                              Button(
                                  onClick = { showDeleteConfirm = true },
                                  enabled = !ui.isDeleting,
                                  colors =
                                      ButtonDefaults.buttonColors(
                                          containerColor = BackGroundColor,
                                          contentColor = MainColor,
                                          disabledContainerColor = BackGroundColor,
                                          disabledContentColor =
                                              MainColor.copy(alpha = Dimens.AlphaMedium)),
                                  shape = RoundedCornerShape(Dimens.IconSizeXLarge),
                                  elevation =
                                      ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                  border =
                                      BorderStroke(1.dp, MainColor.copy(alpha = Dimens.AlphaLow)),
                                  modifier =
                                      Modifier.fillMaxWidth()
                                          .padding(top = Dimens.PaddingSmall)
                                          .testTag(C.SettingsTags.DELETE_ACCOUNT_BUTTON)
                                          .navigationBarsPadding()) {
                                    Text(
                                        if (ui.isDeleting)
                                            stringResource(R.string.settings_deleting)
                                        else stringResource(R.string.settings_delete))
                                  }
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
        containerColor = BackGroundColor,
        titleContentColor = TextColor,
        textContentColor = TextColor,
        confirmButton = {
          TextButton(
              onClick = {
                showDeleteConfirm = false
                onDeleteAccount()
              },
              colors = ButtonDefaults.textButtonColors(contentColor = MainColor)) {
                Text(stringResource(R.string.delete))
              }
        },
        dismissButton = {
          TextButton(
              onClick = { showDeleteConfirm = false },
              colors = ButtonDefaults.textButtonColors(contentColor = TextColor)) {
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
        Column(Modifier.padding(Dimens.PaddingDefault), content = content)
      }
}

@Composable
private fun SectionLabel(text: String) {
  Text(
      text = text,
      style = MaterialTheme.typography.titleMedium,
      color = TextColor,
      modifier = Modifier.padding(start = Dimens.PaddingXSmall, bottom = Dimens.PaddingXSmall))
}

@Composable
private fun SoftDivider() {
  HorizontalDivider(thickness = 1.dp, color = TextBoxColor.copy(alpha = Dimens.AlphaLow))
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
          uncheckedTrackColor = TextBoxColor.copy(alpha = Dimens.AlphaSecondary))

  if (!isExtraNarrow) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.PaddingXSmall),
        verticalAlignment = Alignment.CenterVertically) {
          Text(
              label,
              style = MaterialTheme.typography.bodyLarge,
              color = TextColor,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f).padding(end = Dimens.PaddingMedium))
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
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.PaddingXSmall)) {
      Text(
          label,
          style = MaterialTheme.typography.bodyLarge,
          color = TextColor,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.fillMaxWidth())
      Row(
          modifier = Modifier.fillMaxWidth().padding(top = Dimens.PaddingMedium),
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
