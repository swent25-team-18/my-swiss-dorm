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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.ui.theme.AccentRed
import com.android.mySwissDorm.ui.theme.BlockBg
import com.android.mySwissDorm.ui.theme.BlockBorder
import com.android.mySwissDorm.ui.theme.HintGrey
import com.android.mySwissDorm.ui.theme.ScreenBg
import androidx.compose.ui.tooling.preview.Preview
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(id: String, onBack: () -> Unit, vm: RequestDetailViewModel = viewModel()) {
  LaunchedEffect(id) { vm.load(id) }
  val ui by vm.ui.collectAsState()

  Scaffold(
      containerColor = ScreenBg,
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Received request") },
            navigationIcon = {
              IconButton(onClick = onBack, modifier = Modifier.testTag("nav_back")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AccentRed)
              }
            },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White, titleContentColor = Color.Black))
      }) { inner ->
        Column(
            modifier =
                Modifier.padding(inner).fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
              if (ui.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
              }
              ui.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
              }

              FieldBlock(
                  label = "Identifier", value = "Request #${ui.id}", tag = "req_field_identifiant")
              FieldBlock(label = "Requester", value = ui.requester, tag = "req_field_requester")
              FieldBlock(label = "Message", value = ui.message, tag = "req_field_message")

              Spacer(Modifier.height(4.dp))

              Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { vm.reject() },
                    modifier = Modifier.testTag("btn_reject"),
                    border = BorderStroke(1.dp, AccentRed),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent, contentColor = AccentRed),
                    shape = MaterialTheme.shapes.medium) {
                      Text("Reject")
                    }

                Button(
                    onClick = { vm.accept() },
                    modifier = Modifier.testTag("btn_accept"),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = AccentRed, contentColor = Color.White),
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
      color = BlockBg,
      border = BorderStroke(1.dp, BlockBorder),
      shadowElevation = 0.dp,
      tonalElevation = 0.dp,
      shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
          Text(text = label, color = HintGrey, style = MaterialTheme.typography.labelMedium)
          Spacer(Modifier.height(4.dp))
          Text(
              text = value,
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
    MySwissDormAppTheme {
        RequestDetailScreen(
            id = "r1",
            onBack = {}
        )
    }
}