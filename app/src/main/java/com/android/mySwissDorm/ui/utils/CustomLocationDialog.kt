package com.android.mySwissDorm.ui.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.theme.White

/**
 * A dialog for entering a custom location, with autocomplete suggestions.
 *
 * @param value The current text in the location search field.
 * @param currentLocation The currently selected location.
 * @param locationSuggestions A list of location suggestions to display.
 * @param onValueChange A callback for when the search text changes.
 * @param onDropDownLocationSelect A callback for when a location is selected from the suggestions.
 * @param onDismiss A callback for when the dialog is dismissed.
 * @param onConfirm A callback for when the confirm button is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomLocationDialog(
    value: String,
    currentLocation: Location?,
    locationSuggestions: List<Location>,
    onValueChange: (String) -> Unit,
    onDropDownLocationSelect: (Location) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Location) -> Unit
) {
  var showDropdown by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackGroundColor)) {
          Box {
            Column(
                modifier =
                    Modifier.padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                  Text(
                      "Enter Custom Location",
                      color = TextColor,
                      fontSize = 20.sp,
                      fontWeight = FontWeight.Bold,
                      modifier =
                          Modifier.testTag(
                              C.CustomLocationDialogTags.DIALOG_TITLE,
                          ))
                  ExposedDropdownMenuBox(
                      expanded = showDropdown && locationSuggestions.isNotEmpty(),
                      onExpandedChange = { showDropdown = it },
                      modifier = Modifier.testTag(C.CustomLocationDialogTags.DROPDOWN_MENU)) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = {
                              onValueChange(it)
                              showDropdown = true
                            },
                            label = { Text("Location") },
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MainColor,
                                    unfocusedBorderColor = TextBoxColor,
                                    focusedLabelColor = MainColor,
                                    cursorColor = TextColor,
                                    unfocusedLabelColor = TextColor.copy(alpha = 0.6f)),
                            modifier =
                                Modifier.menuAnchor()
                                    .fillMaxWidth()
                                    .testTag(C.CustomLocationDialogTags.LOCATION_TEXT_FIELD),
                            singleLine = true)

                        ExposedDropdownMenu(
                            expanded = showDropdown && locationSuggestions.isNotEmpty(),
                            onDismissRequest = { showDropdown = false }) {
                              locationSuggestions.filterNotNull().take(3).forEachIndexed {
                                  index,
                                  location ->
                                DropdownMenuItem(
                                    text = {
                                      Text(
                                          text =
                                              location.name.take(30) +
                                                  if (location.name.length > 30) "..." else "",
                                          maxLines = 1)
                                    },
                                    onClick = {
                                      onValueChange(location.name)
                                      onDropDownLocationSelect(location)
                                      showDropdown = false
                                    },
                                    modifier =
                                        Modifier.testTag(
                                            C.CustomLocationDialogTags.locationSuggestion(index)))
                              }

                              if (locationSuggestions.size > 3) {
                                DropdownMenuItem(
                                    text = { Text("More...") },
                                    onClick = { /* Optionally show more results */}, // TODO
                                    modifier =
                                        Modifier.testTag(C.CustomLocationDialogTags.MORE_OPTION))
                              }
                            }
                      }
                  Button(
                      onClick = {
                        currentLocation?.let {
                          onConfirm(it)
                          onDismiss()
                        }
                      },
                      enabled = currentLocation != null,
                      colors = ButtonDefaults.buttonColors(containerColor = MainColor),
                      modifier = Modifier.testTag(C.CustomLocationDialogTags.CONFIRM_BUTTON)) {
                        Text("Confirm", color = White)
                      }
                }
            IconButton(
                onClick = onDismiss,
                modifier =
                    Modifier.align(Alignment.TopStart)
                        .testTag(C.CustomLocationDialogTags.CLOSE_BUTTON)) {
                  Icon(Icons.Default.Close, contentDescription = "Close", tint = MainColor)
                }
          }
        }
  }
}
