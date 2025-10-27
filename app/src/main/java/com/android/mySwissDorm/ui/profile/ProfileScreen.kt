package com.android.mySwissDorm.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.model.profile.Language
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.LightGray
import com.android.mySwissDorm.ui.theme.Red0
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
 * - "field_first_name", "field_last_name", "field_language", "field_residence"
 */
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onChangeProfilePicture: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileScreenViewModel = viewModel()
) {
  // Collect VM state (initial ensures preview/first composition has data)
  val state by viewModel.uiState.collectAsState(initial = ProfileUiState())

  ProfileScreenContent(
      state = state,
      onFirstNameChange = viewModel::onFirstNameChange,
      onLastNameChange = viewModel::onLastNameChange,
      onLanguageChange = viewModel::onLanguageChange,
      onResidenceChange = viewModel::onResidenceChange,
      onLogout = onLogout,
      onChangeProfilePicture = onChangeProfilePicture,
      onBack = onBack,
      onToggleEditing = viewModel::toggleEditing,
      onSave = viewModel::saveProfile)
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
 * @param onLanguageChange Pushes final language to VM on save.
 * @param onResidenceChange Pushes final residence to VM on save.
 * @param onLogout Invoked in view mode by the "LOGOUT" button.
 * @param onChangeProfilePicture Invoked when avatar is tapped in edit mode.
 * @param onBack Invoked by top app bar back button.
 * @param onToggleEditing Toggles between view and edit mode.
 * @param onSave Persists profile (Firestore) via the VM; also exits edit mode on success.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreenContent(
    state: ProfileUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onResidenceChange: (String) -> Unit,
    onLogout: () -> Unit,
    onChangeProfilePicture: () -> Unit,
    onBack: () -> Unit,
    onToggleEditing: () -> Unit,
    onSave: () -> Unit
) {
  // Local editable buffers used ONLY in edit mode (remembered across recompositions)
  var firstLocal by rememberSaveable(state.isEditing) { mutableStateOf(state.firstName) }
  var lastLocal by rememberSaveable(state.isEditing) { mutableStateOf(state.lastName) }
  var languageLocal by rememberSaveable(state.isEditing) { mutableStateOf(state.language) }
  var residenceLocal by rememberSaveable(state.isEditing) { mutableStateOf(state.residence) }

  // When entering edit mode, snapshot current VM values into locals to start from latest persisted
  // values
  LaunchedEffect(state.isEditing) {
    if (state.isEditing) {
      firstLocal = state.firstName
      lastLocal = state.lastName
      languageLocal = state.language
      residenceLocal = state.residence
    }
  }

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Text(
                  text = "Profile",
                  fontSize = 24.sp,
                  color = Color.Black,
                  modifier = Modifier.testTag(C.Tag.PROFILE_SCREEN_TITLE))
            },
            navigationIcon = {
              IconButton(
                  onClick = onBack, modifier = Modifier.testTag(C.Tag.PROFILE_SCREEN_BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go back",
                        tint = Red0)
                  }
            },
            actions = {
              // Toggle between view/edit (Edit icon ↔ Close icon)
              IconButton(
                  onClick = onToggleEditing, modifier = Modifier.testTag("profile_edit_toggle")) {
                    if (state.isEditing) {
                      Icon(Icons.Default.Close, contentDescription = "Cancel editing", tint = Red0)
                    } else {
                      Icon(Icons.Default.Edit, contentDescription = "Modify profile", tint = Red0)
                    }
                  }
            })
      }) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("profile_list"),
            horizontalAlignment = Alignment.CenterHorizontally) {

              // Profile Picture (clickable only in edit mode; remains disabled in view mode but
              // keeps click semantics)
              Box(
                  modifier = Modifier.fillMaxWidth().height(150.dp),
                  contentAlignment = Alignment.Center) {
                    Box(
                        modifier =
                            Modifier.size(100.dp)
                                .clip(CircleShape)
                                .border(2.dp, Red0, CircleShape)
                                .background(LightGray)
                                .clickable(enabled = state.isEditing) { onChangeProfilePicture() }
                                .testTag("profile_picture_box"),
                        contentAlignment = Alignment.Center) {
                          Icon(
                              imageVector = Icons.Default.Person,
                              contentDescription = "Change profile picture",
                              modifier = Modifier.size(40.dp),
                              tint = Red0)
                        }
                  }

              // Name row: First name | Last name (equal widths via weight)
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditableTextField(
                        label = "First name",
                        value = if (state.isEditing) firstLocal else state.firstName,
                        onValueChange = {
                          if (it.length <= 20) firstLocal = it
                        }, // ← cap at 20 chars
                        tag = "field_first_name",
                        enabled = state.isEditing,
                        modifier = Modifier.weight(1f))

                    EditableTextField(
                        label = "Last name",
                        value = if (state.isEditing) lastLocal else state.lastName,
                        onValueChange = {
                          if (it.length <= 20) lastLocal = it
                        }, // ← cap at 20 chars
                        tag = "field_last_name",
                        enabled = state.isEditing,
                        modifier = Modifier.weight(1f))
                  }

              // Language dropdown (from Language enum)
              DropdownField(
                  label = "Language",
                  value = if (state.isEditing) languageLocal else state.language,
                  onValueChange = { languageLocal = it },
                  tag = "field_language",
                  enabled = state.isEditing,
                  modifier = Modifier.fillMaxWidth(),
                  options = remember { Language.values().toList() })

              // Residence dropdown (from ResidencyName enum)
              DropdownField(
                  label = "Residence",
                  value = if (state.isEditing) residenceLocal else state.residence,
                  onValueChange = {
                    residenceLocal = it
                  }, // receives the selected enum's display string
                  enabled = state.isEditing,
                  modifier = Modifier.fillMaxWidth(),
                  tag = "field_residence",
                  options = remember { state.allResidencies })

              // Bottom action area: Save (edit mode) or Logout (view mode)
              if (state.isEditing) {
                Button(
                    onClick = {
                      // Push local edits to VM, then persist
                      onFirstNameChange(firstLocal)
                      onLastNameChange(lastLocal)
                      onLanguageChange(languageLocal)
                      onResidenceChange(residenceLocal)
                      onSave()
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(top = 16.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("profile_save_button"),
                    colors =
                        ButtonDefaults.buttonColors(containerColor = Red0, contentColor = White),
                    enabled = !state.isSaving) {
                      Text(text = if (state.isSaving) "SAVING..." else "SAVE")
                    }
              } else {
                Button(
                    onClick = onLogout,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(top = 16.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("profile_logout_button"),
                    colors =
                        ButtonDefaults.buttonColors(containerColor = White, contentColor = Red0)) {
                      Text(text = "LOGOUT", color = Red0)
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
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    tag: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange, // never gate this (enabled already blocks edits)
      enabled = enabled,
      singleLine = true,
      label = { Text(text = label, color = Color.Gray) },
      modifier = modifier.height(64.dp).testTag(tag),
      placeholder = { Text(text = label) },
      shape = RoundedCornerShape(12.dp),
      colors =
          TextFieldDefaults.colors(
              unfocusedIndicatorColor = Color.Transparent,
              focusedIndicatorColor = Red0,
              focusedLabelColor = Red0,
              cursorColor = Red0,
              focusedContainerColor = LightGray,
              unfocusedContainerColor = LightGray,
              disabledContainerColor = LightGray),
      textStyle = TextStyle(color = Color.Black, fontSize = 16.sp, textAlign = TextAlign.Start),
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
            label = { Text(label, color = Color.Gray) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.menuAnchor().fillMaxWidth().height(64.dp).testTag(tag),
            colors =
                TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Red0,
                    focusedLabelColor = Red0,
                    cursorColor = Red0,
                    focusedContainerColor = LightGray,
                    unfocusedContainerColor = LightGray,
                    disabledContainerColor = LightGray),
            textStyle =
                TextStyle(color = Color.Black, fontSize = 16.sp, textAlign = TextAlign.Start))

        // Dropdown menu (rounded corners to match the field)
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(12.dp)) {
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

/**
 * Interactive preview that lets you toggle between view/edit modes and simulate typing/selection
 * locally without a ViewModel.
 * - Tap the top-right "Modify/Cancel" to toggle modes.
 * - In edit mode, you can type and pick dropdown values.
 * - Pressing "SAVE" in this preview simply flips back to view mode.
 */
@Preview(showBackground = true, name = "Profile – Interactive")
@Composable
private fun Preview_Profile_Interactive() {
  // Local preview state (no ViewModel involved)
  var isEditing by rememberSaveable { mutableStateOf(false) }
  var firstName by rememberSaveable { mutableStateOf("Mansour") }
  var lastName by rememberSaveable { mutableStateOf("Kanaan") }
  var language by rememberSaveable { mutableStateOf("English") }
  var residence by rememberSaveable { mutableStateOf("Vortex, Coloc") }

  // Build the UI state the screen expects
  val ui =
      ProfileUiState(
          firstName = firstName,
          lastName = lastName,
          language = language,
          residence = residence,
          isEditing = isEditing,
          isSaving = false,
          errorMsg = null)

  MaterialTheme {
    ProfileScreenContent(
        state = ui,
        onFirstNameChange = { firstName = it }, // called on SAVE
        onLastNameChange = { lastName = it }, // called on SAVE
        onLanguageChange = { language = it }, // called on SAVE
        onResidenceChange = { residence = it }, // called on SAVE
        onLogout = {},
        onChangeProfilePicture = {},
        onBack = {},
        onToggleEditing = { isEditing = !isEditing }, // Modify/Cancel
        onSave = { isEditing = false } // after SAVE, return to view mode
        )
  }
}
