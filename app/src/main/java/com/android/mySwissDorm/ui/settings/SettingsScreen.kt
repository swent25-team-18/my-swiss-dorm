package com.android.mySwissDorm.ui.settings

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.mySwissDorm.ui.theme.*

// ✅ keep a preview instance only
private val previewUiState =
    SettingsUiState(
        userName = "John Doe", errorMsg = null, topItems = emptyList(), accountItems = emptyList())

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class) // <-- add Foundation opt-in
@Composable
fun SettingsScreenContent(
    ui: SettingsUiState,
    onGoBack: () -> Unit = {},
    onItemClick: (String) -> Unit = {}
) {
  val context = LocalContext.current

  var notificationsMessages by remember { mutableStateOf(true) }
  var notificationsListings by remember { mutableStateOf(false) }
  var readReceipts by remember { mutableStateOf(true) }
  var email by remember { mutableStateOf("john.doe@email.com") }
  var blockedExpanded by remember { mutableStateOf(false) }
  val blockedContacts = listOf("Clarisse K.", "Alice P.", "Benjamin M.")

  LaunchedEffect(ui.errorMsg) {
    ui.errorMsg?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
  }

  Scaffold(
      containerColor = LightGray,
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
              IconButton(onClick = onGoBack, modifier = Modifier.testTag("BackButton")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Red0)
              }
            },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = White, titleContentColor = Color.Black))
      }) { inner ->
        Column(
            modifier =
                Modifier.padding(inner)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

              // ---- Profile card ----------------------------------------------------
              CardBlock {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier =
                                Modifier.size(56.dp)
                                    .clip(CircleShape)
                                    .background(PalePink.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center) {
                              Text("A", fontWeight = FontWeight.Bold, color = Red0)
                            }
                        Spacer(Modifier.width(12.dp))
                        Column {
                          Text(ui.userName, style = MaterialTheme.typography.titleMedium)
                          Text(
                              "View profile",
                              style = MaterialTheme.typography.bodySmall,
                              color = Color(0xFF7A7A7A))
                        }
                      }
                      IconButton(
                          onClick = { /* TODO: open profile */},
                          modifier = Modifier.testTag("ProfileButton")) {
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = "Open profile")
                          }
                    }
              }

              // ---- Notifications ---------------------------------------------------
              SectionLabel("Notifications")
              CardBlock {
                SettingSwitchRow(
                    label = "Show notifications for messages",
                    checked = notificationsMessages,
                    onCheckedChange = { notificationsMessages = it })
                SoftDivider()
                SettingSwitchRow(
                    label = "Show notifications for new listings",
                    checked = notificationsListings,
                    onCheckedChange = { notificationsListings = it })
              }

              // ---- Account ---------------------------------------------------------
              SectionLabel("Account")
              CardBlock {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email address") },
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("EmailField"))
                SoftDivider()
                Button(
                    onClick = { onItemClick("Delete my account") },
                    colors =
                        ButtonDefaults.buttonColors(containerColor = Red0, contentColor = White),
                    shape = MaterialTheme.shapes.medium,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("DeleteAccountButton")) {
                      Text("Delete my account")
                    }
              }

              // ---- Privacy ---------------------------------------------------------
              SectionLabel("Privacy")
              CardBlock {
                SettingSwitchRow(
                    label = "Read receipts",
                    checked = readReceipts,
                    onCheckedChange = { readReceipts = it })
                SoftDivider()

                // Blocked contacts
                Row(
                    modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                      Text(
                          "Blocked contacts (${blockedContacts.size})",
                          style = MaterialTheme.typography.bodyLarge)
                      val rotation by
                          animateFloatAsState(
                              targetValue = if (blockedExpanded) 90f else 0f,
                              label = "blockedArrowRotation")
                      IconButton(
                          onClick = { blockedExpanded = !blockedExpanded },
                          modifier = Modifier.testTag("BlockedContactsToggle")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription =
                                    if (blockedExpanded) "Hide blocked" else "Show blocked",
                                modifier = Modifier.rotate(rotation))
                          }
                    }

                // Bring the list into view when expanding
                val blockedBringIntoView = remember { BringIntoViewRequester() }
                LaunchedEffect(blockedExpanded) {
                  if (blockedExpanded) blockedBringIntoView.bringIntoView()
                }

                if (blockedExpanded) {
                  Surface(
                      color = LightGray.copy(alpha = 0.6f),
                      shape = MaterialTheme.shapes.medium,
                      border = BorderStroke(1.dp, LightGray0),
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(horizontal = 16.dp, vertical = 6.dp)
                              .bringIntoViewRequester(blockedBringIntoView)
                              .testTag("BlockedContactsList")) {
                        Column(Modifier.padding(12.dp)) {
                          blockedContacts.forEach { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp))
                          }
                        }
                      }
                }
              }
            }
      }
}

// ---------- Helpers ----------

@Composable
private fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
  Surface(
      color = White,
      shape = MaterialTheme.shapes.large,
      border = BorderStroke(1.dp, LightGray0),
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
      color = Color.Black,
      modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
}

@Composable
private fun SoftDivider() {
  HorizontalDivider(thickness = 1.dp, color = LightGray0.copy(alpha = 0.4f))
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(
            modifier =
                Modifier.testTag("SettingSwitch_${label}").semantics {
                  role = Role.Switch
                }, // <-- set role via semantics
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = PalePink,
                    checkedTrackColor = Red0,
                    uncheckedThumbColor = LightGray0,
                    uncheckedTrackColor = LightGray0.copy(alpha = 0.6f)))
      }
}

// ---------- Preview ----------
@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
  MySwissDormAppTheme {
    SettingsScreenContent(ui = previewUiState, onGoBack = {}, onItemClick = {})
  }
}
