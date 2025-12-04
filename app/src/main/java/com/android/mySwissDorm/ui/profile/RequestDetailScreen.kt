package com.android.mySwissDorm.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.R
import com.android.mySwissDorm.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    id: String,
    onBack: () -> Unit,
) {
  val vm: RequestDetailViewModel = viewModel()
  val ui by vm.ui.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(id) { vm.load(id, context) }

  Scaffold(
      containerColor = BackGroundColor,
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.request_detail_title)) },
            navigationIcon = {
              IconButton(onClick = onBack, modifier = Modifier.testTag("nav_back")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MainColor)
              }
            },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackGroundColor, titleContentColor = TextColor))
      }) { inner ->
        Column(
            modifier =
                Modifier.padding(inner)
                    .fillMaxSize()
                    .background(BackGroundColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
              FieldBlock(
                  stringResource(R.string.identifier),
                  "${stringResource(R.string.request)} #${ui.id}",
                  "req_field_identifiant")
              FieldBlock(stringResource(R.string.requester), ui.requester, "req_field_requester")
              FieldBlock(stringResource(R.string.message), ui.message, "req_field_message")

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
                    border = BorderStroke(1.dp, MainColor),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = BackGroundColor, contentColor = MainColor),
                    shape = MaterialTheme.shapes.medium) {
                      Text(stringResource(R.string.reject))
                    }

                Button(
                    onClick = { vm.accept() },
                    modifier = Modifier.testTag("btn_accept"),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MainColor, contentColor = BackGroundColor),
                    shape = MaterialTheme.shapes.medium) {
                      Text(stringResource(R.string.accept))
                    }
              }
            }
      }
}

@Composable
private fun FieldBlock(label: String, value: String, tag: String? = null) {
  Surface(
      modifier = Modifier.fillMaxWidth().then(if (tag != null) Modifier.testTag(tag) else Modifier),
      color = BackGroundColor, // white field like Figma
      border = BorderStroke(1.dp, LightGray),
      shadowElevation = 0.dp,
      tonalElevation = 0.dp,
      shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
          Text(label, color = Gray, style = MaterialTheme.typography.labelMedium)
          Spacer(Modifier.height(4.dp))
          Text(
              value,
              modifier = if (tag != null) Modifier.testTag("${tag}_value") else Modifier,
              style =
                  MaterialTheme.typography.bodyLarge.copy(
                      fontSize = 16.sp, fontWeight = FontWeight.Medium),
              color = TextColor)
        }
      }
}

@Preview(showBackground = true)
@Composable
private fun RequestDetailScreenPreview() {
  MySwissDormAppTheme { RequestDetailScreen(id = "r1", onBack = {}) }
}
