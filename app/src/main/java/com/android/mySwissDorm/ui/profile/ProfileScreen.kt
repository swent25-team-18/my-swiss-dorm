package com.android.mySwissDorm.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.android.mySwissDorm.ui.theme.LightGray
import com.android.mySwissDorm.ui.theme.LightGray0
import com.android.mySwissDorm.ui.theme.Red0
import com.android.mySwissDorm.ui.theme.White

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onChangeProfilePicture: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
  val state by
      viewModel.uiState.collectAsState(
          initial = ProfileUiState()) // works without lifecycle-compose

  ProfileScreenContent(
      state = state,
      onUsernameChange = viewModel::onUsernameChange,
      onLanguageChange = viewModel::onLanguageChange,
      onResidenceChange = viewModel::onResidenceChange,
      onAnonymousChange = viewModel::setAnonymous,
      onNotificationsChange = viewModel::setNotifications,
      onLogout = onLogout,
      onChangeProfilePicture = onChangeProfilePicture,
      onBack = onBack,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreenContent(
    state: ProfileUiState,
    onUsernameChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onResidenceChange: (String) -> Unit,
    onAnonymousChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onChangeProfilePicture: () -> Unit,
    onBack: () -> Unit,
) {
  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Text(
                  text = "Profile",
                  fontSize = 24.sp,
                  color = Color.Black,
                  modifier = Modifier.testTag("profile_title"))
            },
            navigationIcon = {
              IconButton(onClick = onBack, modifier = Modifier.testTag("profile_back_button")) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go back",
                    tint = Red0)
              }
            })
      }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              // Profile Picture
              Box(
                  modifier = Modifier.fillMaxWidth().height(150.dp),
                  contentAlignment = Alignment.Center) {
                    Box(
                        modifier =
                            Modifier.size(100.dp)
                                .clip(CircleShape)
                                .border(2.dp, Red0, CircleShape)
                                .background(LightGray)
                                .clickable { onChangeProfilePicture() }
                                .testTag("profile_picture_box"),
                        contentAlignment = Alignment.Center) {
                          Icon(
                              imageVector = Icons.Default.Person,
                              contentDescription = "Change profile picture",
                              modifier = Modifier.size(40.dp),
                              tint = Red0)
                        }
                  }

              // Editable Fields
              EditableTextField(
                  label = "Username",
                  value = state.username,
                  onValueChange = onUsernameChange,
                  tag = "field_username")
              EditableTextField(
                  label = "Language",
                  value = state.language,
                  onValueChange = onLanguageChange,
                  tag = "field_language")
              EditableTextField(
                  label = "Residence",
                  value = state.residence,
                  onValueChange = onResidenceChange,
                  tag = "field_residence")

              // Toggles
              SettingToggle(
                  label = "Anonymous",
                  redColor = Red0,
                  checked = state.anonymous,
                  onCheckedChange = onAnonymousChange,
                  tag = "switch_anonymous")
              SettingToggle(
                  label = "Notifications",
                  redColor = Red0,
                  checked = state.notifications,
                  onCheckedChange = onNotificationsChange,
                  tag = "switch_notifications")

              // Logout Button
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
      }
}

@Composable
fun SettingToggle(
    label: String,
    redColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
  Column(modifier = Modifier.padding(vertical = 2.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
          Box(
              modifier =
                  Modifier.padding(end = 16.dp)
                      .weight(1f)
                      .background(LightGray, RoundedCornerShape(12.dp))
                      .padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(text = label, fontSize = 16.sp, color = Color.Gray)
              }

          Switch(
              checked = checked,
              onCheckedChange = onCheckedChange,
              modifier = Modifier.testTag(tag),
              colors =
                  SwitchDefaults.colors(
                      checkedThumbColor = White,
                      checkedTrackColor = redColor,
                      uncheckedThumbColor = White,
                      uncheckedTrackColor = LightGray0))
        }
  }
}

@Composable
fun EditableTextField(label: String, value: String, onValueChange: (String) -> Unit, tag: String) {
  Column(modifier = Modifier.padding(vertical = 8.dp)) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = {
          Text(text = label, modifier = Modifier.background(LightGray), color = Color.Gray)
        },
        modifier = Modifier.fillMaxWidth().height(64.dp).testTag(tag),
        placeholder = { Text(text = label) },
        shape = RoundedCornerShape(12.dp),
        colors =
            TextFieldDefaults.colors(
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Red0,
                focusedLabelColor = Red0,
                cursorColor = Red0,
                focusedContainerColor = LightGray,
                unfocusedContainerColor = LightGray),
        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp, textAlign = TextAlign.Start),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done))
  }
}

@Preview(showBackground = true)
@Composable
private fun PreviewProfileScreen() {
  ProfileScreen(
      onLogout = {},
      onChangeProfilePicture = {},
      onBack = {},
  )
}
