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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.ui.InputSanitizers
import com.android.mySwissDorm.ui.SanitizedOutlinedTextField
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.theme.White
import com.android.mySwissDorm.ui.utils.CustomLocationDialog

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
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Admins only.") }
    return
  }

  val ui = vm.uiState
  val scrollState = rememberScrollState()

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Admin Page") },
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
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(24.dp)) {
                  if (ui.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                  } else {
                    Text("Save", color = Color.White)
                  }
                }
            Spacer(Modifier.height(8.dp))

            // Show success or error message from the ViewModel
            if (ui.message != null) {
              val isError = ui.message.startsWith("Error:")
              Text(
                  text = ui.message,
                  style = MaterialTheme.typography.bodySmall,
                  color =
                      if (isError) MaterialTheme.colorScheme.error
                      else MaterialTheme.colorScheme.primary)
            }
          }
        }
      }) { pad ->
        Column(
            Modifier.padding(pad)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EntityChip("City", ui.selected == AdminPageViewModel.EntityType.CITY) {
                  vm.onTypeChange(AdminPageViewModel.EntityType.CITY)
                }
                EntityChip("Residency", ui.selected == AdminPageViewModel.EntityType.RESIDENCY) {
                  vm.onTypeChange(AdminPageViewModel.EntityType.RESIDENCY)
                }
                EntityChip("University", ui.selected == AdminPageViewModel.EntityType.UNIVERSITY) {
                  vm.onTypeChange(AdminPageViewModel.EntityType.UNIVERSITY)
                }
              }

              SanitizedOutlinedTextField(
                  value = ui.name,
                  onValueChange = vm::onName,
                  label = "Name",
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth(),
                  fieldType = InputSanitizers.FieldType.FirstName,
                  placeholder = "Name",
                  imeAction = ImeAction.Next)

              // Entity-specific fields
              when (ui.selected) {
                // City fields
                AdminPageViewModel.EntityType.CITY -> {
                  SanitizedOutlinedTextField(
                      value = ui.description,
                      onValueChange = vm::onDescription,
                      label = "Description",
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.Description,
                      placeholder = "Description",
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.imageId,
                      onValueChange = vm::onImageId,
                      label = "Image ID",
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.Website, // for now
                      placeholder = "Image ID",
                      imeAction = ImeAction.Next)
                }
                // Residency fields
                AdminPageViewModel.EntityType.RESIDENCY -> {
                  SanitizedOutlinedTextField(
                      value = ui.description,
                      onValueChange = vm::onDescription,
                      label = "Description",
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.Description,
                      placeholder = "Description",
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.city,
                      onValueChange = vm::onCity,
                      label = "City",
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.City,
                      placeholder = "City",
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.email,
                      onValueChange = vm::onEmail,
                      label = "Email",
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.City,
                      placeholder = "Email (optional)",
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.phone,
                      onValueChange = vm::onPhone,
                      label = "Phone",
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.Phone,
                      placeholder = "Phone (optional)",
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.website,
                      onValueChange = vm::onWebsite,
                      label = "Website",
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.Website,
                      placeholder = "Website (optional)",
                      imeAction = ImeAction.Next)
                }
                // University fields
                AdminPageViewModel.EntityType.UNIVERSITY -> {
                  SanitizedOutlinedTextField(
                      value = ui.city,
                      onValueChange = vm::onCity,
                      label = "City",
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.City,
                      placeholder = "City",
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.email,
                      onValueChange = vm::onEmail,
                      label = "Email",
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.Email,
                      placeholder = "Email",
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.phone,
                      onValueChange = vm::onPhone,
                      label = "Phone",
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.Phone,
                      placeholder = "Phone",
                      imeAction = ImeAction.Next)
                  SanitizedOutlinedTextField(
                      value = ui.website,
                      onValueChange = vm::onWebsite,
                      label = "Website",
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      fieldType = InputSanitizers.FieldType.Website,
                      placeholder = "Website URL",
                      imeAction = ImeAction.Next)
                }
              }

              // Location picker button
              TextButton(
                  onClick = { vm.onCustomLocationClick() },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(16.dp)) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Location",
                        tint = MainColor)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = ui.location?.name ?: "Select Location",
                        color = if (ui.location != null) TextColor else MainColor)
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
              onConfirm = onConfirm)
        }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntityChip(text: String, selected: Boolean, onClick: () -> Unit) {
  // This helper was implemented using AI {
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
          } else null)
}
