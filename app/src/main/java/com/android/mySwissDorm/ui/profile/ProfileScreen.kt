package com.android.mySwissDorm.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack // Added import for the back icon
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.android.mySwissDorm.ui.theme.LightGray
import com.android.mySwissDorm.ui.theme.LightGray0
import com.android.mySwissDorm.ui.theme.Red0
import com.android.mySwissDorm.ui.theme.White

@Preview(showBackground = true)
@Composable
fun PreviewProfileScreen() {
  ProfileScreen(
      onLogout = { /* Handle logout */},
      onChangeProfilePicture = { /* Handle profile picture change */},
      onBack = { /* Handle back press */} // Added onBack for preview
      )
}

// Added onBack parameter
@Composable
fun ProfileScreen(onLogout: () -> Unit, onChangeProfilePicture: () -> Unit, onBack: () -> Unit) {
  // Lighter gray for the profile picture background

  // State for each field (Username, Birthday, etc.)
  var username by remember { mutableStateOf("") }
  var birthDate by remember { mutableStateOf("") }
  var language by remember { mutableStateOf("") }
  var residence by remember { mutableStateOf("") }

  // Fields for the profile
  Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

    // TOP BAR SECTION: Added a Box to position the back button on the left
    Box(
        modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(bottom = 16.dp),
        contentAlignment = Alignment.Center) {
          // Back Button
          IconButton(
              onClick = onBack,
              modifier =
                  Modifier.align(Alignment.CenterStart).testTag("profile_back_button") // ADDED TAG
              ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go back",
                    tint = Color.Black)
              }

          // Profile Text (kept centered)
          Text(
              text = "Profile",
              fontSize = 24.sp,
              modifier = Modifier.align(Alignment.Center).testTag("profile_title"), // ADDED TAG
              color = Color.Black,
              textAlign = TextAlign.Center)
        }

    // Profile Picture Section with red border centered
    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
      // Profile picture with red border and clickable
      Box(
          modifier =
              Modifier.size(100.dp)
                  .clip(CircleShape)
                  .border(2.dp, Red0, CircleShape)
                  .background(LightGray)
                  .clickable { onChangeProfilePicture() }
                  .testTag("profile_picture_box"), // ADDED TAG
          contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Change profile picture",
                modifier = Modifier.size(40.dp),
                tint = Red0)
          }
    }

    // Editable Fields (Tags will be set inside EditableTextField)
    EditableTextField(
        label = "Username",
        value = username,
        onValueChange = { username = it },
        tag = "field_username")
    EditableTextField(
        label = "Birth Date",
        value = birthDate,
        onValueChange = { birthDate = it },
        tag = "field_birth_date")
    EditableTextField(
        label = "Language",
        value = language,
        onValueChange = { language = it },
        tag = "field_language")
    EditableTextField(
        label = "Residence",
        value = residence,
        onValueChange = { residence = it },
        tag = "field_residence")

    // Fields with boxes for last 3 (Tags will be set inside SettingToggle)
    SettingToggle(label = "Anonymous", redColor = Red0, tag = "switch_anonymous")
    SettingToggle(label = "Visibility", redColor = Red0, tag = "switch_visibility")
    SettingToggle(label = "Notifications", redColor = Red0, tag = "switch_notifications")

    // Logout Button
    Button(
        onClick = onLogout,
        modifier =
            Modifier.fillMaxWidth()
                .padding(top = 16.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .testTag("profile_logout_button"), // ADDED TAG
        colors =
            ButtonDefaults.buttonColors(
                containerColor = White, // White background
                contentColor = Red0 // Red text color
                )) {
          Text(text = "LOGOUT", color = Red0) // Red color for text
    }
  }
}

@Composable
fun SettingToggle(label: String, redColor: Color, tag: String) {
  // 1. Declare and remember the state for the switch
  var isChecked by remember { mutableStateOf(true) } // Start checked (red) or false (gray)

  Column(modifier = Modifier.padding(vertical = 2.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
          // Box only for the label, now with adjusted width and padding
          Box(
              modifier =
                  Modifier.padding(end = 16.dp)
                      .weight(1f)
                      .background(LightGray, RoundedCornerShape(12.dp))
                      .padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(text = label, fontSize = 16.sp, color = Color.Gray)
              }

          // The switch
          Switch(
              checked = isChecked,
              onCheckedChange = { newState -> isChecked = newState },
              modifier = Modifier.testTag(tag), // ADDED TAG
              colors =
                  SwitchDefaults.colors(
                      checkedThumbColor = White,
                      checkedTrackColor = redColor, // Red when checked
                      uncheckedThumbColor = White,
                      uncheckedTrackColor = LightGray0 // Light gray when unchecked
                      ))
        }
  }
}

// In EditableTextField
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableTextField(label: String, value: String, onValueChange: (String) -> Unit, tag: String) {
  Column(modifier = Modifier.padding(vertical = 8.dp)) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = {
          Text(
              text = label,
              modifier = Modifier.background(LightGray), // Background fix
              color = Color.Gray // Label text color
              )
        },
        modifier = Modifier.fillMaxWidth().height(64.dp).testTag(tag), // ADDED TAG
        placeholder = { Text(text = label) },
        shape = RoundedCornerShape(12.dp),
        colors =
            TextFieldDefaults.outlinedTextFieldColors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Red0,
                focusedLabelColor = Red0,
                cursorColor = Red0,
                containerColor = LightGray // Main text field background
                ),
        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp, textAlign = TextAlign.Start),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done))
  }
}
