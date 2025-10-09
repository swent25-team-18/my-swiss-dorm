package com.android.mySwissDorm.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.platform.testTag

private val AccentRed = Color(0xFFE57373)         // rouge des maquettes
private val ScreenBg  = Color(0xFFF7F7FA)         // fond gris très léger
private val CardBorder = Color(0xFFE6E6EB)        // bordure fine style iOS/Material flat

data class Contribution(val title: String, val description: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContributionsScreen(
    contributions: List<Contribution>,
    onBackClick: () -> Unit,
    onContributionClick: (Contribution) -> Unit
) {
    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mes contributions") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(contributions) { index, c ->
                // Carte plate, blanche, bordure fine, coins arrondis
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onContributionClick(c) },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = c.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = c.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF7A7A7A)
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onContributionClick(c) },
                            modifier = Modifier.testTag("btn_contrib_details_$index"),
                            border = BorderStroke(1.dp, AccentRed),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = AccentRed
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Voir détails")
                        }
                    }
                }
            }
        }
    }
}
