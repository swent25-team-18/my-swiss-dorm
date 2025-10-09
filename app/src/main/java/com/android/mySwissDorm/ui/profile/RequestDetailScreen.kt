package com.android.mySwissDorm.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.testTag

// --- design tokens (same palette as other screens) ---
private val AccentRed  = Color(0xFFE57373)   // rouge des maquettes
private val ScreenBg   = Color(0xFFF7F7FA)   // fond gris très léger
private val BlockBg    = Color(0xFFF4F4F7)   // fond des "champs" (comme la 1ère photo)
private val HintGrey   = Color(0xFF7A7A7A)
private val BlockBorder = Color(0xFFE6E6EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    id: String,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Demande reçue") },
                navigationIcon = {
                    IconButton(onClick = onBack,
                            modifier = Modifier.testTag("nav_back")  ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = AccentRed
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // En-tête type "champ" (plein largeur)
            FieldBlock(
                label = "Identifiant",
                value = "Demande #$id",
                tag = "req_field_identifiant"
            )

            FieldBlock(
                label = "Demandeur",
                value = "…",
                tag = "req_field_requester"
            )

            FieldBlock(
                label = "Message",
                value = "“Hello, je suis…”",
                tag = "req_field_message"
            )

            Spacer(Modifier.height(4.dp))

            // actions (mêmes styles que les autres écrans)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* TODO reject */ },
                            modifier = Modifier.testTag("btn_reject"),
                    border = BorderStroke(1.dp, AccentRed),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = AccentRed
                    ),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Refuser") }

                Button(
                    onClick = { /* TODO accept */ },
                    modifier = Modifier.testTag("btn_accept"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Accepter") }
            }

            // astuce/bas de page (facultatif, comme l’avertissement gris de la 1ère photo)
            // Text(
            //     "Veuillez vérifier les informations avant d’accepter.",
            //     style = MaterialTheme.typography.bodySmall,
            //     color = HintGrey
            // )
        }
    }
}

/** Bloc plein largeur qui imite un TextField désactivé / carte plate
 *  (fond clair, coins arrondis, label gris au-dessus de la valeur) */
// Make FieldBlock accept an optional tag and apply it:
@Composable
private fun FieldBlock(
    label: String,
    value: String,
    tag: String? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        // …rest unchanged
    ) {
        // optional: tag the value text too
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text = label, color = HintGrey, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                modifier = if (tag != null) Modifier.testTag("${tag}_value") else Modifier,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                color = Color.Black
            )
        }
    }
}
