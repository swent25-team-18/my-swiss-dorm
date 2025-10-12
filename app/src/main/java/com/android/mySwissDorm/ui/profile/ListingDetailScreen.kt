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
import com.android.mySwissDorm.ui.theme.AccentRed
import com.android.mySwissDorm.ui.theme.BlockBg
import com.android.mySwissDorm.ui.theme.BlockBorder
import com.android.mySwissDorm.ui.theme.HintGrey
import com.android.mySwissDorm.ui.theme.ScreenBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    id: String,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onClose: () -> Unit = {}
) {
  Scaffold(
      containerColor = ScreenBg,
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Listing details") },
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
        val scroll = rememberScrollState()

        Column(
            modifier =
                Modifier.padding(inner)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
              // Form-like read-only blocks
              FieldBlock(label = "Identifier", value = "Listing #$id", tag = "field_identifiant")
              FieldBlock(label = "Title", value = "Listing title", tag = "field_title")

              FieldBlock("Location / Residence", "—", tag = "field_location")
              FieldBlock("Housing type", "—", tag = "field_type")
              FieldBlock("Area (m²)", "—", tag = "field_area")
              FieldBlock("Map location", "—", tag = "field_map")
              FieldBlock("Description", "—", tag = "field_description")
              FieldBlock("Photos", "No photos", tag = "field_photos")

              Spacer(Modifier.height(4.dp))

              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("btn_edit"),
                        border = BorderStroke(1.dp, AccentRed),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent, contentColor = AccentRed),
                        shape = MaterialTheme.shapes.medium) {
                          Text("Edit")
                        }

                    Button(
                        onClick = onClose,
                        modifier = Modifier.testTag("btn_close"),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = AccentRed, contentColor = Color.White),
                        shape = MaterialTheme.shapes.medium) {
                          Text("Close")
                        }
                  }
            }
      }
}

/** Full-width block that mimics a read-only form field. */
@Composable
private fun FieldBlock(label: String, value: String, tag: String? = null) {
  Surface(
      modifier = Modifier.fillMaxWidth().let { m -> if (tag != null) m.testTag(tag) else m },
      shape = RoundedCornerShape(14.dp),
      color = BlockBg,
      border = BorderStroke(1.dp, BlockBorder),
      shadowElevation = 0.dp,
      tonalElevation = 0.dp) {
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
