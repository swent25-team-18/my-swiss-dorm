package com.android.mySwissDorm.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    id: String,
    onBack: () -> Unit,
) {
  val vm: RequestDetailViewModel = viewModel()
  val ui by vm.ui.collectAsState()

  LaunchedEffect(id) { vm.load(id) }

  Scaffold(
      containerColor = LightGray,
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Received request") },
            navigationIcon = {
              IconButton(onClick = onBack, modifier = Modifier.testTag("nav_back")) {
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
                Modifier.padding(inner).fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
              FieldBlock("Identifier", "Request #${ui.id}", "req_field_identifiant")
              FieldBlock("Requester", ui.requester, "req_field_requester")
              FieldBlock("Message", ui.message, "req_field_message")

              // ---- FIX: avoid smart-cast error with delegated Compose state ----
              ui.error?.let { errorText ->
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
              }

              Spacer(Modifier.height(4.dp))

              Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { vm.reject() },
                    modifier = Modifier.testTag("btn_reject"),
                    border = BorderStroke(1.dp, Red0),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent, contentColor = Red0),
                    shape = MaterialTheme.shapes.medium) {
                      Text("Reject")
                    }

                Button(
                    onClick = { vm.accept() },
                    modifier = Modifier.testTag("btn_accept"),
                    colors =
                        ButtonDefaults.buttonColors(containerColor = Red0, contentColor = White),
                    shape = MaterialTheme.shapes.medium) {
                      Text("Accept")
                    }
              }
            }
      }
}

@Composable
private fun FieldBlock(label: String, value: String, tag: String? = null) {
  Surface(
      modifier = Modifier.fillMaxWidth().then(if (tag != null) Modifier.testTag(tag) else Modifier),
      color = LightGray,
      border = BorderStroke(1.dp, LightGray0),
      shadowElevation = 0.dp,
      tonalElevation = 0.dp,
      shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
          Text(label, color = LightGray0, style = MaterialTheme.typography.labelMedium)
          Spacer(Modifier.height(4.dp))
          Text(
              value,
              modifier = if (tag != null) Modifier.testTag("${tag}_value") else Modifier,
              style =
                  MaterialTheme.typography.bodyLarge.copy(
                      fontSize = 16.sp, fontWeight = FontWeight.Medium),
              color = Color.Black)
        }
      }
}

@Preview(showBackground = true)
@Composable
private fun RequestDetailScreenPreview() {
  MySwissDormAppTheme { RequestDetailScreen(id = "r1", onBack = {}) }
}
