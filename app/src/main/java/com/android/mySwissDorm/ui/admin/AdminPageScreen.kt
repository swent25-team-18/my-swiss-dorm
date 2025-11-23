package com.android.mySwissDorm.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.InputSanitizers
import com.android.mySwissDorm.ui.SanitizedOutlinedTextField
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.theme.White
import com.android.mySwissDorm.ui.utils.CustomLocationDialog
import com.android.mySwissDorm.ui.utils.onUserLocationClickFunc

// Documentation was made with the help of AI
/**
 * Admin page screen for creating and managing entities (Cities, Residencies, Universities).
 *
 * This screen provides a form-based interface for administrators to add new entities to the system.
 * It supports three entity types:
 * - **City**: Requires name, description, image ID, and location
 * - **Residency**: Requires name, description, city, location, and optional email, phone, website
 * - **University**: Requires name, city, email, phone, website URL, and location
 *
 * The screen features:
 * - Entity type selection chips (City, Residency, University)
 * - Dynamic form fields based on selected entity type
 * - Custom location picker with autocomplete search
 * - Input sanitization and validation via [SanitizedOutlinedTextField]
 * - Save button with loading state and success/error messages
 *
 * @param vm The [AdminPageViewModel] that manages the form state and submission logic. Defaults to
 *   a new instance created via [viewModel].
 * @param canAccess Whether the current user has admin access. If false, displays "Admins only."
 *   message and returns early.
 * @param onBack Callback invoked when the back button is clicked to navigate away from the screen.
 * @see AdminPageViewModel for state management and validation logic
 * @see CustomLocationDialog for location selection functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPageScreen(
    vm: AdminPageViewModel = viewModel(),
    canAccess: Boolean,
    onBack: () -> Unit,
) {
  if (!canAccess) {
    // This won't show since only admins can view the admin page in settings but it's here for extra
    // security
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(stringResource(R.string.admin_page_admins_only))
    }
    return
  }

  val ui = vm.uiState
  val scrollState = rememberScrollState()
  val context = LocalContext.current
  val onUseCurrentLocationClick = onUserLocationClickFunc(context, vm)

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.admin_page_title)) },
            navigationIcon = {
              IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MainColor)
              }
            })
      },
      bottomBar = {
        Surface(shadowElevation = 8.dp) {
          Column(Modifier.padding(16.dp)) {
            Button(
                onClick = vm::submit,
                enabled = !ui.isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = MainColor),
                modifier =
                    Modifier.fillMaxWidth().height(52.dp).testTag(C.AdminPageTags.SAVE_BUTTON),
                shape = RoundedCornerShape(24.dp)) {
                  if (ui.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                  } else {
                    Text(stringResource(R.string.save), color = Color.White)
                  }
                }
          }
        }
      }) { pad ->
        Column(
            Modifier.padding(pad)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
              // First row: City, Residency, University
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically) {
                    EntityChip(
                        text = stringResource(R.string.city),
                        selected = ui.selected == AdminPageViewModel.EntityType.CITY,
                        testTag = C.AdminPageTags.CHIP_CITY) {
                          vm.onTypeChange(AdminPageViewModel.EntityType.CITY)
                        }
                    EntityChip(
                        text = stringResource(R.string.residency),
                        selected = ui.selected == AdminPageViewModel.EntityType.RESIDENCY,
                        testTag = C.AdminPageTags.CHIP_RESIDENCY) {
                          vm.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)
                        }
                    EntityChip(
                        text = stringResource(R.string.university),
                        selected = ui.selected == AdminPageViewModel.EntityType.UNIVERSITY,
                        testTag = C.AdminPageTags.CHIP_UNIVERSITY) {
                          vm.onTypeChange(AdminPageViewModel.EntityType.UNIVERSITY)
                        }
                  }
              // Second row: Admin
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically) {
                    EntityChip(
                        text = stringResource(R.string.admin),
                        selected = ui.selected == AdminPageViewModel.EntityType.ADMIN,
                        testTag = C.AdminPageTags.CHIP_ADMIN) {
                          vm.onTypeChange(AdminPageViewModel.EntityType.ADMIN)
                        }
                  }

              // Entity-specific fields
              when (ui.selected) {
                // Admin fields
                AdminPageViewModel.EntityType.ADMIN -> {
                  SanitizedOutlinedTextField(
                      value = ui.email,
                      onValueChange = vm::onEmail,
                      label = stringResource(R.string.email),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.Email,
                      placeholder = stringResource(R.string.email),
                      imeAction = ImeAction.Done)
                }
                // City fields
                AdminPageViewModel.EntityType.CITY -> {
                  SanitizedOutlinedTextField(
                      value = ui.name,
                      onValueChange = vm::onName,
                      label = stringResource(R.string.name),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.FirstName,
                      placeholder = stringResource(R.string.name),
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.description,
                      onValueChange = vm::onDescription,
                      label = stringResource(R.string.description),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.Description,
                      placeholder = stringResource(R.string.description),
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.imageId,
                      onValueChange = vm::onImageId,
                      label = stringResource(R.string.admin_page_image_id),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.Website, // for now
                      placeholder = stringResource(R.string.admin_page_image_id),
                      imeAction = ImeAction.Next)
                }
                // Residency fields
                AdminPageViewModel.EntityType.RESIDENCY -> {
                  SanitizedOutlinedTextField(
                      value = ui.name,
                      onValueChange = vm::onName,
                      label = stringResource(R.string.name),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.FirstName,
                      placeholder = stringResource(R.string.name),
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.description,
                      onValueChange = vm::onDescription,
                      label = stringResource(R.string.description),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.Description,
                      placeholder = stringResource(R.string.description),
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.city,
                      onValueChange = vm::onCity,
                      label = stringResource(R.string.city),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.City,
                      placeholder = stringResource(R.string.city),
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.email,
                      onValueChange = vm::onEmail,
                      label = stringResource(R.string.email),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.City,
                      placeholder =
                          "${stringResource(R.string.email)} (${stringResource(R.string.optional)})",
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.phone,
                      onValueChange = vm::onPhone,
                      label = stringResource(R.string.phone),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.Phone,
                      placeholder =
                          "${stringResource(R.string.phone)} (${stringResource(R.string.optional)})",
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.website,
                      onValueChange = vm::onWebsite,
                      label = stringResource(R.string.website),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.Website,
                      placeholder =
                          "${stringResource(R.string.website)} (${stringResource(R.string.optional)})",
                      imeAction = ImeAction.Next)
                }
                // University fields
                AdminPageViewModel.EntityType.UNIVERSITY -> {
                  SanitizedOutlinedTextField(
                      value = ui.name,
                      onValueChange = vm::onName,
                      label = stringResource(R.string.name),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.FirstName,
                      placeholder = stringResource(R.string.name),
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.city,
                      onValueChange = vm::onCity,
                      label = stringResource(R.string.city),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.City,
                      placeholder = stringResource(R.string.city),
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.email,
                      onValueChange = vm::onEmail,
                      label = stringResource(R.string.email),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.Email,
                      placeholder = stringResource(R.string.email),
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.phone,
                      onValueChange = vm::onPhone,
                      label = stringResource(R.string.phone),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.Phone,
                      placeholder = stringResource(R.string.phone),
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.website,
                      onValueChange = vm::onWebsite,
                      label = stringResource(R.string.website),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.Website,
                      placeholder = "${stringResource(R.string.website)} URL",
                      imeAction = ImeAction.Next)
                }
              }

              // Location picker button (not needed for Admin)
              if (ui.selected != AdminPageViewModel.EntityType.ADMIN) {
                TextButton(
                    onClick = { vm.onCustomLocationClick() },
                    modifier = Modifier.fillMaxWidth().testTag(C.AdminPageTags.LOCATION_BUTTON),
                    shape = RoundedCornerShape(16.dp)) {
                      Icon(
                          imageVector = Icons.Default.Place,
                          contentDescription = "Location",
                          tint = MainColor)
                      Spacer(Modifier.width(8.dp))
                      Text(
                          text =
                              ui.location?.name
                                  ?: stringResource(R.string.admin_page_select_location),
                          color = if (ui.location != null) TextColor else MainColor)
                    }
              }

              Spacer(Modifier.height(16.dp))
            }

        // Custom Location Dialog
        if (ui.showCustomLocationDialog) {
          val onValueChange =
              remember<(String) -> Unit> { { query -> vm.setCustomLocationQuery(query) } }
          val onDropDownLocationSelect =
              remember<(com.android.mySwissDorm.model.map.Location) -> Unit> {
                { location -> vm.setCustomLocation(location) }
              }
          val onDismiss = remember { { vm.dismissCustomLocationDialog() } }
          val onConfirm =
              remember<(com.android.mySwissDorm.model.map.Location) -> Unit> {
                { location -> vm.onLocationConfirm(location) }
              }

          CustomLocationDialog(
              value = ui.customLocationQuery,
              currentLocation = ui.customLocation,
              locationSuggestions = ui.locationSuggestions,
              onValueChange = onValueChange,
              onDropDownLocationSelect = onDropDownLocationSelect,
              onDismiss = onDismiss,
              onConfirm = onConfirm,
              onUseCurrentLocationClick = onUseCurrentLocationClick)
        }

        // Admin confirmation dialog
        if (ui.showAdminConfirmDialog) {
          AlertDialog(
              onDismissRequest = { vm.cancelAdminAdd() },
              title = { Text(stringResource(R.string.admin)) },
              text = {
                Text(
                    text = "Confirm you want to add ${ui.email.trim()} as an admin",
                    style = MaterialTheme.typography.bodyMedium)
              },
              confirmButton = {
                TextButton(
                    onClick = { vm.confirmAdminAdd() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MainColor)) {
                      Text(stringResource(R.string.save))
                    }
              },
              dismissButton = {
                TextButton(
                    onClick = { vm.cancelAdminAdd() },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextColor)) {
                      Text(stringResource(R.string.cancel))
                    }
              })
        }

        // Success/Error message dialog
        if (ui.message != null) {
          val isError = ui.message.startsWith("Error:")
          val messageText = if (isError) ui.message.removePrefix("Error: ") else ui.message
          AlertDialog(
              onDismissRequest = { vm.clearMessage() },
              title = {
                Text(
                    text = if (isError) "Error" else stringResource(R.string.admin),
                    color = if (isError) MaterialTheme.colorScheme.error else MainColor)
              },
              text = { Text(text = messageText, style = MaterialTheme.typography.bodyMedium) },
              confirmButton = {
                TextButton(
                    onClick = { vm.clearMessage() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MainColor)) {
                      Text("OK")
                    }
              })
        }
      }
}

/**
 * A chip component for selecting entity types in the admin page.
 *
 * Displays an [AssistChip] with customizable styling based on selection state. When selected, the
 * chip displays a check icon and uses MainColor background with white text. When unselected, it
 * uses TextBoxColor background with TextColor text.
 *
 * @param text The label text to display on the chip (e.g., "City", "Residency", "University").
 * @param selected Whether this chip is currently selected.
 * @param testTag The test tag identifier for UI testing. Should be from [C.AdminPageTags].
 * @param onClick Callback invoked when the chip is clicked to change the entity type.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntityChip(text: String, selected: Boolean, testTag: String, onClick: () -> Unit) {
  AssistChip(
      onClick = onClick,
      colors =
          AssistChipDefaults.assistChipColors(
              containerColor = if (selected) MainColor else TextBoxColor,
              labelColor = if (selected) White else TextColor,
              leadingIconContentColor = if (selected) White else TextColor),
      label = { Text(text) },
      leadingIcon =
          if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, tint = White) }
          } else null,
      modifier = Modifier.testTag(testTag))
}
