package com.android.mySwissDorm.ui.authentification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.LightGray
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.ui.theme.Red0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    signUpViewModel: SignUpViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onBack: () -> Unit = {},
    onSignedUp: () -> Unit = {}
) {
  val context = LocalContext.current
  val uiState by signUpViewModel.uiState.collectAsState()
  val scrollState = rememberScrollState()

  LaunchedEffect(uiState.user) { uiState.user?.let { onSignedUp() } }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {},
            navigationIcon = {
              IconButton(onClick = onBack, modifier = Modifier.testTag(C.Tag.SIGN_UP_BACK_BUTTON)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Red0)
              }
            })
      },
      modifier = Modifier.testTag(C.Tag.SIGN_UP_SCREEN)) { innerPadding ->
        Column(
            modifier =
                Modifier.padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
              val outlinedTextModifier =
                  Modifier.background(LightGray, RoundedCornerShape(10.dp)).fillMaxWidth()
              val textFieldColors =
                  OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = Red0,
                      unfocusedBorderColor = Color.Transparent,
                      focusedLabelColor = Red0,
                      unfocusedLabelColor = Color.Gray)

              Spacer(modifier = Modifier.size(16.dp))
              OutlinedTextField(
                  value = uiState.name,
                  onValueChange = { signUpViewModel.updateName(it) },
                  label = { Text(text = stringResource(R.string.sign_up_first_name_placeholder)) },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                  modifier = outlinedTextModifier.testTag(C.Tag.SIGN_UP_NAME_FIELD),
                  colors = textFieldColors,
                  isError = uiState.isNameErr,
                  supportingText = {
                    if (uiState.isNameErr)
                        Text(
                            text = stringResource(R.string.sign_up_name_help),
                            modifier = Modifier.testTag(C.Tag.SIGN_UP_NAME_HELP_TEXT))
                  })

              OutlinedTextField(
                  value = uiState.lastName,
                  onValueChange = { signUpViewModel.updateLastName(it) },
                  label = { Text(text = stringResource(R.string.sign_up_last_name_placeholder)) },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                  modifier = outlinedTextModifier.testTag(C.Tag.SIGN_UP_LAST_NAME_FIELD),
                  colors = textFieldColors,
                  isError = uiState.isLastNameErr,
                  supportingText = {
                    if (uiState.isLastNameErr)
                        Text(
                            text = stringResource(R.string.sign_up_last_name_help),
                            modifier = Modifier.testTag(C.Tag.SIGN_UP_LAST_NAME_HELP_TEXT))
                  })

              OutlinedTextField(
                  value = uiState.phoneNumber,
                  onValueChange = { signUpViewModel.updatePhoneNumber(it) },
                  label = {
                    Text(text = stringResource(R.string.sign_up_phone_number_placeholder))
                  },
                  singleLine = true,
                  keyboardOptions =
                      KeyboardOptions(
                          imeAction = ImeAction.Next, keyboardType = KeyboardType.Number),
                  modifier = outlinedTextModifier.testTag(C.Tag.SIGN_UP_PHONE_NUMBER_FIELD),
                  colors = textFieldColors,
                  leadingIcon = { Text("+41") },
                  isError = uiState.isPhoneNumberErr,
                  supportingText = {
                    if (uiState.isPhoneNumberErr)
                        Text(
                            text = stringResource(R.string.sign_up_phone_number_help),
                            modifier = Modifier.testTag(C.Tag.SIGN_UP_PHONE_NUMBER_HELP_TEXT))
                  })

              var residenciesExpanded by remember { mutableStateOf(false) }
              ExposedDropdownMenuBox(
                  expanded = residenciesExpanded,
                  onExpandedChange = { residenciesExpanded = it },
                  modifier = Modifier.testTag(C.Tag.SIGN_UP_RESIDENCY_DROP_DOWN_BOX)) {
                    OutlinedTextField(
                        value =
                            uiState.residencyName
                                ?: stringResource(R.string.sign_up_button_not_specified),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(text = stringResource(R.string.sign_up_residency)) },
                        modifier =
                            outlinedTextModifier
                                .menuAnchor(
                                    type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .testTag(C.Tag.SIGN_UP_RESIDENCY_FIELD),
                        colors = textFieldColors,
                        trailingIcon = {
                          ExposedDropdownMenuDefaults.TrailingIcon(residenciesExpanded)
                        })
                    ExposedDropdownMenu(
                        expanded = residenciesExpanded,
                        onDismissRequest = { residenciesExpanded = false },
                        modifier = Modifier.testTag(C.Tag.SIGN_UP_RESIDENCY_DROP_DOWN_MENU)) {
                          DropdownMenuItem(
                              text = {
                                Text(
                                    text = stringResource(R.string.sign_up_button_not_specified),
                                    modifier = Modifier.testTag(C.Tag.residencyNameTestTag(null)))
                              },
                              onClick = {
                                signUpViewModel.updateResidencyName(null)
                                residenciesExpanded = false
                              })
                          uiState.residencies.forEach {
                            DropdownMenuItem(
                                text = {
                                  Text(
                                      text = it.name,
                                      modifier =
                                          Modifier.testTag(C.Tag.residencyNameTestTag(it.name)))
                                },
                                onClick = {
                                  signUpViewModel.updateResidencyName(it.name)
                                  residenciesExpanded = false
                                })
                          }
                        }
                  }

              var universitiesExpanded by remember { mutableStateOf(false) }
              ExposedDropdownMenuBox(
                  expanded = universitiesExpanded,
                  onExpandedChange = { universitiesExpanded = it },
                  modifier = Modifier.testTag(C.Tag.SIGN_UP_UNIVERSITY_DROP_DOWN_BOX)) {
                    OutlinedTextField(
                        value =
                            uiState.universityName
                                ?: stringResource(R.string.sign_up_button_not_specified),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(text = stringResource(R.string.sign_up_university)) },
                        modifier =
                            outlinedTextModifier
                                .menuAnchor(
                                    type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .testTag(C.Tag.SIGN_UP_UNIVERSITY_FIELD),
                        trailingIcon = {
                          ExposedDropdownMenuDefaults.TrailingIcon(universitiesExpanded)
                        },
                        colors = textFieldColors)
                    ExposedDropdownMenu(
                        expanded = universitiesExpanded,
                        onDismissRequest = { universitiesExpanded = false },
                        modifier = Modifier.testTag(C.Tag.SIGN_UP_UNIVERSITY_DROP_DOWN_MENU)) {
                          DropdownMenuItem(
                              text = {
                                Text(
                                    text = stringResource(R.string.sign_up_button_not_specified),
                                    modifier = Modifier.testTag(C.Tag.universityNameTestTag(null)))
                              },
                              onClick = {
                                signUpViewModel.updateUniversityName(null)
                                universitiesExpanded = false
                              },
                          )
                          uiState.universities.forEach {
                            DropdownMenuItem(
                                text = {
                                  Text(
                                      text = it.name,
                                      modifier =
                                          Modifier.testTag(C.Tag.universityNameTestTag(it.name)))
                                },
                                onClick = {
                                  signUpViewModel.updateUniversityName(it.name)
                                  universitiesExpanded = false
                                })
                          }
                        }
                  }

              Button(
                  onClick = {
                    signUpViewModel.signUp(context = context, credentialManager = credentialManager)
                  },
                  enabled = signUpViewModel.isFormValid,
                  colors = ButtonDefaults.buttonColors(containerColor = Red0),
                  modifier = Modifier.fillMaxWidth().height(52.dp).testTag(C.Tag.SIGN_UP_BUTTON),
                  shape = RoundedCornerShape(16.dp)) {
                    Text(text = stringResource(R.string.sign_up_button_text), color = Color.White)
                  }
              Spacer(Modifier.height(8.dp))
              if (!signUpViewModel.isFormValid) {
                Text(
                    stringResource(R.string.sign_up_button_help_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(C.Tag.SIGN_UP_HELP_TEXT))
              }
            }
      }
}

@Preview
@Composable
fun SignUpScreenPreview() {
  MySwissDormAppTheme { SignUpScreen() }
}
