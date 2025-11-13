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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextColor

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
                shape = RoundedCornerShape(16.dp)) {
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

              OutlinedTextField(
                  value = ui.name,
                  onValueChange = vm::onName,
                  label = { Text("Name") },
                  singleLine = true,
                  colors = coloring(),
                  modifier = Modifier.fillMaxWidth())
              OutlinedTextField(
                  value = ui.latitude,
                  onValueChange = vm::onLatitude,
                  label = { Text("Latitude") },
                  singleLine = true,
                  colors = coloring(),
                  modifier = Modifier.fillMaxWidth())
              OutlinedTextField(
                  value = ui.longitude,
                  onValueChange = vm::onLongitude,
                  label = { Text("Longitude") },
                  singleLine = true,
                  colors = coloring(),
                  modifier = Modifier.fillMaxWidth())
              OutlinedTextField(
                  value = ui.locName,
                  onValueChange = vm::onLocName,
                  label = { Text("Location Name") },
                  singleLine = true,
                  colors = coloring(),
                  modifier = Modifier.fillMaxWidth())

              // Entity-specific fields
              when (ui.selected) {
                // City fields
                AdminPageViewModel.EntityType.CITY -> {
                  OutlinedTextField(
                      value = ui.description,
                      onValueChange = vm::onDescription,
                      colors = coloring(),
                      label = { Text("Description") },
                      modifier = Modifier.fillMaxWidth())
                  OutlinedTextField(
                      value = ui.imageId,
                      onValueChange = vm::onImageId,
                      colors = coloring(),
                      label = { Text("Image ID") },
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth())
                }
                // Residency fields
                AdminPageViewModel.EntityType.RESIDENCY -> {
                  OutlinedTextField(
                      value = ui.description,
                      onValueChange = vm::onDescription,
                      colors = coloring(),
                      label = { Text("Description") },
                      modifier = Modifier.fillMaxWidth())
                  OutlinedTextField(
                      value = ui.city,
                      onValueChange = vm::onCity,
                      label = { Text("City") },
                      colors = coloring(),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth())
                  OutlinedTextField(
                      value = ui.email,
                      colors = coloring(),
                      onValueChange = vm::onEmail,
                      label = { Text("Email (optional)") },
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth())
                  OutlinedTextField(
                      value = ui.phone,
                      colors = coloring(),
                      onValueChange = vm::onPhone,
                      label = { Text("Phone (optional)") },
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth())
                  OutlinedTextField(
                      value = ui.website,
                      colors = coloring(),
                      onValueChange = vm::onWebsite,
                      label = { Text("Website (optional)") },
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth())
                }
                // University fields
                AdminPageViewModel.EntityType.UNIVERSITY -> {
                  OutlinedTextField(
                      value = ui.city,
                      onValueChange = vm::onCity,
                      label = { Text("City") },
                      singleLine = true,
                      colors = coloring(),
                      modifier = Modifier.fillMaxWidth())
                  OutlinedTextField(
                      value = ui.email,
                      onValueChange = vm::onEmail,
                      label = { Text("Email") },
                      singleLine = true,
                      colors = coloring(),
                      modifier = Modifier.fillMaxWidth())
                  OutlinedTextField(
                      value = ui.phone,
                      onValueChange = vm::onPhone,
                      label = { Text("Phone") },
                      singleLine = true,
                      colors = coloring(),
                      modifier = Modifier.fillMaxWidth())
                  OutlinedTextField(
                      value = ui.website,
                      onValueChange = vm::onWebsite,
                      label = { Text("Website URL") },
                      singleLine = true,
                      colors = coloring(),
                      modifier = Modifier.fillMaxWidth())
                }
              }
              Spacer(Modifier.height(16.dp))
            }
      }
}

@Composable
private fun coloring(): androidx.compose.material3.TextFieldColors {
  return OutlinedTextFieldDefaults.colors(
      focusedBorderColor = MainColor,
      unfocusedBorderColor = MainColor,
      focusedLabelColor = MainColor,
      unfocusedLabelColor = Color.Gray)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntityChip(text: String, selected: Boolean, onClick: () -> Unit) {
  // This helper was implemented using AI
  AssistChip(
      onClick = onClick,
      label = { Text(text) },
      leadingIcon =
          if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, tint = TextColor) }
          } else null,
      modifier = Modifier.testTag("Chip_$text"))
}
