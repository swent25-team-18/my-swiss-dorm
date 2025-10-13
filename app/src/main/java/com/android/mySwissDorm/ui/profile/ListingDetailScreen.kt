package com.android.mySwissDorm.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mySwissDorm.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    id: String,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onClose: () -> Unit = onBack,
) {
  Scaffold(
      containerColor = LightGray,
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Listing details") },
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
        val scroll = rememberScrollState()

        Column(
            modifier =
                Modifier.padding(inner)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
              FieldBlock("Identifier", "Listing #$id", "field_identifiant")
              FieldBlock("Title", "Listing title", "field_title")
              FieldBlock("Location / Residence", "—", "field_location")
              FieldBlock("Housing type", "—", "field_type")
              FieldBlock("Area (m²)", "—", "field_area")
              FieldBlock("Map location", "—", "field_map")
              FieldBlock("Description", "—", "field_description")
              FieldBlock("Photos", "No photos", "field_photos")

              Spacer(Modifier.height(4.dp))

              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("btn_edit"),
                        border = BorderStroke(1.dp, Red0),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent, contentColor = Red0),
                        shape = MaterialTheme.shapes.medium) {
                          Text("Edit")
                        }

                    Button(
                        onClick = onClose,
                        modifier = Modifier.testTag("btn_close"),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Red0, contentColor = White),
                        shape = MaterialTheme.shapes.medium) {
                          Text("Close")
                        }
                  }
            }
      }
}

@Composable
private fun FieldBlock(label: String, value: String, tag: String? = null) {
  Surface(
      modifier = Modifier.fillMaxWidth().let { m -> if (tag != null) m.testTag(tag) else m },
      shape = RoundedCornerShape(14.dp),
      color = LightGray,
      border = BorderStroke(1.dp, LightGray0),
      shadowElevation = 0.dp,
      tonalElevation = 0.dp) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
          Text(label, color = HintGrey, style = MaterialTheme.typography.labelMedium)
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
