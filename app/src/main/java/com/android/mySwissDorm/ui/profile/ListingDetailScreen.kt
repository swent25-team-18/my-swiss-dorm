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

// --- Design tokens (same palette used elsewhere) ---
private val AccentRed = Color(0xFFE57373) // rouge des maquettes
private val ScreenBg = Color(0xFFF7F7FA) // fond gris très léger
private val BlockBg = Color(0xFFF4F4F7) // fond “champ” style formulaire
private val HintGrey = Color(0xFF7A7A7A)
private val BlockBorder = Color(0xFFE6E6EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(id: String, onBack: () -> Unit) {
  Scaffold(
      containerColor = ScreenBg,
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Détail annonce") },
            navigationIcon = {
              IconButton(onClick = onBack, modifier = Modifier.testTag("nav_back")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
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
                    .verticalScroll(scroll), // <-- make content scrollable
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
              // Champs façon “formulaire” plein largeur (comme la 1ère photo)
              FieldBlock(label = "Identifiant", value = "Annonce #$id", tag = "field_identifiant")
              FieldBlock(label = "Titre", value = "Titre de l’annonce", tag = "field_title")

              FieldBlock("Localisation / Résidence", "—", tag = "field_location")
              FieldBlock("Type de logement", "—", tag = "field_type")
              FieldBlock("Surface (m²)", "—", tag = "field_area")
              FieldBlock("Localisation sur la carte", "—", tag = "field_map")
              FieldBlock("Description", "—", tag = "field_description")
              // placeholder en forme de bloc
              FieldBlock("Photos", "Aucune photo", tag = "field_photos")

              Spacer(Modifier.height(4.dp))

              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { /* TODO edit */},
                        modifier = Modifier.testTag("btn_edit"),
                        border = BorderStroke(1.dp, AccentRed),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent, contentColor = AccentRed),
                        shape = MaterialTheme.shapes.medium) {
                          Text("Éditer")
                        }

                    Button(
                        onClick = { /* TODO close */},
                        modifier = Modifier.testTag("btn_close"),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = AccentRed, contentColor = Color.White),
                        shape = MaterialTheme.shapes.medium) {
                          Text("Fermer")
                        }
                  }
            }
      }
}

/**
 * Bloc plein largeur qui imite un champ de formulaire en lecture seule : fond clair, coin arrondi,
 * fine bordure, label gris + valeur.
 */
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
