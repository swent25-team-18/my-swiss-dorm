package com.android.mySwissDorm.ui.profile

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.profile.Language
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.AddPhotoDialog
import com.android.mySwissDorm.ui.navigation.BottomBarFromNav
import com.android.mySwissDorm.ui.navigation.NavigationActions
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.Dimens
import com.android.mySwissDorm.ui.theme.Gray
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.ui.theme.Red0
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.theme.White

/**
 * High-level Profile screen entry point.
 * - Collects [ProfileUiState] from [ProfileScreenViewModel].
 * - Delegates rendering to [ProfileScreenContent].
 * - Exposes callbacks for logout, profile picture change, and navigation back.
 *
 * Test tags used in this file (stable for tests):
 * - "profile_title", "profile_back_button", "profile_edit_toggle", "profile_list"
 * - "profile_picture_box", "profile_logout_button", "profile_save_button"
 * - "field_first_name", "field_last_name", "field_university", "field_language", "field_residence"
 */
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onSettingsClicked: () -> Unit,
    onEditPreferencesClick: () -> Unit,
    onContributionClick: () -> Unit,
    onViewBookmarks: () -> Unit,
    navigationActions: NavigationActions? = null,
    viewModel: ProfileScreenViewModel = viewModel()
) {
  // Collect VM state (initial ensures preview/first composition has data)
  val state by viewModel.uiState.collectAsState(initial = ProfileUiState())
  val context = LocalContext.current
  var restartPopUpIsDisplayed by remember { mutableStateOf(false) }
  var newLanguage by remember { mutableStateOf(state.language) }

  LaunchedEffect(Unit) { viewModel.loadProfile(context) }

  ProfileScreenContent(
      state = state,
      onFirstNameChange = viewModel::onFirstNameChange,
      onLastNameChange = viewModel::onLastNameChange,
      onUniversityChange = viewModel::onUniversityChange,
      onLanguageChange = {
        if (state.language != it) {
          newLanguage = it
          restartPopUpIsDisplayed = true
        }
      },
      onResidenceChange = viewModel::onResidenceChange,
      onLogout = onLogout,
      onChangeProfilePicture = { viewModel.onProfilePictureChange(it) },
      onSettingsClicked = onSettingsClicked,
      onToggleEditing = viewModel::toggleEditing,
      onSave = { viewModel.saveProfile(context) },
      onEditPreferencesClick = onEditPreferencesClick,
      onContributionClick = onContributionClick,
      onViewBookmarks = onViewBookmarks,
      navigationActions = navigationActions)

  if (restartPopUpIsDisplayed && !state.isSaving) {
    RestartDialog(
        onDismissRequest = { restartPopUpIsDisplayed = false },
        onRestart = {
          viewModel.onLanguageChange(newLanguage)
          val langCode =
              when (newLanguage) {
                Language.ENGLISH.displayLanguage -> Language.ENGLISH.codeLanguage
                Language.FRENCH.displayLanguage -> Language.FRENCH.codeLanguage
                else -> Language.ENGLISH.codeLanguage
              }
          viewModel.saveProfile(context, { onLanguageChange(langCode) })
        })
  }
}

/**
 * Pure UI for the profile screen.
 *
 * ## Modes
 * - **View mode** (`state.isEditing == false`): fields are disabled/read-only; "Logout" button is
 *   shown.
 * - **Edit mode** (`state.isEditing == true`): fields are enabled; "Save" button is shown; avatar
 *   is clickable.
 *
 * ## Local edit buffers
 * Uses local `rememberSaveable` buffers for text fields during edit mode to ensure smooth typing,
 * then pushes values to VM via `on*Change` just before calling `onSave()`.
 *
 * @param state Immutable UI state from the VM.
 * @param onFirstNameChange Pushes final first name to VM on save.
 * @param onLastNameChange Pushes final last name to VM on save.
 * @param onUniversityChange Pushes final university to VM on save.
 * @param onLanguageChange Pushes final language to VM on save.
 * @param onResidenceChange Pushes final residence to VM on save.
 * @param onLogout Invoked in view mode by the "LOGOUT" button.
 * @param onChangeProfilePicture Invoked when avatar is tapped in edit mode.
 * @param onSettingsClicked Invoked by top app bar button.
 * @param onToggleEditing Toggles between view and edit mode.
 * @param onSave Persists profile (Firestore) via the VM; also exits edit mode on success.
 * @param onEditPreferencesClick Invoked by the "Preferences" button.
 * @param onContributionClick Invoked by the "My contributions" button.
 * @param onViewBookmarks Invoked by the "View bookmarks" button.
 * @param navigationActions Optional [NavigationActions] for bottom bar navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreenContent(
    state: ProfileUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onUniversityChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onResidenceChange: (String) -> Unit,
    onLogout: () -> Unit,
    onChangeProfilePicture: (Photo?) -> Unit,
    onSettingsClicked: () -> Unit,
    onToggleEditing: () -> Unit,
    onSave: () -> Unit,
    onEditPreferencesClick: () -> Unit,
    onContributionClick: () -> Unit,
    onViewBookmarks: () -> Unit,
    navigationActions: NavigationActions? = null,
) {
  val focusManager = LocalFocusManager.current

  // Local editable buffers used ONLY in edit mode (remembered across recompositions)
  var firstLocal by rememberSaveable(state.isEditing) { mutableStateOf(state.firstName) }
  var lastLocal by rememberSaveable(state.isEditing) { mutableStateOf(state.lastName) }
  var universityLocal by rememberSaveable(state.isEditing) { mutableStateOf(state.university) }
  var languageLocal by rememberSaveable(state.isEditing) { mutableStateOf(state.language) }
  var residenceLocal by rememberSaveable(state.isEditing) { mutableStateOf(state.residence) }
  var displayPhotoDialog by rememberSaveable(state.isEditing) { mutableStateOf(false) }

  LaunchedEffect(Unit) {}

  // When entering edit mode, snapshot current VM values into locals to start from latest persisted
  // values
  LaunchedEffect(state.isEditing) {
    if (state.isEditing) {
      firstLocal = state.firstName
      lastLocal = state.lastName
      universityLocal = state.university
      languageLocal = state.language
      residenceLocal = state.residence
      displayPhotoDialog = false
    }
  }

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Text(
                  text = stringResource(R.string.profile_title),
                  fontSize = 24.sp,
                  color = TextColor,
                  modifier = Modifier.testTag(C.Tag.PROFILE_SCREEN_TITLE))
            },
            navigationIcon = {
              IconButton(
                  onClick = onSettingsClicked,
                  modifier = Modifier.testTag(C.ProfileTags.SETTINGS_ICON)) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MainColor)
                  }
            },
            actions = {
              // Toggle between view/edit (Edit icon ↔ Close icon)
              IconButton(
                  onClick = onToggleEditing, modifier = Modifier.testTag("profile_edit_toggle")) {
                    if (state.isEditing) {
                      Icon(
                          Icons.Default.Close,
                          contentDescription = "Cancel editing",
                          tint = MainColor)
                    } else {
                      Icon(
                          Icons.Default.Edit,
                          contentDescription = "Modify profile",
                          tint = MainColor)
                    }
                  }
            })
      },
      bottomBar = {
        if (navigationActions != null) {
          BottomBarFromNav(navigationActions)
        }
      }) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .padding(Dimens.PaddingDefault)
                    .verticalScroll(rememberScrollState())
                    .testTag("profile_list"),
            horizontalAlignment = Alignment.CenterHorizontally) {

              // Profile Picture (clickable only in edit mode; remains disabled in view mode but
              // keeps click semantics)
              Box(modifier = Modifier.height(100.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier =
                        Modifier.size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, MainColor, CircleShape)
                            .background(BackGroundColor)
                            .clickable(enabled = state.isEditing) { displayPhotoDialog = true }
                            .testTag("profile_picture_box"),
                    contentAlignment = Alignment.Center) {
                      if (state.profilePicture != null) {
                        AsyncImage(
                            model = state.profilePicture.image,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier.testTag(
                                    C.ProfileTags.profilePictureTag(
                                        uri = state.profilePicture.image)),
                            onSuccess = {
                              Log.d(
                                  "ProfileScreen",
                                  "Photo ${state.profilePicture.fileName} loaded successfully")
                            },
                            onLoading = {
                              Log.d(
                                  "ProfileScreen", "Photo ${state.profilePicture.fileName} loading")
                            })
                      } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier =
                                Modifier.size(Dimens.IconSizeButton)
                                    .testTag(C.ProfileTags.profilePictureTag(null)),
                            tint = MainColor)
                      }
                    }
                if (state.isEditing && state.profilePicture != null) {
                  FloatingActionButton(
                      onClick = { onChangeProfilePicture(null) },
                      modifier =
                          Modifier.size(Dimens.IconSizeXXXLarge)
                              .align(Alignment.TopEnd)
                              .offset(x = Dimens.SpacingMedium, y = -Dimens.SpacingMedium),
                      containerColor = MaterialTheme.colorScheme.primary,
                      contentColor = Red0) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier =
                                Modifier.size(Dimens.IconSizeDefault)
                                    .testTag(C.ProfileTags.DELETE_PP_BUTTON),
                            tint = Red0)
                      }
                }
              }

              // Name row: First name | Last name (equal widths via weight)
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)) {
                    EditableTextField(
                        label = stringResource(R.string.first_name),
                        value = if (state.isEditing) firstLocal else state.firstName,
                        onValueChange = {
                          if (it.length <= 20) firstLocal = it
                        }, // ← cap at 20 chars
                        tag = "field_first_name",
                        enabled = state.isEditing,
                        modifier = Modifier.weight(1f))

                    EditableTextField(
                        label = stringResource(R.string.last_name),
                        value = if (state.isEditing) lastLocal else state.lastName,
                        onValueChange = {
                          if (it.length <= 20) lastLocal = it
                        }, // ← cap at 20 chars
                        tag = "field_last_name",
                        enabled = state.isEditing,
                        modifier = Modifier.weight(1f))
                  }

              DropdownField(
                  label = stringResource(R.string.university),
                  value = if (state.isEditing) universityLocal else state.university,
                  onValueChange = { universityLocal = it },
                  tag = "field_university",
                  enabled = state.isEditing,
                  modifier = Modifier.fillMaxWidth(),
                  options = state.allUniversities.map { it.name })

              // Language dropdown (from Language enum)
              DropdownField(
                  label = stringResource(R.string.language),
                  value = if (state.isEditing) languageLocal else state.language,
                  onValueChange = { languageLocal = it },
                  tag = "field_language",
                  enabled = state.isEditing,
                  modifier = Modifier.fillMaxWidth(),
                  options = remember { Language.values().toList() })

              // Residence dropdown (from ResidencyName enum)
              DropdownField(
                  label = stringResource(R.string.residency),
                  value = if (state.isEditing) residenceLocal else state.residence,
                  onValueChange = {
                    residenceLocal = it
                  }, // receives the selected enum's display string
                  enabled = state.isEditing,
                  modifier = Modifier.fillMaxWidth(),
                  tag = "field_residence",
                  options = state.allResidencies.map { it.name })
              // Bottom action area: Save (edit mode) or Logout (view mode)
              if (state.isEditing) {
                Button(
                    onClick = {
                      // Push local edits to VM, then persist
                      onFirstNameChange(firstLocal)
                      onLastNameChange(lastLocal)
                      onUniversityChange(universityLocal)
                      onLanguageChange(languageLocal)
                      onResidenceChange(residenceLocal)
                      onSave()
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(top = Dimens.SpacingXLarge)
                            .height(Dimens.ButtonHeight)
                            .clip(RoundedCornerShape(Dimens.PaddingMedium))
                            .testTag(C.ProfileTags.SAVE_BUTTON),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MainColor, contentColor = BackGroundColor),
                    enabled = !state.isSaving) {
                      Text(
                          text =
                              if (state.isSaving) stringResource(R.string.profile_saving)
                              else stringResource(R.string.profile_save))
                    }
              }

              // Optional inline error message (e.g., Firestore write failure or auth issue)
              if (state.errorMsg != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("profile_error_text"))
              }

              if (!state.isEditing) {
                Spacer(Modifier.height(12.dp))

                // ---- Account ---------------------------------------------------------
                SectionLabel(stringResource(R.string.account))
                CardBlock(modifier = Modifier.testTag(C.ProfileTags.ACCOUNT_CARD)) {
                  OutlinedTextField(
                      value = state.email,
                      onValueChange = {},
                      label = { Text(stringResource(R.string.email_address)) },
                      singleLine = true,
                      readOnly = true,
                      enabled = false,
                      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                      keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                      colors =
                          OutlinedTextFieldDefaults.colors(
                              disabledTextColor = TextColor.copy(alpha = 0.9f),
                              disabledBorderColor = TextBoxColor,
                              disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                              disabledContainerColor = MaterialTheme.colorScheme.surface),
                      modifier = Modifier.fillMaxWidth().testTag(C.ProfileTags.EMAIL_FIELD))
                  Spacer(Modifier.height(12.dp))
                  Button(
                      onClick = onContributionClick,
                      modifier =
                          Modifier.fillMaxWidth().testTag(C.ProfileTags.CONTRIBUTIONS_BUTTON),
                      shape = RoundedCornerShape(16.dp),
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = MainColor, contentColor = White)) {
                        Text(stringResource(R.string.settings_view_contributions))
                      }
                  Spacer(Modifier.height(12.dp))
                  Button(
                      onClick = onViewBookmarks,
                      modifier = Modifier.fillMaxWidth().testTag(C.ProfileTags.BOOKMARKS_BUTTON),
                      shape = RoundedCornerShape(16.dp),
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = MainColor, contentColor = White)) {
                        Text(stringResource(R.string.profile_view_bookmarks))
                      }
                  Spacer(Modifier.height(12.dp))
                  Button(
                      onClick = onEditPreferencesClick,
                      modifier = Modifier.fillMaxWidth().testTag(C.ProfileTags.PREFERENCES_BUTTON),
                      shape = RoundedCornerShape(16.dp),
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = MainColor, contentColor = White)) {
                        Text(stringResource(R.string.listing_preferences))
                      }
                }

                // Logout button
                Button(
                    onClick = onLogout,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(top = Dimens.SpacingXLarge)
                            .height(Dimens.ButtonHeight)
                            .clip(RoundedCornerShape(Dimens.PaddingMedium))
                            .testTag("profile_logout_button"),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = BackGroundColor, contentColor = MainColor)) {
                      Text(text = stringResource(R.string.profile_logout), color = MainColor)
                    }
              }
            }
        if (displayPhotoDialog) {
          AddPhotoDialog(
              onSelectPhoto = {
                displayPhotoDialog = false
                onChangeProfilePicture(it)
              },
              onDismissRequest = { displayPhotoDialog = false })
        }
      }
}

/**
 * Reusable outlined text field with fixed visual style to match the screen.
 * - Honors [enabled] to allow/disable editing.
 * - Uses [modifier] to size/layout (e.g., weight or fillMaxWidth).
 * - Tagged with [tag] for UI tests.
 */
@Composable
fun EditableTextField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    tag: String,
    enabled: Boolean = true,
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange, // never gate this (enabled already blocks edits)
      enabled = enabled,
      singleLine = true,
      label = { Text(text = label, color = Gray) },
      modifier = modifier.height(Dimens.ImageSizeSmall).testTag(tag),
      placeholder = { Text(text = label) },
      shape = RoundedCornerShape(Dimens.PaddingMedium),
      colors =
          TextFieldDefaults.colors(
              unfocusedIndicatorColor = Gray,
              focusedIndicatorColor = MainColor,
              focusedLabelColor = MainColor,
              cursorColor = TextColor,
              focusedContainerColor = BackGroundColor,
              unfocusedContainerColor = BackGroundColor,
              disabledContainerColor = BackGroundColor),
      textStyle = TextStyle(color = TextColor, fontSize = 16.sp, textAlign = TextAlign.Start),
      keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done))
}

/**
 * Generic exposed dropdown using an [OutlinedTextField] as the anchor, styled to visually match
 * other inputs (rounded corners, 64dp height).
 *
 * The [options] list is rendered via `item.toString()` which is perfect for enums like [Language]
 * and [ResidencyName] where `toString()` returns the display label.
 *
 * Behavior:
 * - Read-only input opens a menu when enabled.
 * - Selecting an item calls [onValueChange] with the item's display string.
 * - In view mode (enabled = false) the field is non-interactive.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    tag: String,
    options: List<Any>
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { if (enabled) expanded = !expanded },
      modifier = modifier) {
        // Anchor: outlined text field (read-only) with same visuals as other inputs
        OutlinedTextField(
            value = value,
            onValueChange = {}, // read-only: selection happens from the menu
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label, color = Gray) },
            // Only show trailing dropdown icon when enabled (edit mode) to avoid confusion in view
            // mode
            trailingIcon =
                if (enabled) {
                  { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                } else {
                  null
                },
            shape = RoundedCornerShape(Dimens.PaddingMedium),
            modifier =
                Modifier.menuAnchor().fillMaxWidth().height(Dimens.ImageSizeSmall).testTag(tag),
            colors =
                TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Gray,
                    focusedIndicatorColor = MainColor,
                    focusedLabelColor = MainColor,
                    cursorColor = TextColor,
                    focusedContainerColor = BackGroundColor,
                    unfocusedContainerColor = BackGroundColor,
                    disabledContainerColor = BackGroundColor),
            textStyle = TextStyle(color = TextColor, fontSize = 16.sp, textAlign = TextAlign.Start))

        // Dropdown menu (rounded corners to match the field)
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(Dimens.PaddingMedium)) {
              options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.toString()) },
                    onClick = {
                      onValueChange(item.toString())
                      expanded = false
                    })
              }
            }
      }
}

@Composable
private fun RestartDialog(onDismissRequest: () -> Unit, onRestart: () -> Unit) {
  Dialog(onDismissRequest = { onDismissRequest() }) {
    Card(
        modifier =
            Modifier.testTag(C.ProfileScreenTags.RESTART_DIALOG)
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.CardCornerRadius))
                .background(BackGroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.PaddingSmall),
    ) {
      Column(
          modifier = Modifier.fillMaxWidth().padding(Dimens.PaddingSmall),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)) {
            Text(stringResource(R.string.profile_must_restart_pop_up), textAlign = TextAlign.Center)
            Row(
                modifier = Modifier.fillMaxWidth(1f),
            ) {
              RestartPopUpButton(
                  textId = R.string.cancel,
                  onClick = { onDismissRequest() },
                  modifier =
                      Modifier.weight(1f).testTag(C.ProfileScreenTags.RESTART_DIALOG_CANCEL_BUTTON))
              RestartPopUpButton(
                  textId = R.string.restart,
                  onClick = { onRestart() },
                  modifier =
                      Modifier.weight(1f)
                          .testTag(C.ProfileScreenTags.RESTART_DIALOG_RESTART_BUTTON))
            }
          }
    }
  }
}

@Composable
private fun RestartPopUpButton(@StringRes textId: Int, onClick: () -> Unit, modifier: Modifier) {
  Row(
      modifier = modifier.padding(horizontal = Dimens.PaddingSmall),
      horizontalArrangement = Arrangement.Center) {
        Button(
            onClick = { onClick() },
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = MainColor, contentColor = White)) {
              Text(text = stringResource(textId))
            }
      }
}

@Composable
private fun CardBlock(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
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

/**
 * Interactive preview that lets you toggle between view/edit modes and simulate typing/selection
 * locally without a ViewModel.
 * - Tap the top-right "Modify/Cancel" to toggle modes.
 * - In edit mode, you can type and pick dropdown values.
 * - Pressing "SAVE" in this preview simply flips back to view mode.
 */
@Preview(showBackground = true, name = "Profile – Light Mode")
@Preview(
    showBackground = true,
    name = "Profile – Dark Mode",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_Profile_Interactive() {
  // Local preview state (no ViewModel involved)
  var isEditing by rememberSaveable { mutableStateOf(false) }
  var firstName by rememberSaveable { mutableStateOf("Mansour") }
  var lastName by rememberSaveable { mutableStateOf("Kanaan") }
  var university by rememberSaveable { mutableStateOf("EPFL") }
  var language by rememberSaveable { mutableStateOf("English") }
  var residence by rememberSaveable { mutableStateOf("Vortex, Coloc") }

  val ui =
      ProfileUiState(
          firstName = firstName,
          lastName = lastName,
          university = university,
          language = language,
          residence = residence,
          isEditing = isEditing,
          isSaving = false,
          errorMsg = null)

  MySwissDormAppTheme {
    ProfileScreenContent(
        state = ui,
        onFirstNameChange = { firstName = it },
        onLastNameChange = { lastName = it },
        onUniversityChange = { university = it },
        onLanguageChange = { language = it },
        onResidenceChange = { residence = it },
        onLogout = {},
        onChangeProfilePicture = {},
        onSettingsClicked = {},
        onToggleEditing = { isEditing = !isEditing },
        onSave = { isEditing = false },
        onEditPreferencesClick = {},
        onContributionClick = {},
        onViewBookmarks = {})
  }
}
